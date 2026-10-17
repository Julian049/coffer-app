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
import androidx.work.WorkManager
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncSchedulerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
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

        companion object {
            const val TAG = "sync"
            const val ONE_OFF = "sync"
            const val INITIAL = "initial-sync"
            const val PERIODIC = "sync-periodic"
        }
    }
