package com.dev.julian09.financeapp.data.push

import android.content.Context
import android.util.Log
import com.dev.julian09.financeapp.BuildConfig
import com.dev.julian09.financeapp.data.remote.DeviceApi
import com.dev.julian09.financeapp.data.remote.DeviceTokenDto
import com.dev.julian09.financeapp.domain.port.PushTokens
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** ADR-0016: Firebase from BuildConfig (coffer.firebase.* in gradle.properties) instead of google-services.json. */
object FirebasePush {
    @Volatile
    var enabled = false
        private set

    fun init(context: Context) {
        if (BuildConfig.FIREBASE_APP_ID.isBlank() || FirebaseApp.getApps(context).isNotEmpty()) return
        FirebaseApp.initializeApp(
            context,
            FirebaseOptions
                .Builder()
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                .build(),
        )
        enabled = true
    }
}

@Singleton
class FirebasePushTokens
    @Inject
    constructor(
        private val api: DeviceApi,
    ) : PushTokens {
        override suspend fun register() {
            val token = token() ?: return
            send { api.register(DeviceTokenDto(token)) }
        }

        override suspend fun unregister() {
            val token = token() ?: return
            send { api.remove(DeviceTokenDto(token)) }
            FirebaseMessaging.getInstance().deleteToken()
        }

        /** The server never refuses a sign-in or a sign-out because of a notice: errors are only logged. */
        private suspend fun send(call: suspend () -> Unit) {
            try {
                call()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Device token not sent: ${e.message}")
            }
        }

        private suspend fun token(): String? {
            if (!FirebasePush.enabled) return null
            return suspendCancellableCoroutine { cont ->
                FirebaseMessaging
                    .getInstance()
                    .token
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
        }

        private companion object {
            const val TAG = "PushTokens"
        }
    }
