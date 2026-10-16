package com.dev.julian09.financeapp.legacy.domain.usecase

import com.dev.julian09.financeapp.legacy.domain.model.Transaction
import com.dev.julian09.financeapp.legacy.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTotalAmountUseCase
    @Inject
    constructor(
        private val repository: TransactionRepository,
    ) {
        suspend operator fun invoke(transactions: List<Transaction>): Double = repository.getTotalAmount(transactions)
    }
