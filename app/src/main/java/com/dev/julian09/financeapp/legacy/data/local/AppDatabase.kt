package com.dev.julian09.financeapp.legacy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema of the MVP database (`finance_db`) still present on installed phones. The v0.1.0 screens were removed
 * in v0.2.0 and nothing opens it now; Sprint 3 (`LegacyDatabaseMigrator`, data-model §5) moves its rows into the
 * encrypted database and deletes this package.
 */
@Database(entities = [TransactionEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
