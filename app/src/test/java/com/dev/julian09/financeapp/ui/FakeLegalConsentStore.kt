package com.dev.julian09.financeapp.ui

import com.dev.julian09.financeapp.domain.port.LegalConsentStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeLegalConsentStore : LegalConsentStore {
    val versions = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun acceptedVersion(userId: String) = versions.map { it[userId] }

    override suspend fun accept(
        userId: String,
        version: String,
    ) = versions.update { it + (userId to version) }
}
