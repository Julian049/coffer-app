package com.dev.julian09.financeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dev.julian09.financeapp.data.local.dao.AccountDao
import com.dev.julian09.financeapp.data.local.dao.CategoryDao
import com.dev.julian09.financeapp.data.local.dao.SettingsDao
import com.dev.julian09.financeapp.data.local.dao.SyncMetaDao
import com.dev.julian09.financeapp.data.local.dao.TagDao
import com.dev.julian09.financeapp.data.local.dao.TransactionDao
import com.dev.julian09.financeapp.data.local.entity.AccountEntity
import com.dev.julian09.financeapp.data.local.entity.CategoryEntity
import com.dev.julian09.financeapp.data.local.entity.SettingsEntity
import com.dev.julian09.financeapp.data.local.entity.SyncMetaEntity
import com.dev.julian09.financeapp.data.local.entity.TagEntity
import com.dev.julian09.financeapp.data.local.entity.TransactionEntity

/**
 * finance.db, encrypted with SQLCipher (ADR-0008). Version 1 with exported schemas (app/schemas); every later change
 * is a tested Migration, never a destructive fallback (data-model §5.2).
 */
@Database(
    entities = [
        SettingsEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        TransactionEntity::class,
        SyncMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao

    abstract fun categories(): CategoryDao

    abstract fun tags(): TagDao

    abstract fun transactions(): TransactionDao

    abstract fun settings(): SettingsDao

    abstract fun syncMeta(): SyncMetaDao

    companion object {
        const val NAME = "finance.db"
    }
}
