package com.dev.julian09.financeapp.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import com.dev.julian09.financeapp.data.local.entity.AccountEntity
import com.dev.julian09.financeapp.data.local.entity.CategoryEntity
import com.dev.julian09.financeapp.data.local.entity.SyncMetaEntity
import com.dev.julian09.financeapp.data.local.entity.TransactionEntity
import com.dev.julian09.financeapp.di.IoDispatcher
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.port.Clock
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject

/** A transaction of the MVP database finance_db (v0.1.0). */
data class LegacyRow(
    val localId: String,
    val title: String,
    val value: Double,
    val description: String?,
    val date: String,
)

/**
 * One-time move of the MVP data into finance.db (data-model §5.2): SQLCipher cannot encrypt finance_db in place.
 * Same rules as the server's V6 and the same fixed UUIDs, so the first sync cannot duplicate anything.
 * Every row starts PENDING: "synced" in the MVP meant its old server, not the Coffer cloud.
 */
class LegacyDatabaseMigrator
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        // Lazy: injected at app start, but the encrypted database must open on the IO thread, not the main one
        private val database: Lazy<FinanceDatabase>,
        private val clock: Clock,
        @IoDispatcher private val io: CoroutineDispatcher,
    ) {
        suspend fun migrateIfNeeded() =
            withContext(io) {
                val db = database.get()
                if (db.syncMeta().get(MIGRATED) == "true") return@withContext
                val file = context.getDatabasePath(LEGACY_NAME)
                if (file.exists()) {
                    val rows = readLegacy(file.path)
                    db.withTransaction { insert(db, rows.mapNotNull { toTransaction(it, clock.now()) }, clock.now()) }
                    context.deleteDatabase(LEGACY_NAME)
                }
                db.syncMeta().put(SyncMetaEntity(MIGRATED, "true"))
            }

        private suspend fun insert(
            db: FinanceDatabase,
            transactions: List<TransactionEntity>,
            now: Instant,
        ) {
            if (transactions.isEmpty()) return
            db.accounts().upsert(
                listOf(
                    AccountEntity(
                        id = ACCOUNT,
                        name = "Efectivo (migrada)",
                        type = "CASH",
                        initialBalance = 0,
                        createdAt = now,
                        updatedAt = now,
                    ),
                ),
            )
            db.categories().upsert(
                listOf(
                    CategoryEntity(
                        id = EXPENSE_CATEGORY,
                        name = "Sin categoría",
                        kind = "EXPENSE",
                        isBase = true,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    CategoryEntity(
                        id = INCOME_CATEGORY,
                        name = "Sin categoría",
                        kind = "INCOME",
                        isBase = true,
                        createdAt = now,
                        updatedAt = now,
                    ),
                ),
            )
            db.transactions().upsert(transactions)
        }

        private fun readLegacy(path: String): List<LegacyRow> =
            SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use { legacy ->
                legacy.rawQuery("SELECT localId, title, value, description, date FROM transactions", null).use { c ->
                    buildList {
                        while (c.moveToNext()) {
                            add(LegacyRow(c.getString(0), c.getString(1), c.getDouble(2), c.getString(3), c.getString(4)))
                        }
                    }
                }
            }

        companion object {
            const val LEGACY_NAME = "finance_db"
            const val MIGRATED = "legacy_migrated"
            val ACCOUNT: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000a1")
            val EXPENSE_CATEGORY: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000b1")
            val INCOME_CATEGORY: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000b2")

            /** V6 rules: sign → type, absolute amount, "title · description" note; a zero amount does not exist any more. */
            fun toTransaction(
                row: LegacyRow,
                now: Instant,
            ): TransactionEntity? {
                if (row.value == 0.0) return null
                // toString keeps what the user typed (no binary fraction noise)
                val amount = Money.of(BigDecimal(row.value.toString()).abs())
                val expense = row.value < 0
                val occurredAt = parseDate(row.date) ?: now
                val note = (row.title + (row.description?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")).take(500)
                return TransactionEntity(
                    id = UUID.fromString(row.localId),
                    type = if (expense) "EXPENSE" else "INCOME",
                    amount = amount.scaled,
                    occurredAt = occurredAt,
                    accountId = ACCOUNT,
                    categoryId = if (expense) EXPENSE_CATEGORY else INCOME_CATEGORY,
                    note = note,
                    createdAt = occurredAt,
                    updatedAt = now,
                )
            }

            /** The MVP stored ISO-8601 offsets, or "<epochMillis> <H>:<m>" when its own parsing failed. */
            fun parseDate(raw: String): Instant? =
                runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
                    ?: raw.substringBefore(' ').toLongOrNull()?.let(Instant::ofEpochMilli)
        }
    }
