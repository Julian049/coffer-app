package com.dev.julian09.financeapp.data.session

import kotlinx.coroutines.flow.Flow

/** Tokens and profile of this installation. Kept as an interface so network code can be tested without Android. */
interface SessionStore {
    val session: Flow<StoredSession?>

    suspend fun current(): StoredSession?

    suspend fun save(session: StoredSession)

    /** The refresh token was rejected: keep the profile (and all local data), ask to sign in again. */
    suspend fun markExpired()

    suspend fun clear()
}
