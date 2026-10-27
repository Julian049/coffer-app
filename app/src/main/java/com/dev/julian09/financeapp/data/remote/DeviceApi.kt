package com.dev.julian09.financeapp.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.PUT

@Serializable
data class DeviceTokenDto(
    val token: String,
)

/** ADR-0016: PUT and DELETE /api/v1/me/device-token. */
interface DeviceApi {
    @PUT("api/v1/me/device-token")
    suspend fun register(
        @Body body: DeviceTokenDto,
    )

    @HTTP(method = "DELETE", path = "api/v1/me/device-token", hasBody = true)
    suspend fun remove(
        @Body body: DeviceTokenDto,
    )
}
