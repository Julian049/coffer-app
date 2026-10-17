package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.port.AuthRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import javax.inject.Inject

/** SEQ-03: stop the sync, then forget the session; local data is never deleted (REQ-AUT-06). */
class Logout
    @Inject
    constructor(
        private val repository: AuthRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke() {
            scheduler.cancelAll()
            repository.logout()
        }
    }
