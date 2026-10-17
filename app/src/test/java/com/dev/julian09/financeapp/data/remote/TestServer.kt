package com.dev.julian09.financeapp.data.remote

import com.dev.julian09.financeapp.data.session.DeviceIdProvider
import com.dev.julian09.financeapp.data.session.SessionStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

val testJson = Json { ignoreUnknownKeys = true }

/** Same wiring as NetworkModule, pointed at a MockWebServer. */
class TestNetwork(
    server: MockWebServer,
    store: SessionStore,
) {
    private val deviceId = mockk<DeviceIdProvider> { every { deviceId } returns "device-test-01" }
    private lateinit var retrofit: Retrofit
    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val client: OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(AuthInterceptor(store))
            .authenticator(TokenAuthenticator(store, deviceId) { authApi })
            .build()

    init {
        retrofit =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(testJson.asConverterFactory("application/json".toMediaType()))
                .build()
    }

    val device: DeviceIdProvider get() = deviceId
}

fun authBody(
    access: String,
    refresh: String,
) = """{"accessToken":"$access","accessTokenExpiresAt":"2026-10-17T03:00:00Z","refreshToken":"$refresh",
       "user":{"id":"u-1","username":"julianb","displayName":"Julian","email":"julian@correo.com","currency":"COP"}}"""
