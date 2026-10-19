package com.dev.julian09.financeapp.domain.port

import com.dev.julian09.financeapp.domain.model.Reminder

/** The phone's notifications (SEQ-10 §10.3): only pending payments, never anything else (REQ-REC-10). */
interface ReminderNotifier {
    /** The user allowed notifications for the app (POST_NOTIFICATIONS on Android 13+). */
    fun canNotify(): Boolean

    fun notify(reminder: Reminder)
}

/** The daily check of pending payments, near 08:00 and without network. */
interface ReminderScheduler {
    fun ensureScheduled()

    /** Sign-out: no reminders until the next sign-in. */
    fun cancel()
}
