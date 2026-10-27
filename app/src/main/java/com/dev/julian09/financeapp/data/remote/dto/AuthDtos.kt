package com.dev.julian09.financeapp.data.remote.dto

import com.dev.julian09.financeapp.data.session.StoredUser
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val displayName: String,
    val email: String,
    val username: String,
    val password: String,
    val deviceId: String,
)

@Serializable
data class LoginRequestDto(
    val identifier: String,
    val password: String,
    val deviceId: String,
)

/** One proof of identity: the password, or a fresh Google ID token for accounts without one. */
@Serializable
data class DeleteAccountRequestDto(
    val password: String? = null,
    val idToken: String? = null,
)

/** REQ-AUT-09: asks for the 6-digit code. */
@Serializable
data class PasswordResetRequestDto(
    val email: String,
)

@Serializable
data class PasswordResetConfirmRequestDto(
    val email: String,
    val code: String,
    val newPassword: String,
)

@Serializable
data class RefreshRequestDto(
    val refreshToken: String,
    val deviceId: String,
)

@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val displayName: String,
    val email: String,
    val currency: String,
    val googleLinked: Boolean = false,
    val forwardingAddress: String? = null,
    val forwardingConfirmation: String? = null,
) {
    fun toStored() = StoredUser(id, username, displayName, email, currency, googleLinked, forwardingAddress, forwardingConfirmation)
}

/** RFC 9457 body of every server error (openapi.yml Problem / ValidationProblem). */
@Serializable
data class ProblemDto(
    val code: String? = null,
    val detail: String? = null,
    val errors: List<FieldErrorDto> = emptyList(),
)

@Serializable
data class FieldErrorDto(
    val field: String,
    val message: String,
)

@Serializable
data class GoogleLoginRequestDto(
    val idToken: String,
    val deviceId: String,
)
