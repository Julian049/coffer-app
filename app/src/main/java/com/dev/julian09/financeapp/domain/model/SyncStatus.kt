package com.dev.julian09.financeapp.domain.model

import java.time.Instant

/** What the sync indicator shows (REQ-SYN-05): pending changes and when the last sync happened. */
sealed interface SyncStatus {
    data class Synced(
        val lastSyncAt: Instant?,
    ) : SyncStatus

    data class Pending(
        val count: Int,
    ) : SyncStatus

    data object Syncing : SyncStatus

    data object Offline : SyncStatus
}

/** Initial download (SEQ-02): rows received so far, per entity, and the total when the server reports it. */
data class InitialSyncProgress(
    val received: Long = 0,
    val total: Long? = null,
    val byEntity: Map<String, Long> = emptyMap(),
    /** No network or the server is waking up: the worker retries with backoff (RNF-06). */
    val waiting: Boolean = false,
    /** Stopped for good (expired session, another account's data): the user must act. */
    val failed: Boolean = false,
)
