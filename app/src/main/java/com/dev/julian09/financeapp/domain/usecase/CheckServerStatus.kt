package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.port.ServerStatusRepository
import javax.inject.Inject

class CheckServerStatus
    @Inject
    constructor(
        private val repository: ServerStatusRepository,
    ) {
        suspend operator fun invoke(): Boolean = repository.isAvailable()
    }
