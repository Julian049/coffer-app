package com.dev.julian09.financeapp.data.local.dao

import androidx.room.Dao
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

@Dao
interface AccountDao {
    @Upsert
    suspend fun upsert(accounts: List<AccountEntity>)

    @Query("SELECT * FROM accounts WHERE deletedAt IS NULL AND active = 1 ORDER BY sortOrder, name")
    fun observeActive(): Flow<List<AccountEntity>>

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
