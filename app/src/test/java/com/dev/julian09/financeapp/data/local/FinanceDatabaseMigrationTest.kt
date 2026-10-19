package com.dev.julian09.financeapp.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** data-model §5.2: every schema change is a tested migration, never a destructive fallback. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FinanceDatabaseMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    // Plain SQLite here: SQLCipher is native-only (EncryptionInstrumentedTest covers it on a device)
    @get:Rule
    val helper =
        MigrationTestHelper(
            instrumentation,
            File(instrumentation.targetContext.filesDir, "migration-test.db"),
            AndroidSQLiteDriver(),
            FinanceDatabase::class,
        )

    @Test
    fun version1To2KeepsTheDataAndAddsChangeLog() {
        helper.createDatabase(1).apply {
            execSQL(
                "INSERT INTO accounts (id, name, type, initialBalance, includeInTotal, active, sortOrder, createdAt, updatedAt, " +
                    "syncState) VALUES ('a1', 'Efectivo', 'CASH', 0, 1, 1, 0, 0, 0, 'SYNCED')",
            )
            close()
        }
        helper.runMigrationsAndValidate(2).apply {
            prepare("SELECT name FROM accounts").use {
                it.step()
                assertEquals("Efectivo", it.getText(0))
            }
            prepare("SELECT COUNT(*) FROM change_log").use {
                it.step()
                assertEquals(0L, it.getLong(0))
            }
            close()
        }
    }

    @Test
    fun version2To3KeepsTheDataAndAddsRecurringExpenses() {
        helper.createDatabase(2).apply {
            execSQL(
                "INSERT INTO accounts (id, name, type, initialBalance, includeInTotal, active, sortOrder, createdAt, updatedAt, " +
                    "syncState) VALUES ('a1', 'Efectivo', 'CASH', 0, 1, 1, 0, 0, 0, 'SYNCED')",
            )
            close()
        }
        helper.runMigrationsAndValidate(3).apply {
            prepare("SELECT name FROM accounts").use {
                it.step()
                assertEquals("Efectivo", it.getText(0))
            }
            prepare("SELECT COUNT(*) FROM recurring_expenses").use {
                it.step()
                assertEquals(0L, it.getLong(0))
            }
            close()
        }
    }
}
