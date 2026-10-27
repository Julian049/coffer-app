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

    @Test
    fun version3To4KeepsTheDataAndAddsTheCardTables() {
        helper.createDatabase(3).apply {
            execSQL(
                "INSERT INTO accounts (id, name, type, initialBalance, includeInTotal, active, sortOrder, createdAt, updatedAt, " +
                    "syncState) VALUES ('a1', 'Efectivo', 'CASH', 0, 1, 1, 0, 0, 0, 'SYNCED')",
            )
            close()
        }
        helper.runMigrationsAndValidate(4).apply {
            prepare("SELECT name FROM accounts").use {
                it.step()
                assertEquals("Efectivo", it.getText(0))
            }
            listOf("credit_card_details", "deferred_purchases", "installments").forEach { table ->
                prepare("SELECT COUNT(*) FROM $table").use {
                    it.step()
                    assertEquals(0L, it.getLong(0))
                }
            }
            close()
        }
    }

    @Test
    fun version4To5KeepsTheCategoriesIncludedInReports() {
        helper.createDatabase(4).apply {
            execSQL(
                "INSERT INTO categories (id, name, kind, isBase, sortOrder, createdAt, updatedAt, syncState) " +
                    "VALUES ('c1', 'Comida', 'EXPENSE', 1, 0, 0, 0, 'SYNCED')",
            )
            close()
        }
        helper.runMigrationsAndValidate(5).apply {
            prepare("SELECT name, excludeFromReports FROM categories").use {
                it.step()
                assertEquals("Comida", it.getText(0))
                assertEquals(0L, it.getLong(1))
            }
            close()
        }
    }

    @Test
    fun version5To6AddsTheLoanTerms() {
        helper.createDatabase(5).apply {
            execSQL(
                "INSERT INTO accounts (id, name, type, initialBalance, includeInTotal, active, sortOrder, createdAt, updatedAt, " +
                    "syncState) VALUES ('l1', 'Libre inversión', 'LOAN', 0, 1, 1, 0, 0, 0, 'SYNCED')",
            )
            close()
        }
        helper.runMigrationsAndValidate(6).apply {
            execSQL(
                "INSERT INTO loan_details (id, mode, paymentDay, lifeInsuranceMonthly, otherCostsMonthly, createdAt, updatedAt, " +
                    "syncState) " +
                    "VALUES ('l1', 'SIMPLE', 5, 0, 0, 0, 0, 'PENDING')",
            )
            prepare("SELECT amortization FROM loan_details").use {
                it.step()
                assertEquals("FIXED_PRINCIPAL", it.getText(0))
            }
            close()
        }
    }

    @Test
    fun version6To7KeepsTheSettingsByPeriodAndAddsTheConcept() {
        helper.createDatabase(6).apply {
            execSQL(
                "INSERT INTO settings (id, reminderLeadDays, notificationsEnabled, homeBalanceView, amountDecimals, homeCards, " +
                    "createdAt, updatedAt, syncState) VALUES ('u1', 5, 1, 'NET', 0, '[]', 0, 0, 'SYNCED')",
            )
            close()
        }
        helper.runMigrationsAndValidate(7).apply {
            prepare("SELECT netBalanceMode FROM settings").use {
                it.step()
                assertEquals("PERIOD", it.getText(0))
            }
            prepare("SELECT COUNT(concept) FROM transactions").use {
                it.step()
                assertEquals(0L, it.getLong(0))
            }
            close()
        }
    }

    @Test
    fun version8To9TurnsTheSingleTagIntoAListAndAddsTheColor() {
        helper.createDatabase(8).apply {
            execSQL("INSERT INTO tags (id, name, pinned, createdAt, updatedAt, syncState) VALUES ('g1', 'Viaje', 0, 0, 0, 'SYNCED')")
            execSQL(
                "INSERT INTO transactions (id, type, amount, occurredAt, accountId, installments, tagId, createdAt, updatedAt, " +
                    "syncState) " +
                    "VALUES ('t1', 'EXPENSE', 10000, 0, 'a1', 1, 'g1', 0, 0, 'SYNCED'), " +
                    "('t2', 'EXPENSE', 10000, 0, 'a1', 1, NULL, 0, 0, 'SYNCED')",
            )
            close()
        }
        helper.runMigrationsAndValidate(9).apply {
            prepare("SELECT tagIds FROM transactions ORDER BY id").use {
                it.step()
                assertEquals(",g1,", it.getText(0))
                it.step()
                assertEquals("", it.getText(0))
            }
            prepare("SELECT COUNT(color) FROM tags").use {
                it.step()
                assertEquals(0L, it.getLong(0))
            }
            close()
        }
    }
}
