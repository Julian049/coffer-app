package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.SessionState
import com.dev.julian09.financeapp.domain.port.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSession
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) {
        operator fun invoke(): Flow<SessionState> = repository.observeSession()
    }
