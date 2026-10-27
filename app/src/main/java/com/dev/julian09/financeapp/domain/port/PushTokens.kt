package com.dev.julian09.financeapp.domain.port

/** ADR-0016: this phone's FCM token on the server, so a bank email can wake it. No-ops while Firebase is not set up. */
interface PushTokens {
    suspend fun register()

    /** Before signing out, while the session can still call the server. */
    suspend fun unregister()
}
