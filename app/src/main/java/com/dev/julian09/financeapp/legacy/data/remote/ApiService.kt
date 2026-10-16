package com.dev.julian09.financeapp.legacy.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("actuator/health")
    suspend fun healthCheck(): Response<Unit>

    @GET("/transactions")
    suspend fun getAllTransactions(): List<TransactionDto>

    @POST("/transactions")
    suspend fun createTransaction(
        @Body transaction: TransactionDto,
    ): TransactionDto
}
