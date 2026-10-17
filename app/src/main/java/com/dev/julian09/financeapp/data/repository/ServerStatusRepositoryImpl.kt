package com.dev.julian09.financeapp.data.repository

import com.dev.julian09.financeapp.data.remote.HealthApi
import com.dev.julian09.financeapp.di.IoDispatcher
import com.dev.julian09.financeapp.domain.port.ServerStatusRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class ServerStatusRepositoryImpl
    @Inject
    constructor(
        private val api: HealthApi,
        @IoDispatcher private val io: CoroutineDispatcher,
    ) : ServerStatusRepository {
        override suspend fun isAvailable(): Boolean =
            withContext(io) {
                try {
                    api.health().status == "UP"
                } catch (e: IOException) {
                    false
                } catch (e: HttpException) {
                    false
                } catch (e: SerializationException) {
                    false
                }
            }
    }
