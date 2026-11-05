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
    private val links = MutableStateFlow(emptyMap<String, UUID>())

    override fun observeRecurringLinks(): Flow<Map<String, UUID>> = links

    override suspend fun linkRecurring(
        key: String,
        recurringId: UUID,
    ) {
        links.value += key to recurringId
    }

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

/** The recurring-expense side of a draft, over the fixture's real repositories. */
fun draftRecurring(
    f: com.dev.julian09.financeapp.data.local.LedgerFixture,
    prefs: DraftPrefs = FakeDraftPrefs(),
    sync: com.dev.julian09.financeapp.domain.port.SyncScheduler = FakeSyncScheduler(),
) = com.dev.julian09.financeapp.domain.usecase.DraftRecurring(
    f.recurring,
    f.drafts,
    com.dev.julian09.financeapp.domain.usecase.PayPending(
        f.recurring,
        com.dev.julian09.financeapp.domain.usecase
            .TransactionValidator(f.accounts, f.categories),
        sync,
    ),
    prefs,
    f.clock,
    sync,
)
