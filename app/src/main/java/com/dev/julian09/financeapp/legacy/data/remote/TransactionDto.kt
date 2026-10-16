package com.dev.julian09.financeapp.legacy.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val localId: String,
    val title: String,
    // The server sends money as a JSON string ("28000.0000") since v0.2.0
    val value: String,
    val type: Boolean,
    val description: String? = null,
    val date: String,
    val synced: Boolean = true,
)
