package com.dev.julian09.financeapp.data.remote

import com.dev.julian09.financeapp.data.remote.dto.HealthDto
import retrofit2.http.GET
import retrofit2.http.Headers

interface HealthApi {
    // Without this header Actuator answers with its own vnd media type instead of the contract's JSON
    @Headers("Accept: application/json")
    @GET("actuator/health")
    suspend fun health(): HealthDto
}
