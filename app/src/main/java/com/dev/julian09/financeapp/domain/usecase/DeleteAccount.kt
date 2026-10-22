package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.port.AuthRepository
import com.dev.julian09.financeapp.domain.port.LocalDataEraser
import com.dev.julian09.financeapp.domain.port.ReminderScheduler
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * S14-H01: the server erases the account first (online only, with the password or a Google token); only then
 * does this phone forget the sync, the reminders, all local data and the session.
 */
class DeleteAccount
    @Inject
    constructor(
        private val repository: AuthRepository,
        private val scheduler: SyncScheduler,
        private val reminders: ReminderScheduler,
        private val eraser: LocalDataEraser,
    ) {
        suspend operator fun invoke(
            password: String?,
            idToken: String?,
        ): Result<Unit> =
            repository.deleteAccount(password, idToken).onSuccess {
                // The screen is gone as soon as the session is, but the cleanup must finish
                withContext(NonCancellable) {
                    scheduler.cancelAll()
                    reminders.cancel()
                    eraser.eraseAll()
                    repository.clearSession()
                }
            }
    }
