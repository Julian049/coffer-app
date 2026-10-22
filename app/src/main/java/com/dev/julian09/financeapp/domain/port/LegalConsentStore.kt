package com.dev.julian09.financeapp.domain.port

import kotlinx.coroutines.flow.Flow

/** Which version of the terms and privacy policy each user accepted on this phone (S14-DAT02); null = never. */
interface LegalConsentStore {
    fun acceptedVersion(userId: String): Flow<String?>

    suspend fun accept(
        userId: String,
        version: String,
    )
}
