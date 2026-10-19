package com.dev.julian09.financeapp.ui

import com.dev.julian09.financeapp.domain.model.LoginInput
import com.dev.julian09.financeapp.domain.model.RegisterInput
import com.dev.julian09.financeapp.domain.model.SessionState
import com.dev.julian09.financeapp.domain.model.UserProfile
import com.dev.julian09.financeapp.domain.port.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow

val PROFILE = UserProfile("u-1", "julianb", "Julian Bocanegra", "julian@correo.com", "COP", false)

/** Scripted AuthRepository for ViewModel tests: set [next] to the result the next call returns. */
class FakeAuthRepository : AuthRepository {
    val session = MutableStateFlow<SessionState>(SessionState.SignedOut)
    var next: Result<UserProfile> = Result.success(PROFILE)
    var calls = 0
    var loggedOut = false

    override fun observeSession() = session

    override suspend fun register(input: RegisterInput) = respond()

    override suspend fun login(input: LoginInput) = respond()

    var googleTokens = mutableListOf<String>()

    override suspend fun loginWithGoogle(idToken: String): Result<UserProfile> {
        googleTokens += idToken
        return respond()
    }

    override suspend fun logout() {
        loggedOut = true
        session.value = SessionState.SignedOut
    }

    override suspend fun refreshProfile() = Result.success(PROFILE)

    private fun respond(): Result<UserProfile> {
        calls++
        next.onSuccess { session.value = SessionState.Active(it) }
        return next
    }
}
