package com.dev.julian09.financeapp.legacy.domain.usecase

import com.dev.julian09.financeapp.legacy.domain.repository.TransactionRepository
import javax.inject.Inject

class SyncTransactionsUseCase
    @Inject
    constructor(
        private val repository: TransactionRepository,
    ) {
        suspend operator fun invoke() {
            repository.syncTransactions()
        }
    }
