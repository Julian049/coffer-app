package com.dev.julian09.financeapp.data.session

import android.content.Context
import com.dev.julian09.financeapp.domain.port.CalendarAccountsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** A view preference, not ledger data: it stays on the phone and does not sync (like the legal consent). */
@Singleton
class SharedPrefsCalendarAccountsStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : CalendarAccountsStore {
        private val prefs = context.getSharedPreferences("calendar", Context.MODE_PRIVATE)
        private val ids =
            MutableStateFlow(
                prefs.getStringSet(KEY, null)?.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }?.toSet(),
            )

        override fun observe(): Flow<Set<UUID>?> = ids

        override suspend fun save(ids: Set<UUID>) {
            prefs.edit().putStringSet(KEY, ids.map { it.toString() }.toSet()).apply()
            this.ids.value = ids
        }

        private companion object {
            const val KEY = "accounts"
        }
    }
