package com.dev.julian09.financeapp.domain.model

/** What the sync indicator shows. Sprint 3 feeds it from the outbox and the worker. */
sealed interface SyncStatus {
    data object Synced : SyncStatus

    data class Pending(
        val count: Int,
    ) : SyncStatus

    data object Syncing : SyncStatus

    data object Offline : SyncStatus
}
