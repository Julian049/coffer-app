package com.dev.julian09.financeapp.domain.port

import com.dev.julian09.financeapp.domain.model.InitialSyncProgress
import com.dev.julian09.financeapp.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** When the background sync runs (SEQ-02, 08). */
interface SyncScheduler {
    /** Soon after a local change: one pending run at a time, with network. */
    fun requestSync()

    /** First download after sign-in, as soon as possible; returns the work id to follow its progress. */
    fun requestInitialSync(): UUID

    /** Every 15 minutes with network, while signed in. */
    fun ensurePeriodic()

    /** Sign-out: no sync until the next sign-in (REQ-AUT-06). */
    fun cancelAll()

    /** Pending changes, running sync, connectivity and last sync time combined for the indicator. */
    fun observeStatus(): Flow<SyncStatus>

    /** False until the first download of this database finished (start shows Descargando tus datos meanwhile). */
    fun observeInitialSyncDone(): Flow<Boolean>

    fun observeInitialSync(id: UUID): Flow<InitialSyncProgress>
}
