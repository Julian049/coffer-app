package com.dev.julian09.financeapp.legacy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val localId: String,
    val title: String,
    val value: Double,
    val type: Boolean,
    val description: String?,
    val date: String,
    val synced: Boolean = false,
)
