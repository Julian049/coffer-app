package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.FixedClock
import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.local.entity.TransactionEntity
import com.dev.julian09.financeapp.domain.usecase.ObserveHomeDashboard
import com.dev.julian09.financeapp.domain.usecase.ObservePendingPayments
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

/**
 * S8-QA03 on the JVM: Home's first load with 10.000 movements under 2 s (RNF-03). The device startup is measured
 * with the phone in the v1.0.0 acceptance test; saving under 300 ms is in RegisterTransactionTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PerformanceTest {
    private val f = LedgerFixture(FixedClock(Instant.parse("2026-10-19T15:00:00Z")))

    @After
    fun close() = f.db.close()

    @Test
    fun homeLoadsTenThousandMovementsInLessThanTwoSeconds() =
        runBlocking {
            val accounts = (1..5).map { f.account("Cuenta $it", 1_000_000) }
            val food = f.category("Comida")
            val start = Instant.parse("2024-01-01T00:00:00Z")
            f.db.transactions().upsert(
                (0 until 10_000).map { i ->
                    TransactionEntity(
                        id = UUID.randomUUID(),
                        type = "EXPENSE",
                        amount = (1_000L + i) * 10_000,
                        occurredAt = start.plusSeconds(i * 5_000L),
                        accountId = accounts[i % accounts.size],
                        categoryId = food,
                        createdAt = start,
                        updatedAt = start,
                        syncState = SyncState.SYNCED,
                    )
                },
            )
            val home =
                ObserveHomeDashboard(
                    f.accounts,
                    f.transactions,
                    ObservePendingPayments(f.recurring, f.settings, f.clock),
                    f.cardDues,
                    f.clock,
                )
            val begin = System.nanoTime()
            val dashboard = home().first()
            val seconds = (System.nanoTime() - begin) / 1_000_000_000.0
            assertEquals(5, dashboard.accounts.size)
            assertTrue("took $seconds s", seconds < 2)
        }
}
