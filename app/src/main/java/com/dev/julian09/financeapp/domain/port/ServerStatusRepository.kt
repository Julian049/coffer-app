package com.dev.julian09.financeapp.domain.port

interface ServerStatusRepository {
    /** True when the cloud service answers its health check; never throws. */
    suspend fun isAvailable(): Boolean
}
