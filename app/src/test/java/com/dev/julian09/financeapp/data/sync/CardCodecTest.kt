package com.dev.julian09.financeapp.data.sync

import com.dev.julian09.financeapp.data.local.entity.CreditCardDetailsEntity
import com.dev.julian09.financeapp.data.local.entity.DeferredPurchaseEntity
import com.dev.julian09.financeapp.data.local.entity.InstallmentEntity
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

/** The contract's CreditCardDetailsData, DeferredPurchaseData and InstallmentData both ways (S7-DAT01). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CardCodecTest {
    private val db = inMemoryDatabase()
    private val tables = SyncTables(db)
    private val at = Instant.parse("2026-10-19T12:00:00Z")

    @After
    fun close() = db.close()

    @Test
    fun cardTermsTravelWithTheRateAsTheServerWritesIt() {
        val row = CreditCardDetailsEntity(UUID.randomUUID(), 5_000_000_0000, 20, 5, "0.020000", "MV", true, at, at)
        val change = tables.cards.encode(row)
        assertEquals("creditCardDetails", change.entity)
        assertEquals("\"5000000.0000\"", change.data["creditLimit"].toString())
        assertEquals("\"0.020000\"", change.data["rate"].toString())
        assertEquals(row.copy(syncState = SyncState.SYNCED), tables.cards.decode(change))
    }

    @Test
    fun aDeferredPurchaseAndItsInstallmentsComeBackTheSame() {
        val purchase =
            DeferredPurchaseEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1_200_000_0000,
                12,
                "0.020000",
                "MV",
                false,
                LocalDate.parse("2026-11-05"),
                at,
                at,
            )
        assertEquals(purchase.copy(syncState = SyncState.SYNCED), tables.deferred.decode(tables.deferred.encode(purchase)))
        val installment =
            InstallmentEntity(
                id = UUID.randomUUID(),
                deferredPurchaseId = purchase.id,
                number = 1,
                dueDate = LocalDate.parse("2026-11-05"),
                principal = 100_000_0000,
                interest = 24_000_0000,
                status = "PAID",
                paidAt = at,
                createdAt = at,
                updatedAt = at,
            )
        val change = tables.installments.encode(installment)
        assertEquals("null", change.data["loanAccountId"].toString())
        assertEquals("\"0.0000\"", change.data["insurance"].toString())
        assertEquals(installment.copy(syncState = SyncState.SYNCED), tables.installments.decode(change))
    }
}
