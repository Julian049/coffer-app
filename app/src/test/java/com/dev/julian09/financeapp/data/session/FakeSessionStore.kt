package com.dev.julian09.financeapp.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory SessionStore for JVM tests (the real one needs Android Keystore). */
class FakeSessionStore(
    initial: StoredSession? = null,
) : SessionStore {
    private val state = MutableStateFlow(initial)
    override val session: StateFlow<StoredSession?> = state

    override suspend fun current() = state.value

    override suspend fun save(session: StoredSession) {
        state.value = session
    }

    override suspend fun markExpired() {
        state.value = state.value?.copy(expired = true)
    }

    override suspend fun clear() {
        state.value = null
    }
}

val TEST_USER = StoredUser("u-1", "julianb", "Julian", "julian@correo.com", "COP")
