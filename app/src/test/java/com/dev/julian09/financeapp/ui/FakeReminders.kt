package com.dev.julian09.financeapp.ui

import com.dev.julian09.financeapp.domain.model.Reminder
import com.dev.julian09.financeapp.domain.port.ReminderNotifier
import com.dev.julian09.financeapp.domain.port.ReminderScheduler

class FakeReminderScheduler : ReminderScheduler {
    var scheduled = false
    var cancelled = false

    override fun ensureScheduled() {
        scheduled = true
    }

    override fun cancel() {
        cancelled = true
    }
}

/** Records the notices instead of showing them; [allowed] plays the system permission. */
class FakeReminderNotifier(
    var allowed: Boolean = true,
) : ReminderNotifier {
    val sent = mutableListOf<Reminder>()

    override fun canNotify() = allowed

    override fun notify(reminder: Reminder) {
        sent += reminder
    }
}
