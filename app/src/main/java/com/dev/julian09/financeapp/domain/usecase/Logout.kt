package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.port.AuthRepository
import javax.inject.Inject

/** SEQ-03: Sprint 3 also cancels the sync worker here; local data is never deleted (REQ-AUT-06). */
class Logout
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) {
        suspend operator fun invoke() = repository.logout()
    }
