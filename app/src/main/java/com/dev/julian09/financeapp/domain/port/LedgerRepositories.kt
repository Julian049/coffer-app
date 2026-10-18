package com.dev.julian09.financeapp.domain.port

import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountImpact
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryTotal
import com.dev.julian09.financeapp.domain.model.PeriodTotals
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

/** Local ledger. Reads never touch the network; every write is atomic and leaves the rows PENDING (ADR-0006). */
interface AccountRepository {
    /** Active, not deleted accounts in their display order. */
    fun observeActive(): Flow<List<Account>>

    /** Every not deleted account (active or not) with what moved through it before [before] (all if null). */
    fun observeBalances(before: Instant? = null): Flow<List<AccountBalance>>

    suspend fun create(account: Account)

    suspend fun update(account: Account)

    suspend fun setActive(
        id: UUID,
        active: Boolean,
    )

    suspend fun impact(id: UUID): AccountImpact

    /** SEQ-12 MOVE: its movements go to [targetId], transfers between both disappear, then the account is deleted. */
    suspend fun moveAndDelete(
        id: UUID,
        targetId: UUID,
    )

    /** SEQ-12 DELETE_ALL: the account and every movement where it appears. */
    suspend fun deleteWithTransactions(id: UUID)
}

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>

    /** Totals per category (subcategories apart) of movements in [from, to). */
    fun observeTotals(
        from: Instant,
        to: Instant,
    ): Flow<List<CategoryTotal>>

    suspend fun create(category: Category)

    suspend fun rename(
        id: UUID,
        name: String,
    )

    /** Movements of the category and its subcategories, which are deleted with it (REQ-CAT-03). */
    suspend fun transactionCount(id: UUID): Int

    /** Deletes the category and its subcategories, moving their movements to [targetId] first. */
    suspend fun delete(
        id: UUID,
        targetId: UUID?,
    )
}

interface TagRepository {
    fun observeAll(): Flow<List<Tag>>

    suspend fun create(tag: Tag)
}

interface TransactionRepository {
    fun observe(id: UUID): Flow<Transaction?>

    /** SEQ-04: the account of the last movement registered, if it is still active. */
    suspend fun lastUsedAccountId(): UUID?

    /** Each write and its change_log row go in one database transaction (REQ-HIS-01). */
    suspend fun insert(transaction: Transaction)

    suspend fun update(transaction: Transaction)

    suspend fun delete(id: UUID)

    /** Newest first, not deleted. */
    fun observeRecent(limit: Int): Flow<List<Transaction>>

    /** Newest first, from [from] on. */
    fun observeSince(from: Instant): Flow<List<Transaction>>

    /** Newest first, where the account is the source or the destination. */
    fun observeByAccount(accountId: UUID): Flow<List<Transaction>>

    /** Income and expenses (no transfers) in [from, to) of active accounts included in the total. */
    fun observeTotals(
        from: Instant,
        to: Instant,
    ): Flow<PeriodTotals>

    fun observeCount(): Flow<Int>
}
