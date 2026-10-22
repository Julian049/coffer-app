package com.dev.julian09.financeapp.data.session

import android.content.Context
import com.dev.julian09.financeapp.domain.port.LegalConsentStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** One preference per user id holding the accepted version; it says nothing secret, so plain SharedPreferences. */
@Singleton
class SharedPrefsLegalConsentStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : LegalConsentStore {
        private val prefs = context.getSharedPreferences("legal", Context.MODE_PRIVATE)
        private val versions = MutableStateFlow(prefs.all.mapNotNull { (user, v) -> (v as? String)?.let { user to it } }.toMap())

        override fun acceptedVersion(userId: String): Flow<String?> = versions.map { it[userId] }.distinctUntilChanged()

        override suspend fun accept(
            userId: String,
            version: String,
        ) {
            prefs.edit().putString(userId, version).apply()
            versions.update { it + (userId to version) }
        }
    }
