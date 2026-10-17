package com.dev.julian09.financeapp.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** A change on the wire (openapi.yml Change / PulledChange); [data] depends on [entity]. Instants are ISO-8601. */
@Serializable
data class ChangeDto(
    val entity: String,
    val id: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val data: JsonObject,
    val version: Long? = null,
)

@Serializable
data class PushRequestDto(
    val deviceId: String,
    val changes: List<ChangeDto>,
)

@Serializable
data class ChangeResultDto(
    val id: String,
    val status: String,
    val version: Long? = null,
    val code: String? = null,
    val detail: String? = null,
)

@Serializable
data class PushResponseDto(
    val results: List<ChangeResultDto>,
)

@Serializable
data class PullResponseDto(
    val changes: List<ChangeDto>,
    val nextCursor: Long,
    val hasMore: Boolean,
    val remaining: Long? = null,
)
