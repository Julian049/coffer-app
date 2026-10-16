package com.dev.julian09.financeapp.legacy.data

import android.util.Log
import com.dev.julian09.financeapp.legacy.data.local.TransactionDao
import com.dev.julian09.financeapp.legacy.data.local.TransactionEntity
import com.dev.julian09.financeapp.legacy.data.remote.ApiService
import com.dev.julian09.financeapp.legacy.data.remote.TransactionDto
import com.dev.julian09.financeapp.legacy.domain.model.Transaction
import com.dev.julian09.financeapp.legacy.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.collections.map

class TransactionRepositoryImpl(
    private val dao: TransactionDao,
    private val api: ApiService,
) : TransactionRepository {
    override suspend fun healthCheck(): Boolean =
        try {
            val response = api.healthCheck()
            response.code() == 200
        } catch (e: Exception) {
            Log.e("REPOSITORIOLOG", "Error en el health check: ${e.message}")
            false
        }

    fun getLocalTransactions(): Flow<List<Transaction>> =
        dao.getAllTransactions().map { entities ->
            entities.map { entity ->
                Transaction(
                    entity.localId,
                    entity.title,
                    entity.value,
                    entity.type,
                    entity.description,
                    entity.date,
                    entity.synced,
                )
            }
        }

    override fun getTransactions(): Flow<List<Transaction>> {
        try {
            Log.d("REPOSITORIOLOG", "Obteniendo transacciones...")
            runBlocking {
                dao.insertAll(
                    api.getAllTransactions().map { dto ->
                        TransactionEntity(
                            dto.localId,
                            dto.title,
                            dto.value.toDouble(),
                            dto.type,
                            dto.description,
                            dto.date,
                            dto.synced,
                        )
                    },
                )
            }
            Log.d("REPOSITORIOLOG", "Transacciones obtenidas correctamente")
        } catch (e: Exception) {
            Log.e("REPOSITORIOLOG", "Error al sincronizar: ${e.message}")
        }
        return getLocalTransactions()
    }

    override suspend fun syncTransactions() {
        try {
            Log.d("REPOSITORIOLOG", "Iniciando sincronización...")
            val unSyncedTransactions = dao.getUnSyncedTransactions()
            if (unSyncedTransactions.isEmpty()) {
                Log.d("REPOSITORIOLOG", "No hay transacciones sin sincronizar.")
                return
            }
            Log.d("REPOSITORIOLOG", "${unSyncedTransactions.size} transacciones sin sincronizar.")
            unSyncedTransactions.forEach { entity ->
                api.createTransaction(
                    TransactionDto(
                        entity.localId,
                        entity.title,
                        entity.value.toString(),
                        entity.type,
                        entity.description,
                        formatDate(entity.date),
                        entity.synced,
                    ),
                )
            }
            dao.updateSyncedStatus()
            Log.d(
                "REPOSITORIOLOG",
                "Sincronización completada: ${unSyncedTransactions.size} elementos.",
            )
        } catch (e: Exception) {
            Log.e("REPOSITORIOLOG", "Error al sincronizar: ${e.message}")
        }
    }

    override suspend fun addTransaction(transaction: Transaction) {
        val entity =
            TransactionEntity(
                transaction.localId,
                transaction.title,
                transaction.value,
                transaction.type,
                transaction.description,
                formatDate(transaction.date),
                transaction.synced,
            )
        dao.insert(entity)
    }

    override suspend fun getTotalAmount(transactions: List<Transaction>): Double {
        var total = 0.0
        Log.d("TOTALAMOUNT", "Inicia a calcular el saldo $total")
        transactions.forEach { transaction ->
            total += transaction.value
            Log.d("TOTALAMOUNT", "Saldo: $total")
        }

        return total
    }

    private fun formatDate(raw: String): String =
        try {
            val parts = raw.trim().split(" ")
            val millis = parts[0].toLong()
            val timePart = if (parts.size > 1) parts[1] else "00:00"
            val (hour, minute) = timePart.split(":").map { it.toInt() }

            val instant = Instant.ofEpochMilli(millis)
            val odt =
                OffsetDateTime
                    .ofInstant(instant, ZoneOffset.UTC)
                    .withHour(hour)
                    .withMinute(minute)
                    .withSecond(0)
                    .withNano(0)

            odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } catch (e: Exception) {
            raw
        }
}
