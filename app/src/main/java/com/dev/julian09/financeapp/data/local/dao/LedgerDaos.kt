package com.dev.julian09.financeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Upsert
import com.dev.julian09.financeapp.data.local.entity.AccountEntity
import com.dev.julian09.financeapp.data.local.entity.CategoryEntity
import com.dev.julian09.financeapp.data.local.entity.SettingsEntity
import com.dev.julian09.financeapp.data.local.entity.SyncMetaEntity
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.local.entity.TagEntity
import com.dev.julian09.financeapp.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
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

data class CategoryTotalRow(
    val categoryId: UUID,
    val total: Long,
    val count: Int,
)

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

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL AND occurredAt >= :from ORDER BY occurredAt DESC")
    fun observeSince(from: Instant): Flow<List<TransactionEntity>>

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
        WHERE t.deletedAt IS NULL AND a.deletedAt IS NULL AND a.active = 1 AND a.includeInTotal = 1
          AND t.occurredAt >= :from AND t.occurredAt < :to
        """,
    )
    fun observeTotals(
        from: Instant,
        to: Instant,
    ): Flow<TotalsRow>

    @Query("SELECT COUNT(*) FROM transactions WHERE deletedAt IS NULL")
    fun observeCount(): Flow<Int>

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

    /** Rows not yet in the cloud across every synced table (REQ-SYN-05). */
    @Query(
        "SELECT (SELECT COUNT(*) FROM settings WHERE syncState <> 'SYNCED') + " +
            "(SELECT COUNT(*) FROM accounts WHERE syncState <> 'SYNCED') + " +
            "(SELECT COUNT(*) FROM categories WHERE syncState <> 'SYNCED') + " +
            "(SELECT COUNT(*) FROM tags WHERE syncState <> 'SYNCED') + " +
            "(SELECT COUNT(*) FROM transactions WHERE syncState <> 'SYNCED')",
    )
    fun observeUnsynced(): Flow<Int>

    @Upsert
    suspend fun put(meta: SyncMetaEntity)
}
