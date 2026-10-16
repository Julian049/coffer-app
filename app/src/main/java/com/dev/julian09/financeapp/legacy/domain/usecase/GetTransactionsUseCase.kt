package com.dev.julian09.financeapp.legacy.domain.usecase

import com.dev.julian09.financeapp.legacy.domain.model.Transaction
import com.dev.julian09.financeapp.legacy.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsUseCase
    @Inject
    constructor(
        private val repository: TransactionRepository,
    ) {
        suspend operator fun invoke(): Flow<List<Transaction>> = repository.getTransactions()
    }
