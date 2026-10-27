package com.dev.julian09.financeapp.data.repository

import com.dev.julian09.financeapp.data.remote.AuthApi
import com.dev.julian09.financeapp.data.remote.dto.AuthResponseDto
import com.dev.julian09.financeapp.data.remote.dto.DeleteAccountRequestDto
import com.dev.julian09.financeapp.data.remote.dto.GoogleLoginRequestDto
import com.dev.julian09.financeapp.data.remote.dto.LoginRequestDto
import com.dev.julian09.financeapp.data.remote.dto.PasswordResetConfirmRequestDto
import com.dev.julian09.financeapp.data.remote.dto.PasswordResetRequestDto
import com.dev.julian09.financeapp.data.remote.dto.ProblemDto
import com.dev.julian09.financeapp.data.remote.dto.RefreshRequestDto
import com.dev.julian09.financeapp.data.remote.dto.RegisterRequestDto
import com.dev.julian09.financeapp.data.session.DeviceIdProvider
import com.dev.julian09.financeapp.data.session.SessionStore
import com.dev.julian09.financeapp.data.session.StoredSession
import com.dev.julian09.financeapp.domain.model.AuthFailure
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.domain.model.LoginInput
import com.dev.julian09.financeapp.domain.model.RegisterInput
import com.dev.julian09.financeapp.domain.model.SessionState
import com.dev.julian09.financeapp.domain.model.UserProfile
import com.dev.julian09.financeapp.domain.port.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val api: AuthApi,
        private val store: SessionStore,
        private val deviceId: DeviceIdProvider,
        private val json: Json,
    ) : AuthRepository {
        override fun observeSession(): Flow<SessionState> =
            store.session.map { session ->
                when {
                    session == null -> SessionState.SignedOut
                    session.expired -> SessionState.Expired(session.user.toDomain())
                    else -> SessionState.Active(session.user.toDomain())
                }
            }

        override suspend fun register(input: RegisterInput): Result<UserProfile> =
            authenticate {
                api.register(
                    RegisterRequestDto(input.displayName.trim(), input.email.trim(), input.username, input.password, deviceId.deviceId),
                )
            }

        override suspend fun login(input: LoginInput): Result<UserProfile> =
            authenticate { api.login(LoginRequestDto(input.identifier.trim(), input.password, deviceId.deviceId)) }

        override suspend fun loginWithGoogle(idToken: String): Result<UserProfile> =
            authenticate { api.google(GoogleLoginRequestDto(idToken, deviceId.deviceId)) }

        override suspend fun logout() {
            val session = store.current()
            try {
                // Best effort: offline, the token simply expires on the server
                if (session != null && !session.expired) api.logout(RefreshRequestDto(session.refreshToken, deviceId.deviceId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Signing out must work without network (SEQ-03)
            } finally {
                store.clear()
            }
        }

        override suspend fun deleteAccount(
            password: String?,
            idToken: String?,
        ): Result<Unit> = runMapped { api.deleteAccount(DeleteAccountRequestDto(password, idToken)) }

        override suspend fun requestPasswordReset(email: String): Result<Unit> =
            runMapped { api.requestPasswordReset(PasswordResetRequestDto(email.trim())) }

        override suspend fun confirmPasswordReset(
            email: String,
            code: String,
            newPassword: String,
        ): Result<Unit> =
            runMapped { api.confirmPasswordReset(PasswordResetConfirmRequestDto(email.trim(), code, newPassword)) }
                .recoverCatching { e ->
                    // The server's 400 for a bad code lists no field: it belongs to the code
                    throw if (e is AuthFailure.Invalid && e.fields.isEmpty()) {
                        AuthFailure.Invalid(mapOf(AuthField.Code to "El código no es válido o ya venció. Pide uno nuevo."))
                    } else {
                        e
                    }
                }

        override suspend fun clearSession() = store.clear()

        override suspend fun refreshProfile(): Result<UserProfile> =
            runMapped {
                val user = api.me().toStored()
                store.current()?.let { store.save(it.copy(user = user)) }
                user.toDomain()
            }

        private suspend fun authenticate(call: suspend () -> AuthResponseDto): Result<UserProfile> =
            runMapped {
                val response = call()
                val user = response.user.toStored()
                store.save(StoredSession(response.accessToken, response.refreshToken, user))
                user.toDomain()
            }

        private suspend fun <T> runMapped(block: suspend () -> T): Result<T> =
            try {
                Result.success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(toFailure(e))
            }

        private fun toFailure(e: Exception): AuthFailure =
            when (e) {
                is HttpException -> fromProblem(e)
                is IOException -> AuthFailure.NoConnection
                else -> AuthFailure.Unexpected
            }

        private fun fromProblem(e: HttpException): AuthFailure {
            val problem =
                runCatching {
                    json.decodeFromString<ProblemDto>(
                        e
                            .response()
                            ?.errorBody()
                            ?.string()
                            .orEmpty(),
                    )
                }.getOrNull()
            return when (problem?.code) {
                "FIN-VALIDATION" ->
                    AuthFailure.Invalid(
                        problem.errors
                            .mapNotNull { err ->
                                FIELDS[err.field]?.let { it to err.message }
                            }.toMap(),
                    )
                // shortcut: the contract's Problem has no field member, so the server's wording tells username from email
                "FIN-CONFLICT" ->
                    AuthFailure.Taken(
                        if (problem.detail.orEmpty().contains("correo")) AuthField.Email else AuthField.Username,
                        problem.detail ?: "Ya existe una cuenta con esos datos",
                    )
                // On the public auth routes it means bad credentials; anywhere else, the session is gone
                "FIN-AUTH-INVALID" -> if (e.isPublicRoute()) AuthFailure.WrongCredentials else AuthFailure.SessionExpired
                "FIN-CONFIRMATION" -> AuthFailure.ConfirmationFailed
                "FIN-AUTH-REVOKED" -> AuthFailure.SessionExpired
                "FIN-RATE-LIMIT" ->
                    AuthFailure.TooManyAttempts(
                        e
                            .response()
                            ?.headers()
                            ?.get("Retry-After")
                            ?.toLongOrNull() ?: 60,
                    )
                else -> AuthFailure.Unexpected
            }
        }

        private fun HttpException.isPublicRoute() =
            response()
                ?.raw()
                ?.request
                ?.url
                ?.encodedPath in AuthApi.PUBLIC_PATHS

        private companion object {
            val FIELDS =
                mapOf(
                    "displayName" to AuthField.DisplayName,
                    "email" to AuthField.Email,
                    "username" to AuthField.Username,
                    "password" to AuthField.Password,
                    "identifier" to AuthField.Identifier,
                    "code" to AuthField.Code,
                    "newPassword" to AuthField.Password,
                )
        }
    }
