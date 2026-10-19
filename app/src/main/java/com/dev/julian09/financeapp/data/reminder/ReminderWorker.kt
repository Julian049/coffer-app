package com.dev.julian09.financeapp.data.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dev.julian09.financeapp.domain.usecase.SendReminders
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** S6-DAT02: the daily check of pending payments; it only reads the local database, so it needs no network. */
@HiltWorker
class ReminderWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val sendReminders: SendReminders,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            sendReminders()
            return Result.success()
        }
    }
