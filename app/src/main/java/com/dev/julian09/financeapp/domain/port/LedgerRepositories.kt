package com.dev.julian09.financeapp.domain.port

import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/** Read side of the local ledger; writes and sync arrive in S3-H03 and later sprints. Reads never touch the network. */
interface AccountRepository {
    /** Active, not deleted accounts in their display order. */
    fun observeActive(): Flow<List<Account>>
}

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
}

interface TransactionRepository {
    /** Newest first, not deleted. */
    fun observeRecent(limit: Int): Flow<List<Transaction>>
}
