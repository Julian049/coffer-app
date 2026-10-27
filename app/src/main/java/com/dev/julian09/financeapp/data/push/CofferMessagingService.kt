package com.dev.julian09.financeapp.data.push

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.dev.julian09.financeapp.domain.port.PushTokens
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ADR-0016: a data message only says «there are drafts»; the phone syncs and counts them itself. */
@AndroidEntryPoint
class CofferMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var tokens: PushTokens

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { tokens.register() }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        WorkManager.getInstance(this).enqueueUniqueWork(
            DraftNoticeWorker.NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DraftNoticeWorker>().setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build(),
        )
    }
}
