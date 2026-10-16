package com.dev.julian09.financeapp.legacy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TransactionEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
