package com.dev.julian09.financeapp.data.local

import com.dev.julian09.financeapp.di.IoDispatcher
import com.dev.julian09.financeapp.domain.port.LocalDataEraser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomLocalDataEraser
    @Inject
    constructor(
        private val database: FinanceDatabase,
        @param:IoDispatcher private val io: CoroutineDispatcher,
    ) : LocalDataEraser {
        override suspend fun eraseAll() = withContext(io) { database.clearAllTables() }
    }
