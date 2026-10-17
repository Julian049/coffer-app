package com.dev.julian09.financeapp.data.remote

import com.dev.julian09.financeapp.data.remote.dto.PullResponseDto
import com.dev.julian09.financeapp.data.remote.dto.PushRequestDto
import com.dev.julian09.financeapp.data.remote.dto.PushResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** ADR-0006: push by batches of 200, pull pages of 500 from the global version cursor. */
interface SyncApi {
    @POST("api/v1/sync/push")
    suspend fun push(
        @Body body: PushRequestDto,
    ): PushResponseDto

    @GET("api/v1/sync/pull")
    suspend fun pull(
        @Query("since") since: Long,
        @Query("limit") limit: Int,
    ): PullResponseDto
}
