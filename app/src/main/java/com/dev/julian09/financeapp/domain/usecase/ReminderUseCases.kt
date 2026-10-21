package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.finance.RecurrenceCalculator
import com.dev.julian09.financeapp.domain.model.Reminder
import com.dev.julian09.financeapp.domain.model.ReminderKind
import com.dev.julian09.financeapp.domain.model.Settings
import com.dev.julian09.financeapp.domain.port.CardRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.RecurringRepository
import com.dev.julian09.financeapp.domain.port.ReminderNotifier
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import kotlinx.coroutines.flow.first
import java.time.LocalDate
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
        private val cards: CardRepository,
        private val cardDues: ObserveCardDues,
        private val loanDues: ObserveLoanDues,
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
            val lead = s.reminderLeadDays

            fun kind(
                dueDate: LocalDate,
                last: LocalDate?,
            ): ReminderKind? {
                val opens = dueDate.minusDays(lead.toLong())
                return when {
                    dueDate == today && last != today -> ReminderKind.DUE_TODAY
                    today.isBefore(dueDate) &&
                        RecurrenceCalculator.inWindow(dueDate, today, lead) &&
                        (last == null || last.isBefore(opens)) -> ReminderKind.WINDOW_OPEN
                    else -> null
                }
            }
            val due =
                recurring.observeAll().first().filter { it.active }.mapNotNull { r ->
                    kind(r.nextDueDate, r.lastNotifiedOn)?.let { Reminder(RecurrenceCalculator.pending(r, today, lead), it) }
                }
            // Card statements (PO, 2026-10-19) and loan installments (REQ-PRE-04): the same notices, remembered on the phone
            val cardDue =
                (cardDues().first() + loanDues().first()).mapNotNull { c ->
                    kind(c.dueDate, cards.lastNotifiedOn(c.id))?.let { Reminder(c, it) }
                }
            due.forEach {
                notifier.notify(it)
                recurring.markNotified(it.payment.id, today)
            }
            cardDue.forEach {
                notifier.notify(it)
                cards.markNotified(it.payment.id, today)
            }
            if (due.isNotEmpty()) scheduler.requestSync()
            return due.size + cardDue.size
        }
    }

/** Ajustes: notices (REQ-REC-03, 10; lead days 0 to 30), balance view (REQ-CTA-09), decimals (RNF-13) and Home cards (REQ-INI-04). */
class UpdateSettings
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
