package com.dev.julian09.financeapp.data.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderSchedulerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val clock: Clock,
    ) : ReminderScheduler {
        // KEEP: the first run is placed near 08:00 once; Android may move it a little to save battery
        override fun ensureScheduled() {
            val now = clock.now().atZone(clock.zone())
            val eight = now.toLocalDate().atTime(LocalTime.of(8, 0)).atZone(clock.zone())
            val next = if (now.isBefore(eight)) eight else eight.plusDays(1)
            val request =
                PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(Duration.between(now, next))
                    .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        override fun cancel() {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }

        private companion object {
            const val NAME = "reminders"
        }
    }
