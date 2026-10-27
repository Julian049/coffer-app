package com.dev.julian09.financeapp.data.remote

import com.dev.julian09.financeapp.data.remote.dto.AuthResponseDto
import com.dev.julian09.financeapp.data.remote.dto.DeleteAccountRequestDto
import com.dev.julian09.financeapp.data.remote.dto.GoogleLoginRequestDto
import com.dev.julian09.financeapp.data.remote.dto.LoginRequestDto
import com.dev.julian09.financeapp.data.remote.dto.PasswordResetConfirmRequestDto
import com.dev.julian09.financeapp.data.remote.dto.PasswordResetRequestDto
import com.dev.julian09.financeapp.data.remote.dto.RefreshRequestDto
import com.dev.julian09.financeapp.data.remote.dto.RegisterRequestDto
import com.dev.julian09.financeapp.data.remote.dto.UserDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/register")
    suspend fun register(
        @Body body: RegisterRequestDto,
    ): AuthResponseDto

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body body: LoginRequestDto,
    ): AuthResponseDto

    @POST("api/v1/auth/google")
    suspend fun google(
        @Body body: GoogleLoginRequestDto,
    ): AuthResponseDto

    /** 202 whether or not the email exists (REQ-AUT-10). */
    @POST("api/v1/auth/password-reset")
    suspend fun requestPasswordReset(
        @Body body: PasswordResetRequestDto,
    )

    /** 204: the password changed and every session was revoked. */
    @POST("api/v1/auth/password-reset/confirm")
    suspend fun confirmPasswordReset(
        @Body body: PasswordResetConfirmRequestDto,
    )

    /** Blocking on purpose: it runs inside OkHttp's Authenticator, which is already on a background thread. */
    @POST("api/v1/auth/refresh")
    fun refresh(
        @Body body: RefreshRequestDto,
    ): Call<AuthResponseDto>

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Body body: RefreshRequestDto,
    )

    /** DELETE with a body, which Retrofit's @DELETE does not allow. */
    @HTTP(method = "DELETE", path = "api/v1/me", hasBody = true)
    suspend fun deleteAccount(
        @Body body: DeleteAccountRequestDto,
    )

    @GET("api/v1/me")
    suspend fun me(): UserDto

    companion object {
        /** Routes that must never carry a bearer token nor trigger a refresh. */
        val PUBLIC_PATHS =
            setOf(
                "/api/v1/auth/register",
                "/api/v1/auth/login",
                "/api/v1/auth/google",
                "/api/v1/auth/refresh",
                "/api/v1/auth/password-reset",
                "/api/v1/auth/password-reset/confirm",
            )
    }
}
