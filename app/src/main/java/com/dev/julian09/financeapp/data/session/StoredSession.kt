package com.dev.julian09.financeapp.data.session

import com.dev.julian09.financeapp.domain.model.UserProfile
import kotlinx.serialization.Serializable

/** What survives app restarts; written encrypted (EncryptedSessionStore). */
@Serializable
data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val user: StoredUser,
    val expired: Boolean = false,
)

@Serializable
data class StoredUser(
    val id: String,
    val username: String,
    val displayName: String,
    val email: String,
    val currency: String,
    val googleLinked: Boolean = false,
) {
    fun toDomain() = UserProfile(id, username, displayName, email, currency, googleLinked)
}
