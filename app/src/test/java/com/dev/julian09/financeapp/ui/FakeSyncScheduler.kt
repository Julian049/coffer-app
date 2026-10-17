package com.dev.julian09.financeapp.ui

import com.dev.julian09.financeapp.domain.model.InitialSyncProgress
import com.dev.julian09.financeapp.domain.model.SyncStatus
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Records what the UI asks of the sync work; progress is pushed per work id through [progress]. */
class FakeSyncScheduler : SyncScheduler {
    var cancelled = false
    var syncRequests = 0
    var periodic = false
    val initialRequests = mutableListOf<UUID>()
    val status = MutableStateFlow<SyncStatus>(SyncStatus.Synced(null))
    val initialDone = MutableStateFlow(false)
    val progress = MutableStateFlow<Pair<UUID, InitialSyncProgress>?>(null)

    override fun requestSync() {
        syncRequests++
    }

    override fun requestInitialSync(): UUID = UUID.randomUUID().also(initialRequests::add)

    override fun ensurePeriodic() {
        periodic = true
    }

    override fun cancelAll() {
        cancelled = true
    }

    override fun observeStatus() = status

    override fun observeInitialSyncDone() = initialDone

    override fun observeInitialSync(id: UUID) = progress.filter { it?.first == id }.map { it!!.second }
}
