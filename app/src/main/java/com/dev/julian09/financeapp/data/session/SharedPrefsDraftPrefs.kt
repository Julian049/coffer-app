package com.dev.julian09.financeapp.data.session

import android.content.Context
import com.dev.julian09.financeapp.domain.port.DraftPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Stays on the phone (roadmap-v2.4 §4, open question: sync across devices only if the PO asks). */
@Singleton
class SharedPrefsDraftPrefs
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : DraftPrefs {
        private val prefs = context.getSharedPreferences("drafts", Context.MODE_PRIVATE)
        private val autoSave = MutableStateFlow(prefs.getBoolean(AUTO_SAVE, true))
        private val registered =
            MutableStateFlow(
                prefs.getStringSet(REGISTERED, emptySet()).orEmpty().mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() },
            )

        override fun observeAutoSave(): Flow<Boolean> = autoSave

        override suspend fun setAutoSave(on: Boolean) {
            prefs.edit().putBoolean(AUTO_SAVE, on).apply()
            autoSave.value = on
        }

        override fun observeRegistered(): Flow<List<UUID>> = registered

        override suspend fun addRegistered(transactionId: UUID) = saveRegistered(registered.value + transactionId)

        override suspend fun clearRegistered() = saveRegistered(emptyList())

        private val resets =
            MutableStateFlow(
                prefs
                    .getStringSet(RESETS, emptySet())
                    .orEmpty()
                    .mapNotNull {
                        it.substringAfterLast('=', "").toLongOrNull()?.let { ms ->
                            it.substringBeforeLast('=') to
                                Instant.ofEpochMilli(ms)
                        }
                    }.toMap(),
            )

        override fun observeResets(): Flow<Map<String, Instant>> = resets

        override suspend fun resetLearning(
            key: String,
            at: Instant,
        ) {
            val all = resets.value + (key to at)
            prefs.edit().putStringSet(RESETS, all.map { (k, v) -> "$k=${v.toEpochMilli()}" }.toSet()).apply()
            resets.value = all
        }

        private fun saveRegistered(ids: List<UUID>) {
            prefs.edit().putStringSet(REGISTERED, ids.map { it.toString() }.toSet()).apply()
            registered.value = ids
        }

        private companion object {
            const val AUTO_SAVE = "autoSave"
            const val REGISTERED = "registered"
            const val RESETS = "resets"
        }
    }
