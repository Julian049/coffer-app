package com.dev.julian09.financeapp.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dev.julian09.financeapp.MainActivity
import com.dev.julian09.financeapp.R
import com.dev.julian09.financeapp.domain.model.Reminder
import com.dev.julian09.financeapp.domain.model.ReminderKind
import com.dev.julian09.financeapp.domain.port.ReminderNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private val ES_CO: Locale = Locale.forLanguageTag("es-CO")
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", ES_CO)

/** Channel "Pagos pendientes"; tapping a notice opens its Hoja · Pendiente in Home. */
class NotificationPublisher
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ReminderNotifier {
        override fun canNotify(): Boolean {
            val granted =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        override fun notify(reminder: Reminder) {
            if (!canNotify()) return
            val r = reminder.recurring
            val manager = NotificationManagerCompat.from(context)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Pagos pendientes", NotificationManager.IMPORTANCE_DEFAULT),
            )
            val amount = "$" + DecimalFormat("#,##0", DecimalFormatSymbols(ES_CO)).format(r.suggestedAmount.toBigDecimal())
            val title =
                when (reminder.kind) {
                    ReminderKind.WINDOW_OPEN -> "${r.name} vence el ${DAY_MONTH.format(r.nextDueDate).replace(".", "")}"
                    ReminderKind.DUE_TODAY -> "Hoy vence ${r.name}"
                }
            val open =
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_RECURRING_ID, r.id.toString())
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_launcher_monochrome)
                    .setContentTitle(title)
                    .setContentText("$amount · Toca para pagarlo u omitirlo")
                    .setAutoCancel(true)
                    .setContentIntent(
                        PendingIntent.getActivity(
                            context,
                            r.id.hashCode(),
                            open,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    ).build()
            try {
                manager.notify(r.id.hashCode(), notification)
            } catch (_: SecurityException) {
                // The permission was revoked between the check and the call: no notice this time
            }
        }

        private companion object {
            const val CHANNEL = "pending_payments"
        }
    }
