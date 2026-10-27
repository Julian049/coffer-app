package com.dev.julian09.financeapp.data.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dev.julian09.financeapp.MainActivity
import com.dev.julian09.financeapp.R
import com.dev.julian09.financeapp.data.reminder.NotificationPublisher
import com.dev.julian09.financeapp.data.sync.SyncEngine
import com.dev.julian09.financeapp.data.sync.SyncWorker
import com.dev.julian09.financeapp.domain.port.DraftRepository
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * REQ-COR-10: syncs, then keeps a single notice with how many transactions wait for review (fixed id, so a second email
 * updates it instead of stacking another). With the setting off, or none left, the notice goes away.
 */
@HiltWorker
class DraftNoticeWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val engine: SyncEngine,
        private val drafts: DraftRepository,
        private val settings: SettingsRepository,
        private val notices: NotificationPublisher,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            try {
                engine.sync { _, _, _ -> }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return SyncWorker.resultFor(e)
            }
            val count = drafts.count()
            val manager = NotificationManagerCompat.from(applicationContext)
            if (count == 0 || !settings.observe().first().notifyDrafts || !notices.canNotify()) {
                manager.cancel(NOTICE_ID)
                return Result.success()
            }
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Movimientos por revisar", NotificationManager.IMPORTANCE_DEFAULT),
            )
            val open =
                PendingIntent.getActivity(
                    applicationContext,
                    NOTICE_ID,
                    Intent(applicationContext, MainActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP,
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val notice =
                NotificationCompat
                    .Builder(applicationContext, CHANNEL)
                    .setSmallIcon(R.drawable.ic_launcher_monochrome)
                    .setContentTitle(if (count == 1) "1 movimiento por revisar" else "$count movimientos por revisar")
                    .setContentText("Llegaron por correo. Revísalos para que cuenten en tu saldo.")
                    .setContentIntent(open)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .build()
            try {
                manager.notify(NOTICE_ID, notice)
            } catch (_: SecurityException) {
                // The permission was revoked between the check and the call
            }
            return Result.success()
        }

        companion object {
            const val NAME = "draft-notice"
            private const val CHANNEL = "drafts"
            private const val NOTICE_ID = 2_000_001
        }
    }
