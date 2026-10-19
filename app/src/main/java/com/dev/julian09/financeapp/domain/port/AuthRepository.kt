package com.dev.julian09.financeapp.domain.port

import com.dev.julian09.financeapp.domain.model.LoginInput
import com.dev.julian09.financeapp.domain.model.RegisterInput
import com.dev.julian09.financeapp.domain.model.SessionState
import com.dev.julian09.financeapp.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** Writes return Result; a failure is always an AuthFailure. */
interface AuthRepository {
    fun observeSession(): Flow<SessionState>

    suspend fun register(input: RegisterInput): Result<UserProfile>

    suspend fun login(input: LoginInput): Result<UserProfile>

    /** REQ-AUT-03: signs in, links by email or signs up with a Google ID token from Credential Manager. */
    suspend fun loginWithGoogle(idToken: String): Result<UserProfile>

    /** Revokes the refresh token when online and always forgets the session; local data stays (REQ-AUT-06). */
    suspend fun logout()

    /** GET /me, refreshing the stored profile. */
    suspend fun refreshProfile(): Result<UserProfile>
}
