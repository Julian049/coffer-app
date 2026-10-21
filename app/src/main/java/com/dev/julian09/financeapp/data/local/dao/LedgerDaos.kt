package com.dev.julian09.financeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Upsert
import com.dev.julian09.financeapp.data.local.entity.AccountEntity
import com.dev.julian09.financeapp.data.local.entity.CategoryEntity
import com.dev.julian09.financeapp.data.local.entity.ChangeLogEntity
import com.dev.julian09.financeapp.data.local.entity.CreditCardDetailsEntity
import com.dev.julian09.financeapp.data.local.entity.DeferredPurchaseEntity
import com.dev.julian09.financeapp.data.local.entity.InstallmentEntity
import com.dev.julian09.financeapp.data.local.entity.RecurringExpenseEntity
import com.dev.julian09.financeapp.data.local.entity.SettingsEntity
import com.dev.julian09.financeapp.data.local.entity.SyncMetaEntity
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.local.entity.TagEntity
import com.dev.julian09.financeapp.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Sync state of one local row, to decide last-write-wins against a pulled change (SEQ-09). */
data class SyncInfo(
    val syncState: SyncState,
    val updatedAt: Instant,
)

/** An account with what moved through it (detailed-design §6.6.3), amounts ×10⁴. */
data class AccountFlowRow(
    @Embedded val account: AccountEntity,
    val income: Long,
    val expense: Long,
    val transfersOut: Long,
    val transfersIn: Long,
    val transactions: Int,
)

data class ImpactRow(
    val count: Int,
    val firstAt: Instant?,
    val lastAt: Instant?,
)

data class TotalsRow(
    val income: Long,
    val expense: Long,
)

data class SearchTotalsRow(
    val income: Long,
    val expense: Long,
    val count: Int,
)

data class CategoryTotalRow(
    val categoryId: UUID,
    val total: Long,
    val count: Int,
)

/**
 * The combinable filters of the history (REQ-TRX-09, 10). An empty list means "any"; the flags say so because
 * SQLite cannot tell an empty IN list from a missing one. [like] is "" or "%text%" with % and _ escaped by a backslash.
 */
private const val SEARCH_FROM =
    """
    FROM transactions t
    LEFT JOIN categories c ON c.id = t.categoryId
    LEFT JOIN categories p ON p.id = c.parentId
    LEFT JOIN accounts a ON a.id = t.accountId
    LEFT JOIN accounts d ON d.id = t.destinationAccountId
    LEFT JOIN tags g ON g.id = t.tagId
    WHERE t.deletedAt IS NULL AND t.occurredAt >= :from AND t.occurredAt < :to
      AND (:anyType OR t.type IN (:types))
      AND (:anyCategory OR t.categoryId IN (:categoryIds))
      AND (:anyAccount OR t.accountId IN (:accountIds) OR t.destinationAccountId IN (:accountIds))
      AND (:anyTag OR t.tagId IN (:tagIds))
      AND (:like = '' OR t.note LIKE :like ESCAPE '\' OR c.name LIKE :like ESCAPE '\' OR p.name LIKE :like ESCAPE '\'
           OR a.name LIKE :like ESCAPE '\' OR d.name LIKE :like ESCAPE '\' OR g.name LIKE :like ESCAPE '\')
    """

@Dao
interface AccountDao {
    @Upsert
    suspend fun upsert(accounts: List<AccountEntity>)

    @Query("SELECT * FROM accounts WHERE deletedAt IS NULL AND active = 1 ORDER BY sortOrder, name")
    fun observeActive(): Flow<List<AccountEntity>>

    /** Not deleted accounts, active or not; only movements before [before] count. */
    @Query(
        """
        SELECT a.*,
          COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.accountId = a.id AND t.type = 'INCOME'
                    AND t.deletedAt IS NULL AND t.occurredAt < :before), 0) AS income,
          COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.accountId = a.id AND t.type = 'EXPENSE'
                    AND t.deletedAt IS NULL AND t.occurredAt < :before), 0) AS expense,
          COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.accountId = a.id AND t.type = 'TRANSFER'
                    AND t.deletedAt IS NULL AND t.occurredAt < :before), 0) AS transfersOut,
          COALESCE((SELECT SUM(COALESCE(t.principal, t.amount)) FROM transactions t WHERE t.destinationAccountId = a.id
                    AND t.type = 'TRANSFER' AND t.deletedAt IS NULL AND t.occurredAt < :before), 0) AS transfersIn,
          (SELECT COUNT(*) FROM transactions t WHERE (t.accountId = a.id OR t.destinationAccountId = a.id)
                    AND t.deletedAt IS NULL) AS transactions
        FROM accounts a WHERE a.deletedAt IS NULL ORDER BY a.sortOrder, a.name
        """,
    )
    fun observeBalances(before: Instant): Flow<List<AccountFlowRow>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun get(id: UUID): AccountEntity?

    @Query(
        "UPDATE accounts SET active = :active, deactivatedAt = :deactivatedAt, updatedAt = :now, syncState = 'PENDING' " +
            "WHERE id = :id",
    )
    suspend fun setActive(
        id: UUID,
        active: Boolean,
        deactivatedAt: Instant?,
        now: Instant,
    )

    @Query("UPDATE accounts SET deletedAt = :now, updatedAt = :now, syncState = 'PENDING' WHERE id = :id")
    suspend fun markDeleted(
        id: UUID,
        now: Instant,
    )

    @Query("SELECT * FROM accounts WHERE syncState = 'PENDING' ORDER BY updatedAt LIMIT :limit")
    suspend fun pending(limit: Int): List<AccountEntity>

    @Query("SELECT syncState, updatedAt FROM accounts WHERE id = :id")
    suspend fun syncInfo(id: UUID): SyncInfo?

    /** Only if the row was not edited while it was being sent; otherwise it stays PENDING for the next push. */
    @Query("UPDATE accounts SET syncState = 'SYNCED', syncError = NULL WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markSynced(
        id: UUID,
        sentUpdatedAt: Instant,
    ): Int

    @Query("UPDATE accounts SET syncState = 'FAILED', syncError = :error WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markFailed(
        id: UUID,
        sentUpdatedAt: Instant,
        error: String,
    ): Int
}

@Dao
interface CategoryDao {
    @Upsert
    suspend fun upsert(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY kind, sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("UPDATE categories SET name = :name, updatedAt = :now, syncState = 'PENDING' WHERE id = :id")
    suspend fun rename(
        id: UUID,
        name: String,
        now: Instant,
    )

    @Query("UPDATE categories SET excludeFromReports = :excluded, updatedAt = :now, syncState = 'PENDING' WHERE id = :id")
    suspend fun setExcludedFromReports(
        id: UUID,
        excluded: Boolean,
        now: Instant,
    )

    /** The category and its subcategories (REQ-CAT-03). */
    @Query(
        "UPDATE categories SET deletedAt = :now, updatedAt = :now, syncState = 'PENDING' " +
            "WHERE (id = :id OR parentId = :id) AND deletedAt IS NULL",
    )
    suspend fun deleteWithChildren(
        id: UUID,
        now: Instant,
    )

    @Query("SELECT * FROM categories WHERE syncState = 'PENDING' ORDER BY updatedAt LIMIT :limit")
    suspend fun pending(limit: Int): List<CategoryEntity>

    @Query("SELECT syncState, updatedAt FROM categories WHERE id = :id")
    suspend fun syncInfo(id: UUID): SyncInfo?

    /** Only if the row was not edited while it was being sent; otherwise it stays PENDING for the next push. */
    @Query("UPDATE categories SET syncState = 'SYNCED', syncError = NULL WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markSynced(
        id: UUID,
        sentUpdatedAt: Instant,
    ): Int

    @Query("UPDATE categories SET syncState = 'FAILED', syncError = :error WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markFailed(
        id: UUID,
        sentUpdatedAt: Instant,
        error: String,
    ): Int
}

@Dao
interface TagDao {
    @Upsert
    suspend fun upsert(tags: List<TagEntity>)

    @Query("SELECT * FROM tags WHERE deletedAt IS NULL ORDER BY pinned DESC, name")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE syncState = 'PENDING' ORDER BY updatedAt LIMIT :limit")
    suspend fun pending(limit: Int): List<TagEntity>

    @Query("SELECT syncState, updatedAt FROM tags WHERE id = :id")
    suspend fun syncInfo(id: UUID): SyncInfo?

    /** Only if the row was not edited while it was being sent; otherwise it stays PENDING for the next push. */
    @Query("UPDATE tags SET syncState = 'SYNCED', syncError = NULL WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markSynced(
        id: UUID,
        sentUpdatedAt: Instant,
    ): Int

    @Query("UPDATE tags SET syncState = 'FAILED', syncError = :error WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markFailed(
        id: UUID,
        sentUpdatedAt: Instant,
        error: String,
    ): Int
}

@Dao
interface TransactionDao {
    @Upsert
    suspend fun upsert(transactions: List<TransactionEntity>)

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE syncState <> 'SYNCED'")
    fun observeUnsynced(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: UUID): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id AND deletedAt IS NULL")
    fun observe(id: UUID): Flow<TransactionEntity?>

    /** REQ-TRX-05 / SEQ-04: the account of the last movement registered, if it is still active. */
    @Query(
        "SELECT t.accountId FROM transactions t JOIN accounts a ON a.id = t.accountId " +
            "WHERE t.deletedAt IS NULL AND a.deletedAt IS NULL AND a.active = 1 ORDER BY t.createdAt DESC LIMIT 1",
    )
    suspend fun lastUsedAccountId(): UUID?

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL AND occurredAt >= :from ORDER BY occurredAt DESC")
    fun observeSince(from: Instant): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL AND occurredAt >= :from AND occurredAt < :to")
    fun observeBetween(
        from: Instant,
        to: Instant,
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL AND installments > 1")
    fun observeDeferredPurchases(): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE deletedAt IS NULL AND (accountId = :accountId OR destinationAccountId = :accountId) " +
            "ORDER BY occurredAt DESC",
    )
    fun observeByAccount(accountId: UUID): Flow<List<TransactionEntity>>

    /** Income and expenses of active accounts included in the total. */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount END), 0) AS income,
               COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount END), 0) AS expense
        FROM transactions t JOIN accounts a ON a.id = t.accountId
          LEFT JOIN categories c ON c.id = t.categoryId LEFT JOIN categories p ON p.id = c.parentId
        WHERE t.deletedAt IS NULL AND a.deletedAt IS NULL AND a.active = 1 AND a.includeInTotal = 1
          AND t.occurredAt >= :from AND t.occurredAt < :to
          AND COALESCE(p.excludeFromReports, c.excludeFromReports, 0) = 0
        """,
    )
    fun observeTotals(
        from: Instant,
        to: Instant,
    ): Flow<TotalsRow>

    @Query("SELECT COUNT(*) FROM transactions WHERE deletedAt IS NULL")
    fun observeCount(): Flow<Int>

    /** REQ-TRX-06, 12: by when it happened, newest first; [limit] grows as the list scrolls. */
    @Query("SELECT t.* $SEARCH_FROM ORDER BY t.occurredAt DESC LIMIT :limit")
    fun search(
        from: Instant,
        to: Instant,
        anyType: Boolean,
        types: List<String>,
        anyCategory: Boolean,
        categoryIds: List<UUID>,
        anyAccount: Boolean,
        accountIds: List<UUID>,
        anyTag: Boolean,
        tagIds: List<UUID>,
        like: String,
        limit: Int,
    ): Flow<List<TransactionEntity>>

    /** The same filters, summed for the summary card; transfers are neither (REQ-TRX-03). */
    @Query(
        "SELECT COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount END), 0) AS income, " +
            "COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount END), 0) AS expense, COUNT(*) AS count $SEARCH_FROM",
    )
    fun searchTotals(
        from: Instant,
        to: Instant,
        anyType: Boolean,
        types: List<String>,
        anyCategory: Boolean,
        categoryIds: List<UUID>,
        anyAccount: Boolean,
        accountIds: List<UUID>,
        anyTag: Boolean,
        tagIds: List<UUID>,
        like: String,
    ): Flow<SearchTotalsRow>

    @Query("SELECT MIN(occurredAt) FROM transactions WHERE deletedAt IS NULL")
    fun observeFirstOccurredAt(): Flow<Instant?>

    @Query(
        "SELECT * FROM transactions WHERE deletedAt IS NULL AND recurringId IS NOT NULL " +
            "AND occurredAt >= :from AND occurredAt < :to ORDER BY occurredAt DESC",
    )
    fun observeRecurringPayments(
        from: Instant,
        to: Instant,
    ): Flow<List<TransactionEntity>>

    @Query(
        "SELECT categoryId, SUM(amount) AS total, COUNT(*) AS count FROM transactions WHERE deletedAt IS NULL " +
            "AND categoryId IS NOT NULL AND occurredAt >= :from AND occurredAt < :to GROUP BY categoryId",
    )
    fun observeCategoryTotals(
        from: Instant,
        to: Instant,
    ): Flow<List<CategoryTotalRow>>

    @Query(
        "SELECT COUNT(*) AS count, MIN(occurredAt) AS firstAt, MAX(occurredAt) AS lastAt FROM transactions " +
            "WHERE deletedAt IS NULL AND (accountId = :accountId OR destinationAccountId = :accountId)",
    )
    suspend fun accountImpact(accountId: UUID): ImpactRow

    /** Transfers between both accounts would become transfers to itself after a move: they disappear (SEQ-12). */
    @Query(
        "UPDATE transactions SET deletedAt = :now, updatedAt = :now, syncState = 'PENDING' WHERE deletedAt IS NULL " +
            "AND ((accountId = :a AND destinationAccountId = :b) OR (accountId = :b AND destinationAccountId = :a))",
    )
    suspend fun deleteTransfersBetween(
        a: UUID,
        b: UUID,
        now: Instant,
    )

    @Query("UPDATE transactions SET accountId = :to, updatedAt = :now, syncState = 'PENDING' WHERE accountId = :from AND deletedAt IS NULL")
    suspend fun moveSource(
        from: UUID,
        to: UUID,
        now: Instant,
    )

    @Query(
        "UPDATE transactions SET destinationAccountId = :to, updatedAt = :now, syncState = 'PENDING' " +
            "WHERE destinationAccountId = :from AND deletedAt IS NULL",
    )
    suspend fun moveDestination(
        from: UUID,
        to: UUID,
        now: Instant,
    )

    @Query(
        "UPDATE transactions SET deletedAt = :now, updatedAt = :now, syncState = 'PENDING' WHERE deletedAt IS NULL " +
            "AND (accountId = :accountId OR destinationAccountId = :accountId)",
    )
    suspend fun deleteForAccount(
        accountId: UUID,
        now: Instant,
    )

    @Query(
        "SELECT COUNT(*) FROM transactions WHERE deletedAt IS NULL " +
            "AND categoryId IN (SELECT id FROM categories WHERE id = :categoryId OR parentId = :categoryId)",
    )
    suspend fun countInCategory(categoryId: UUID): Int

    @Query(
        "UPDATE transactions SET categoryId = :to, updatedAt = :now, syncState = 'PENDING' WHERE deletedAt IS NULL " +
            "AND categoryId IN (SELECT id FROM categories WHERE id = :from OR parentId = :from)",
    )
    suspend fun reassignCategory(
        from: UUID,
        to: UUID,
        now: Instant,
    )

    @Query("SELECT * FROM transactions WHERE syncState = 'PENDING' ORDER BY updatedAt LIMIT :limit")
    suspend fun pending(limit: Int): List<TransactionEntity>

    @Query("SELECT syncState, updatedAt FROM transactions WHERE id = :id")
    suspend fun syncInfo(id: UUID): SyncInfo?

    /** Only if the row was not edited while it was being sent; otherwise it stays PENDING for the next push. */
    @Query("UPDATE transactions SET syncState = 'SYNCED', syncError = NULL WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markSynced(
        id: UUID,
        sentUpdatedAt: Instant,
    ): Int

    @Query("UPDATE transactions SET syncState = 'FAILED', syncError = :error WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markFailed(
        id: UUID,
        sentUpdatedAt: Instant,
        error: String,
    ): Int
}

@Dao
interface SettingsDao {
    @Upsert
    suspend fun upsert(settings: SettingsEntity)

    @Upsert
    suspend fun upsertAll(settings: List<SettingsEntity>)

    @Query("SELECT COUNT(*) FROM settings")
    suspend fun count(): Int

    @Query("SELECT * FROM settings LIMIT 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE syncState = 'PENDING' ORDER BY updatedAt LIMIT :limit")
    suspend fun pending(limit: Int): List<SettingsEntity>

    @Query("SELECT syncState, updatedAt FROM settings WHERE id = :id")
    suspend fun syncInfo(id: UUID): SyncInfo?

    /** Only if the row was not edited while it was being sent; otherwise it stays PENDING for the next push. */
    @Query("UPDATE settings SET syncState = 'SYNCED', syncError = NULL WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markSynced(
        id: UUID,
        sentUpdatedAt: Instant,
    ): Int

    @Query("UPDATE settings SET syncState = 'FAILED', syncError = :error WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markFailed(
        id: UUID,
        sentUpdatedAt: Instant,
        error: String,
    ): Int
}

@Dao
interface SyncMetaDao {
    @Query("SELECT value FROM sync_meta WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM sync_meta WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    /** Rows not yet in the cloud across every synced table (REQ-SYN-05); change_log is left out so a movement counts once. */
    @Query(
        "SELECT (SELECT COUNT(*) FROM settings WHERE syncState <> 'SYNCED') + " +
            "(SELECT COUNT(*) FROM accounts WHERE syncState <> 'SYNCED') + " +
            "(SELECT COUNT(*) FROM categories WHERE syncState <> 'SYNCED') + " +
            "(SELECT COUNT(*) FROM tags WHERE syncState <> 'SYNCED') + " +
            "(SELECT COUNT(*) FROM recurring_expenses WHERE syncState <> 'SYNCED') + " +
            "(SELECT COUNT(*) FROM transactions WHERE syncState <> 'SYNCED')",
    )
    fun observeUnsynced(): Flow<Int>

    @Upsert
    suspend fun put(meta: SyncMetaEntity)
}

@Dao
interface ChangeLogDao {
    @Upsert
    suspend fun upsert(rows: List<ChangeLogEntity>)

    @Query("SELECT * FROM change_log WHERE transactionId = :transactionId ORDER BY changedAt")
    suspend fun forTransaction(transactionId: UUID): List<ChangeLogEntity>

    @Query("SELECT * FROM change_log WHERE syncState = 'PENDING' ORDER BY updatedAt LIMIT :limit")
    suspend fun pending(limit: Int): List<ChangeLogEntity>

    @Query("SELECT syncState, updatedAt FROM change_log WHERE id = :id")
    suspend fun syncInfo(id: UUID): SyncInfo?

    @Query("UPDATE change_log SET syncState = 'SYNCED', syncError = NULL WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markSynced(
        id: UUID,
        sentUpdatedAt: Instant,
    ): Int

    @Query("UPDATE change_log SET syncState = 'FAILED', syncError = :error WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markFailed(
        id: UUID,
        sentUpdatedAt: Instant,
        error: String,
    ): Int
}

@Dao
interface RecurringDao {
    @Upsert
    suspend fun upsert(rows: List<RecurringExpenseEntity>)

    @Query("SELECT * FROM recurring_expenses WHERE deletedAt IS NULL ORDER BY nextDueDate, name")
    fun observeAll(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id AND deletedAt IS NULL")
    fun observe(id: UUID): Flow<RecurringExpenseEntity?>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun get(id: UUID): RecurringExpenseEntity?

    @Query("UPDATE recurring_expenses SET lastNotifiedOn = :on, updatedAt = :now, syncState = 'PENDING' WHERE id = :id")
    suspend fun markNotified(
        id: UUID,
        on: LocalDate,
        now: Instant,
    )

    @Query("SELECT * FROM recurring_expenses WHERE syncState = 'PENDING' ORDER BY updatedAt LIMIT :limit")
    suspend fun pending(limit: Int): List<RecurringExpenseEntity>

    @Query("SELECT syncState, updatedAt FROM recurring_expenses WHERE id = :id")
    suspend fun syncInfo(id: UUID): SyncInfo?

    /** Only if the row was not edited while it was being sent; otherwise it stays PENDING for the next push. */
    @Query("UPDATE recurring_expenses SET syncState = 'SYNCED', syncError = NULL WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markSynced(
        id: UUID,
        sentUpdatedAt: Instant,
    ): Int

    @Query("UPDATE recurring_expenses SET syncState = 'FAILED', syncError = :error WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markFailed(
        id: UUID,
        sentUpdatedAt: Instant,
        error: String,
    ): Int
}

/** Card terms, deferred purchases and installments (V8); the sync queries come once per table. */
@Dao
interface CardDao {
    @Upsert
    suspend fun upsertCards(rows: List<CreditCardDetailsEntity>)

    @Upsert
    suspend fun upsertDeferred(rows: List<DeferredPurchaseEntity>)

    @Upsert
    suspend fun upsertInstallments(rows: List<InstallmentEntity>)

    @Query("SELECT * FROM credit_card_details WHERE deletedAt IS NULL")
    fun observeCards(): Flow<List<CreditCardDetailsEntity>>

    @Query("SELECT * FROM credit_card_details WHERE id = :id")
    suspend fun getCard(id: UUID): CreditCardDetailsEntity?

    @Query("SELECT * FROM deferred_purchases WHERE deletedAt IS NULL ORDER BY firstDueDate DESC")
    fun observeDeferred(): Flow<List<DeferredPurchaseEntity>>

    @Query("SELECT * FROM installments WHERE deletedAt IS NULL AND deferredPurchaseId IS NOT NULL ORDER BY dueDate, number")
    fun observeInstallments(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun getInstallment(id: UUID): InstallmentEntity?

    @Query(
        "SELECT t.* FROM transactions t WHERE t.deletedAt IS NULL AND (" +
            "t.accountId IN (SELECT id FROM accounts WHERE type = 'CREDIT_CARD') OR " +
            "t.destinationAccountId IN (SELECT id FROM accounts WHERE type = 'CREDIT_CARD')) ORDER BY t.occurredAt DESC",
    )
    fun observeCardTransactions(): Flow<List<TransactionEntity>>

    /** Deleting a purchase takes its plan with it (logical delete, synced). */
    @Query(
        "UPDATE installments SET deletedAt = :now, updatedAt = :now, syncState = 'PENDING' WHERE deletedAt IS NULL " +
            "AND deferredPurchaseId IN (SELECT id FROM deferred_purchases WHERE purchaseTransactionId = :transactionId)",
    )
    suspend fun deletePlanOf(
        transactionId: UUID,
        now: Instant,
    )

    @Query(
        "UPDATE deferred_purchases SET deletedAt = :now, updatedAt = :now, syncState = 'PENDING' " +
            "WHERE deletedAt IS NULL AND purchaseTransactionId = :transactionId",
    )
    suspend fun deleteDeferredOf(
        transactionId: UUID,
        now: Instant,
    )

    @Query("SELECT * FROM credit_card_details WHERE syncState = 'PENDING' ORDER BY updatedAt LIMIT :limit")
    suspend fun pendingCard(limit: Int): List<CreditCardDetailsEntity>

    @Query("SELECT syncState, updatedAt FROM credit_card_details WHERE id = :id")
    suspend fun syncInfoCard(id: UUID): SyncInfo?

    @Query("UPDATE credit_card_details SET syncState = 'SYNCED', syncError = NULL WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markSyncedCard(
        id: UUID,
        sentUpdatedAt: Instant,
    ): Int

    @Query("UPDATE credit_card_details SET syncState = 'FAILED', syncError = :error WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markFailedCard(
        id: UUID,
        sentUpdatedAt: Instant,
        error: String,
    ): Int

    @Query("SELECT * FROM deferred_purchases WHERE syncState = 'PENDING' ORDER BY updatedAt LIMIT :limit")
    suspend fun pendingDeferred(limit: Int): List<DeferredPurchaseEntity>

    @Query("SELECT syncState, updatedAt FROM deferred_purchases WHERE id = :id")
    suspend fun syncInfoDeferred(id: UUID): SyncInfo?

    @Query("UPDATE deferred_purchases SET syncState = 'SYNCED', syncError = NULL WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markSyncedDeferred(
        id: UUID,
        sentUpdatedAt: Instant,
    ): Int

    @Query("UPDATE deferred_purchases SET syncState = 'FAILED', syncError = :error WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markFailedDeferred(
        id: UUID,
        sentUpdatedAt: Instant,
        error: String,
    ): Int

    @Query("SELECT * FROM installments WHERE syncState = 'PENDING' ORDER BY updatedAt LIMIT :limit")
    suspend fun pendingInstallment(limit: Int): List<InstallmentEntity>

    @Query("SELECT syncState, updatedAt FROM installments WHERE id = :id")
    suspend fun syncInfoInstallment(id: UUID): SyncInfo?

    @Query("UPDATE installments SET syncState = 'SYNCED', syncError = NULL WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markSyncedInstallment(
        id: UUID,
        sentUpdatedAt: Instant,
    ): Int

    @Query("UPDATE installments SET syncState = 'FAILED', syncError = :error WHERE id = :id AND updatedAt = :sentUpdatedAt")
    suspend fun markFailedInstallment(
        id: UUID,
        sentUpdatedAt: Instant,
        error: String,
    ): Int
}
