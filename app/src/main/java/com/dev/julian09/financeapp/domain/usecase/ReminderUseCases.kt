package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.finance.RecurrenceCalculator
import com.dev.julian09.financeapp.domain.model.Reminder
import com.dev.julian09.financeapp.domain.model.ReminderKind
import com.dev.julian09.financeapp.domain.model.Settings
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.RecurringRepository
import com.dev.julian09.financeapp.domain.port.ReminderNotifier
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val LEAD_MAX = 30

/**
 * SEQ-10 §10.3, run once a day by ReminderWorker. lastNotifiedOn keeps it to one notice per day and payment; a window
 * that opened on a day the check did not run still gets its notice on the next run inside it.
 */
class SendReminders
    @Inject
    constructor(
        private val recurring: RecurringRepository,
        private val settings: SettingsRepository,
        private val notifier: ReminderNotifier,
        private val scheduler: SyncScheduler,
        private val clock: Clock,
    ) {
        /** How many notices went out. */
        suspend operator fun invoke(): Int {
            val s = settings.observe().first()
            if (!s.notificationsEnabled || !notifier.canNotify()) return 0
            val today = clock.today()
            val due =
                recurring.observeAll().first().filter { it.active }.mapNotNull { r ->
                    val opens = r.nextDueDate.minusDays(s.reminderLeadDays.toLong())
                    val last = r.lastNotifiedOn
                    when {
                        r.nextDueDate == today && last != today -> Reminder(r, ReminderKind.DUE_TODAY)
                        today.isBefore(r.nextDueDate) &&
                            RecurrenceCalculator.inWindow(r.nextDueDate, today, s.reminderLeadDays) &&
                            (last == null || last.isBefore(opens)) -> Reminder(r, ReminderKind.WINDOW_OPEN)
                        else -> null
                    }
                }
            due.forEach {
                notifier.notify(it)
                recurring.markNotified(it.recurring.id, today)
            }
            if (due.isNotEmpty()) scheduler.requestSync()
            return due.size
        }
    }

/** Ajustes → Avisos (REQ-REC-03, 10): the switch and the lead days, 0 to 30. */
class UpdateReminderSettings
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(transform: (Settings) -> Settings) {
            val changed = transform(settings.observe().first())
            settings.update(changed.copy(reminderLeadDays = changed.reminderLeadDays.coerceIn(0, LEAD_MAX)))
            scheduler.requestSync()
        }
    }
