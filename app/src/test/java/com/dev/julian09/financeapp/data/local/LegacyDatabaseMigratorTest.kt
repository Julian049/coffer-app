package com.dev.julian09.financeapp.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.dev.julian09.financeapp.data.local.LegacyDatabaseMigrator.Companion.ACCOUNT
import com.dev.julian09.financeapp.data.local.LegacyDatabaseMigrator.Companion.EXPENSE_CATEGORY
import com.dev.julian09.financeapp.data.local.LegacyDatabaseMigrator.Companion.INCOME_CATEGORY
import com.dev.julian09.financeapp.data.local.entity.SyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

/** S3-QA02: the MVP finance_db moves into finance.db with the server's V6 rules and fixed UUIDs. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LegacyDatabaseMigratorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = inMemoryDatabase()
    private val clock = FixedClock()
    private val migrator = LegacyDatabaseMigrator(context, { db }, clock, Dispatchers.IO)

    @After
    fun close() = db.close()

    @Test
    fun movesEveryMvpRowAndDeletesTheOldDatabase() =
        runBlocking {
            legacyDatabase(
                row("11111111-1111-1111-1111-111111111111", "Almuerzo", -28000.0, "con Ana", "2026-05-01T12:00:00Z", synced = true),
                row("22222222-2222-2222-2222-222222222222", "Sueldo", 3500000.5, null, "1746086400000 8:0", synced = false),
                row("33333333-3333-3333-3333-333333333333", "Cero", 0.0, null, "2026-05-03T08:00:00Z", synced = true),
            )

            migrator.migrateIfNeeded()

            val txs =
                db
                    .transactions()
                    .observeRecent(10)
                    .first()
                    .sortedBy { it.occurredAt }
            assertEquals(2, txs.size)
            with(txs.first { it.note == "Almuerzo · con Ana" }) {
                assertEquals("EXPENSE", type)
                assertEquals(28000_0000L, amount)
                assertEquals(EXPENSE_CATEGORY, categoryId)
                assertEquals(Instant.parse("2026-05-01T12:00:00Z"), occurredAt)
                // Uploaded to the MVP server is not uploaded to Coffer: it must go up again
                assertEquals(SyncState.PENDING, syncState)
            }
            with(txs.first { it.note == "Sueldo" }) {
                assertEquals("INCOME", type)
                assertEquals(3500000_5000L, amount)
                assertEquals(INCOME_CATEGORY, categoryId)
                assertEquals(Instant.ofEpochMilli(1746086400000), occurredAt)
            }
            assertEquals(ACCOUNT, txs.first().accountId)
            assertEquals(
                "Efectivo (migrada)",
                db
                    .accounts()
                    .observeActive()
                    .first()
                    .single()
                    .name,
            )
            assertEquals(2, db.categories().count())
            assertFalse(context.getDatabasePath(LegacyDatabaseMigrator.LEGACY_NAME).exists())
            assertEquals("true", db.syncMeta().get(LegacyDatabaseMigrator.MIGRATED))
        }

    @Test
    fun runsOnlyOnce() =
        runBlocking {
            legacyDatabase(row("11111111-1111-1111-1111-111111111111", "Uno", -1.0, null, "2026-05-01T12:00:00Z", synced = true))
            migrator.migrateIfNeeded()
            legacyDatabase(row("44444444-4444-4444-4444-444444444444", "Otro", -1.0, null, "2026-05-01T12:00:00Z", synced = true))

            migrator.migrateIfNeeded()

            assertEquals(
                1,
                db
                    .transactions()
                    .observeRecent(10)
                    .first()
                    .size,
            )
        }

    @Test
    fun withoutMvpDataItOnlyRecordsThatItRan() =
        runBlocking {
            migrator.migrateIfNeeded()

            assertTrue(
                db
                    .accounts()
                    .observeActive()
                    .first()
                    .isEmpty(),
            )
            assertEquals(0, db.categories().count())
            assertEquals("true", db.syncMeta().get(LegacyDatabaseMigrator.MIGRATED))
        }

    @Test
    fun unparseableDatesFallBackToNow() {
        assertNull(LegacyDatabaseMigrator.parseDate("ayer"))
        val entity = LegacyDatabaseMigrator.toTransaction(LegacyRow(UUID.randomUUID().toString(), "X", -5.0, null, "ayer"), clock.now())!!
        assertEquals(clock.now(), entity.occurredAt)
    }

    private fun row(
        id: String,
        title: String,
        value: Double,
        description: String?,
        date: String,
        synced: Boolean,
    ) = ContentValues().apply {
        put("localId", id)
        put("title", title)
        put("value", value)
        put("type", if (value < 0) 1 else 0)
        put("description", description)
        put("date", date)
        put("synced", if (synced) 1 else 0)
    }

    /** The MVP schema as Room 2.8 created it in v0.1.0 (legacy TransactionEntity). */
    private fun legacyDatabase(vararg rows: ContentValues) {
        val file = context.getDatabasePath(LegacyDatabaseMigrator.LEGACY_NAME).also { it.parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { legacy ->
            legacy.execSQL(
                "CREATE TABLE IF NOT EXISTS transactions (localId TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, " +
                    "value REAL NOT NULL, type INTEGER NOT NULL, description TEXT, date TEXT NOT NULL, synced INTEGER NOT NULL)",
            )
            rows.forEach { legacy.insertOrThrow("transactions", null, it) }
        }
    }
}
