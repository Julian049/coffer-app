package com.dev.julian09.financeapp.legacy.domain.model

data class Transaction(
    val localId: String,
    val title: String,
    val value: Double,
    val type: Boolean,
    val description: String?,
    val date: String,
    val synced: Boolean,
)
