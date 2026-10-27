package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.port.AuthRepository
import com.dev.julian09.financeapp.domain.port.PushTokens
import com.dev.julian09.financeapp.domain.port.ReminderScheduler
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import javax.inject.Inject

/** SEQ-03: stop the sync and the reminders, then forget the session; local data is never deleted (REQ-AUT-06). */
class Logout
    @Inject
    constructor(
        private val repository: AuthRepository,
        private val scheduler: SyncScheduler,
        private val reminders: ReminderScheduler,
        private val push: PushTokens,
    ) {
        suspend operator fun invoke() {
            push.unregister()
            scheduler.cancelAll()
            reminders.cancel()
            repository.logout()
        }
    }
