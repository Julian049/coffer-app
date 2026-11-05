package com.dev.julian09.financeapp.ui

import com.dev.julian09.financeapp.domain.port.DraftPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.util.UUID

class FakeDraftPrefs(
    autoSave: Boolean = true,
) : DraftPrefs {
    private val auto = MutableStateFlow(autoSave)
    private val registered = MutableStateFlow(emptyList<UUID>())
    private val resets = MutableStateFlow(emptyMap<String, Instant>())

    override fun observeAutoSave(): Flow<Boolean> = auto

    override suspend fun setAutoSave(on: Boolean) {
        auto.value = on
    }

    override fun observeRegistered(): Flow<List<UUID>> = registered

    override suspend fun addRegistered(transactionId: UUID) {
        registered.value += transactionId
    }

    override suspend fun clearRegistered() {
        registered.value = emptyList()
    }

    override fun observeResets(): Flow<Map<String, Instant>> = resets

    override suspend fun resetLearning(
        key: String,
        at: Instant,
    ) {
        resets.value += key to at
    }
}
