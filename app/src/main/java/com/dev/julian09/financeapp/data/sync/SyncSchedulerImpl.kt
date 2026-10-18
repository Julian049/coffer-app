package com.dev.julian09.financeapp.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.dev.julian09.financeapp.data.local.FinanceDatabase
import com.dev.julian09.financeapp.domain.model.InitialSyncProgress
import com.dev.julian09.financeapp.domain.model.SyncStatus
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncSchedulerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val db: FinanceDatabase,
        private val connectivity: ConnectivityObserver,
    ) : SyncScheduler {
        private val workManager get() = WorkManager.getInstance(context)

        // WorkManager waits for the network itself: going back online is what releases pending runs
        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        override fun requestSync() {
            val request =
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(online)
                    .setInitialDelay(5, TimeUnit.SECONDS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .addTag(TAG)
                    .build()
            // KEEP: several edits in a row share one run
            workManager.enqueueUniqueWork(ONE_OFF, ExistingWorkPolicy.KEEP, request)
        }

        override fun requestInitialSync(): UUID {
            val request =
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(online)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                    .addTag(TAG)
                    .build()
            workManager.enqueueUniqueWork(INITIAL, ExistingWorkPolicy.REPLACE, request)
            return request.id
        }

        override fun ensurePeriodic() {
            val request =
                PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(online)
                    .addTag(TAG)
                    .build()
            workManager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        override fun cancelAll() {
            workManager.cancelAllWorkByTag(TAG)
        }

        override fun observeStatus(): Flow<SyncStatus> =
            combine(
                db.syncMeta().observeUnsynced(),
                workManager.getWorkInfosByTagFlow(TAG).map { infos -> infos.any { it.state == WorkInfo.State.RUNNING } },
                connectivity.observe(),
                db.syncMeta().observe(SyncMeta.LAST_SYNC_AT),
            ) { pending, running, online, lastSync ->
                when {
                    running -> SyncStatus.Syncing
                    !online -> SyncStatus.Offline
                    pending > 0 -> SyncStatus.Pending(pending)
                    else -> SyncStatus.Synced(lastSync?.let(Instant::parse))
                }
            }.distinctUntilChanged()

        override fun observeInitialSyncDone(): Flow<Boolean> = db.syncMeta().observe(SyncMeta.INITIAL_SYNC_DONE).map { it == "true" }

        override fun observeInitialSync(id: UUID): Flow<InitialSyncProgress> =
            workManager.getWorkInfoByIdFlow(id).map { info ->
                val progress = info?.progress
                InitialSyncProgress(
                    received = progress?.getLong(SyncWorker.RECEIVED, 0) ?: 0,
                    total = progress?.getLong(SyncWorker.TOTAL, -1)?.takeIf { it >= 0 },
                    byEntity =
                        progress
                            ?.keyValueMap
                            ?.filterKeys { it.startsWith(SyncWorker.ENTITY_PREFIX) }
                            ?.map { (k, v) -> k.removePrefix(SyncWorker.ENTITY_PREFIX) to (v as Long) }
                            ?.toMap()
                            .orEmpty(),
                    // Enqueued again after a failed attempt = waiting for the network or a sleeping server
                    waiting = info?.state == WorkInfo.State.ENQUEUED && (info.runAttemptCount > 0),
                    failed = info?.state == WorkInfo.State.FAILED,
                )
            }

        companion object {
            const val TAG = "sync"
            const val ONE_OFF = "sync"
            const val INITIAL = "initial-sync"
            const val PERIODIC = "sync-periodic"
        }
    }
