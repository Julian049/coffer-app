package com.dev.julian09.financeapp.domain.port

import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountImpact
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryTotal
import com.dev.julian09.financeapp.domain.model.CreditCard
import com.dev.julian09.financeapp.domain.model.DeferredPurchase
import com.dev.julian09.financeapp.domain.model.Installment
import com.dev.julian09.financeapp.domain.model.PeriodTotals
import com.dev.julian09.financeapp.domain.model.Recurring
import com.dev.julian09.financeapp.domain.model.SearchTotals
import com.dev.julian09.financeapp.domain.model.Settings
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionFilter
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
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

    /** An import batch (S8-DAT02): every movement and its CREATE history in one database transaction. */
    suspend fun insertAll(transactions: List<Transaction>)

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

    /** Newest first by occurredAt, at most [limit] rows; category ids are taken as given (no subcategories added). */
    fun search(
        filter: TransactionFilter,
        limit: Int,
    ): Flow<List<Transaction>>

    fun searchTotals(filter: TransactionFilter): Flow<SearchTotals>

    fun observeFirstOccurredAt(): Flow<Instant?>

    /** Not deleted payments of recurring expenses in [from, to), newest first. */
    fun observeRecurringPayments(
        from: Instant,
        to: Instant,
    ): Flow<List<Transaction>>
}

interface RecurringRepository {
    /** Not deleted, active or finalized, by due date. */
    fun observeAll(): Flow<List<Recurring>>

    fun observe(id: UUID): Flow<Recurring?>

    suspend fun create(recurring: Recurring)

    suspend fun update(recurring: Recurring)

    /** PayPending (SEQ-10 §10.2): the expense, its history and the recurring's next date in one database transaction. */
    suspend fun pay(
        recurring: Recurring,
        payment: Transaction,
    )

    suspend fun markNotified(
        id: UUID,
        on: LocalDate,
    )
}

interface SettingsRepository {
    /** The defaults until the first sync seeds the row. */
    fun observe(): Flow<Settings>

    suspend fun update(settings: Settings)
}

/** Credit cards (S7): terms, deferred purchases and their plans; every write is atomic like the rest of the ledger. */
interface CardRepository {
    /** Terms of every not deleted card. */
    fun observeCards(): Flow<List<CreditCard>>

    /** Not deleted deferred purchases of every card. */
    fun observeDeferred(): Flow<List<DeferredPurchase>>

    /** Not deleted installments of every deferred purchase. */
    fun observeInstallments(): Flow<List<Installment>>

    /** Not deleted movements where a card is the source or the destination, newest first. */
    fun observeCardTransactions(): Flow<List<Transaction>>

    /** The CREDIT_CARD account and its terms together (REQ-TC-01). */
    suspend fun create(
        account: Account,
        card: CreditCard,
    )

    suspend fun update(
        account: Account,
        card: CreditCard,
    )

    /** SEQ-06: the expense, its history, the deferred purchase and its plan in one database transaction. */
    suspend fun insertDeferred(
        purchase: Transaction,
        deferred: DeferredPurchase,
        installments: List<Installment>,
    )

    /** SEQ-07: the payment transfer, its history and the installments it settles in one database transaction. */
    suspend fun pay(
        payment: Transaction,
        installments: List<Installment>,
    )

    /** Notices of a card statement are local to the phone: card terms have no lastNotifiedOn. */
    suspend fun lastNotifiedOn(cardId: UUID): LocalDate?

    suspend fun markNotified(
        cardId: UUID,
        on: LocalDate,
    )
}
