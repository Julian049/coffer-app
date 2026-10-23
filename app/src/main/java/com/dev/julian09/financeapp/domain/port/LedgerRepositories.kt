package com.dev.julian09.financeapp.domain.port

import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountImpact
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryTotal
import com.dev.julian09.financeapp.domain.model.ChangeEntry
import com.dev.julian09.financeapp.domain.model.CreditCard
import com.dev.julian09.financeapp.domain.model.DeferredPurchase
import com.dev.julian09.financeapp.domain.model.Installment
import com.dev.julian09.financeapp.domain.model.Loan
import com.dev.julian09.financeapp.domain.model.LoanInstallment
import com.dev.julian09.financeapp.domain.model.NoteSuggestion
import com.dev.julian09.financeapp.domain.model.PeriodTotals
import com.dev.julian09.financeapp.domain.model.Recurring
import com.dev.julian09.financeapp.domain.model.SearchTotals
import com.dev.julian09.financeapp.domain.model.Settings
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.model.TagTotal
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionFilter
import com.dev.julian09.financeapp.domain.model.TransactionType
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

    /**
     * S12-H03: each asset account's opening balance becomes a «Saldo inicial» income, excluded from reports, and the
     * field goes to 0; the balance does not change. Ids are derived from the user and the account, so two phones write
     * the same rows. Does nothing before the database has an owner. True when it wrote something to push.
     */
    suspend fun moveOpeningBalances(): Boolean

    /**
     * S15-DAT03: creates the expense category «Deudas» (a normal one, with an id that depends on the user) and makes it the
     * category of loan installments, when none is set yet and it was never created. False when nothing changed.
     */
    suspend fun ensureLoanCategory(): Boolean

    /** M9: the carousel and the lists follow [ids], first to last. */
    suspend fun reorder(ids: List<UUID>)

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

    /** S13-UI04: a category's icon changes after it was created. */
    suspend fun setIcon(
        id: UUID,
        icon: String,
    )

    /** REQ-CAT-04; the screen offers it on parents only. */
    suspend fun setExcludedFromReports(
        id: UUID,
        excluded: Boolean,
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

    suspend fun rename(
        id: UUID,
        name: String,
    )

    suspend fun setPinned(
        id: UUID,
        pinned: Boolean,
    )

    /** The tag and, in the same database transaction, the tag of its movements (S9-DOM02). */
    suspend fun delete(id: UUID)

    /** How many movements carry the tag (what the delete confirmation tells). */
    suspend fun movementCount(id: UUID): Int

    /** Income and expenses per tag in [from, to), transfers apart (REQ-ETQ-03). */
    fun observeTotals(
        from: Instant,
        to: Instant,
    ): Flow<List<TagTotal>>
}

interface TransactionRepository {
    fun observe(id: UUID): Flow<Transaction?>

    /** SEQ-04: the account of the last movement registered, if it is still active. */
    suspend fun lastUsedAccountId(): UUID?

    /** Each write and its change_log row go in one database transaction (REQ-HIS-01). */
    suspend fun insert(transaction: Transaction)

    suspend fun update(transaction: Transaction)

    /**
     * S15-DOM04: moves a registered payment (installment, extra payment or card payment) to another date, with the
     * installments it settled, and logs UPDATE; false when it is not a live payment.
     */
    suspend fun changePaymentDate(
        id: UUID,
        occurredAt: Instant,
    ): Boolean

    /** An import batch (S8-DAT02): every movement and its CREATE history in one database transaction. */
    suspend fun insertAll(transactions: List<Transaction>)

    suspend fun delete(id: UUID)

    /** Undeletes a movement and logs RESTORE in the same database transaction (REQ-HIS-03); false when it is not deleted. */
    suspend fun restore(id: UUID): Boolean

    /** M5: notes of earlier movements of [type] that contain [text], latest use first, each with its latest category. */
    suspend fun suggestNotes(
        type: TransactionType,
        text: String,
        limit: Int = 5,
    ): List<NoteSuggestion>

    /** Newest first, not deleted. */
    fun observeRecent(limit: Int): Flow<List<Transaction>>

    /** Newest first, from [from] on. */
    fun observeSince(from: Instant): Flow<List<Transaction>>

    /** Newest first, where the account is the source or the destination. */
    fun observeByAccount(accountId: UUID): Flow<List<Transaction>>

    /** Not deleted, in [from, to), any order. */
    fun observeBetween(
        from: Instant,
        to: Instant,
    ): Flow<List<Transaction>>

    /** Card purchases in 2 or more installments, whenever they happened (reports count them by installment). */
    fun observeDeferredPurchases(): Flow<List<Transaction>>

    /** Income and expenses (no transfers) in [from, to) of active accounts included in the total, without excluded categories (REQ-CAT-04). */
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

/** Historial de cambios (REQ-HIS-02 to 04). */
interface ChangeLogRepository {
    /** Entries changed at or after [from], newest first. */
    fun observeSince(from: Instant): Flow<List<ChangeEntry>>

    /** Deletes, on this phone only, the entries changed before [cutoff]; the server purges its own copy (REQ-HIS-04). */
    suspend fun purgeBefore(cutoff: Instant)
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

/** Loans (S10): terms and plan; every write goes with its movements in one database transaction. */
interface LoanRepository {
    fun observeLoans(): Flow<List<Loan>>

    /** Not deleted installments of every loan, by due date. */
    fun observeInstallments(): Flow<List<LoanInstallment>>

    /** Payments to loans from [from] on (the pending payment of simple loans). */
    fun observePaymentsSince(from: Instant): Flow<List<Transaction>>

    /** SEQ-11: the LOAN account, its terms, its plan and the disbursement transfer (a new detailed loan only). */
    suspend fun create(
        account: Account,
        loan: Loan,
        installments: List<LoanInstallment>,
        disbursement: Transaction?,
    )

    /** New terms and plan of a stored loan (REQ-PRE-09: simple to detailed). */
    suspend fun update(
        loan: Loan,
        installments: List<LoanInstallment>,
    )

    /** A payment or an extra payment and the installments it settles, cancels or adds. */
    suspend fun pay(
        payment: Transaction,
        installments: List<LoanInstallment>,
    )
}
