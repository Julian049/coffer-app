package com.dev.julian09.financeapp.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/** Runs [SyncEngine] in the background (ADR-0006: the outbox is the table itself, WorkManager is the queue). */
@HiltWorker
class SyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val engine: SyncEngine,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result =
            try {
                engine.sync { received, total -> setProgress(workDataOf(RECEIVED to received, TOTAL to (total ?: -1L))) }
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                resultFor(e)
            }

        companion object {
            const val RECEIVED = "received"
            const val TOTAL = "total"

            /**
             * No network, a sleeping or failing server (RNF-06) → retry with exponential backoff. An expired session (the
             * authenticator already marked it), invalid data or another account's database → stop; retrying cannot help.
             */
            fun resultFor(e: Exception): Result =
                when {
                    e is IOException -> Result.retry()
                    e is HttpException && (e.code() >= 500 || e.code() == 429 || e.code() == 408) -> Result.retry()
                    else -> Result.failure()
                }
        }
    }
