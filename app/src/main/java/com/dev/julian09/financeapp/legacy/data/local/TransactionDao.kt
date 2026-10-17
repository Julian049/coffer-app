package com.dev.julian09.financeapp.legacy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transactionEntity: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE synced = 0")
    suspend fun getUnSyncedTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TransactionEntity>)

    @Query("UPDATE transactions SET synced = 1 WHERE synced = 0")
    suspend fun updateSyncedStatus()
}
