package com.dev.julian09.financeapp.data.sync

import com.dev.julian09.financeapp.data.local.entity.RecurringExpenseEntity
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.local.inMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** The contract's RecurringExpenseData both ways: what is pushed comes back the same from a pull. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecurringCodecTest {
    private val db = inMemoryDatabase()
    private val table = SyncTables(db).recurring

    @After
    fun close() = db.close()

    @Test
    fun aRecurringExpenseTravelsWithItsDatesAndAmounts() {
        val at = Instant.parse("2026-10-18T12:00:00Z")
        val row =
            RecurringExpenseEntity(
                id = UUID.randomUUID(),
                name = "Gimnasio",
                estimatedAmount = 85_000_0000,
                lastPaidAmount = 90_000_0000,
                accountId = UUID.randomUUID(),
                categoryId = UUID.randomUUID(),
                startDate = LocalDate.parse("2026-01-31"),
                frequency = "CUSTOM",
                intervalCount = 2,
                intervalUnit = "WEEK",
                nextDueDate = LocalDate.parse("2026-10-24"),
                lastNotifiedOn = LocalDate.parse("2026-10-19"),
                createdAt = at,
                updatedAt = at,
            )
        val change = table.encode(row)
        assertEquals("recurringExpense", change.entity)
        assertEquals("\"85000.0000\"", change.data["estimatedAmount"].toString())
        assertEquals("\"2026-10-24\"", change.data["nextDueDate"].toString())
        assertEquals(row.copy(syncState = SyncState.SYNCED), table.decode(change))

        val monthly =
            row.copy(
                frequency = "MONTHLY",
                intervalCount = null,
                intervalUnit = null,
                lastPaidAmount = null,
                lastNotifiedOn = null,
            )
        assertEquals(monthly.copy(syncState = SyncState.SYNCED), table.decode(table.encode(monthly)))
    }
}
