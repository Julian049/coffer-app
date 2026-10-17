package com.dev.julian09.financeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dev.julian09.financeapp.data.local.entity.AccountEntity
import com.dev.julian09.financeapp.data.local.entity.CategoryEntity
import com.dev.julian09.financeapp.data.local.entity.SettingsEntity
import com.dev.julian09.financeapp.data.local.entity.SyncMetaEntity
import com.dev.julian09.financeapp.data.local.entity.TagEntity
import com.dev.julian09.financeapp.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Upsert
    suspend fun upsert(accounts: List<AccountEntity>)

    @Query("SELECT * FROM accounts WHERE deletedAt IS NULL AND active = 1 ORDER BY sortOrder, name")
    fun observeActive(): Flow<List<AccountEntity>>
}

@Dao
interface CategoryDao {
    @Upsert
    suspend fun upsert(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY kind, sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

@Dao
interface TagDao {
    @Upsert
    suspend fun upsert(tags: List<TagEntity>)
}

@Dao
interface TransactionDao {
    @Upsert
    suspend fun upsert(transactions: List<TransactionEntity>)

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE syncState <> 'SYNCED'")
    fun observeUnsynced(): Flow<Int>
}

@Dao
interface SettingsDao {
    @Upsert
    suspend fun upsert(settings: SettingsEntity)

    @Query("SELECT * FROM settings LIMIT 1")
    fun observe(): Flow<SettingsEntity?>
}

@Dao
interface SyncMetaDao {
    @Query("SELECT value FROM sync_meta WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Upsert
    suspend fun put(meta: SyncMetaEntity)
}
