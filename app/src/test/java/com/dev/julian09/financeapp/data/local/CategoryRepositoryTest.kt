package com.dev.julian09.financeapp.data.local

import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.domain.model.Money
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CategoryRepositoryTest {
    private val f = LedgerFixture()

    @After
    fun close() = f.db.close()

    @Test
    fun totalsCountOnlyThePeriodAndLiveMovements() =
        runBlocking {
            val account = f.account("Débito")
            val food = f.category("Comida")
            val october = Instant.parse("2026-10-10T12:00:00Z")
            f.tx("EXPENSE", 10_000, account, at = october, category = food)
            f.tx("EXPENSE", 5_000, account, at = october, category = food)
            f.tx("EXPENSE", 99_000, account, at = Instant.parse("2026-09-30T12:00:00Z"), category = food)
            val deleted = f.tx("EXPENSE", 1_000, account, at = october, category = food)
            f.db.transactions().deleteForAccount(account, f.clock.now())
            f.tx("EXPENSE", 2_000, account, at = october, category = food)

            val totals = f.categories.observeTotals(Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z")).first()
            assertEquals(1, totals.size)
            assertEquals(Money.ofPesos(2_000), totals.single().total)
            assertEquals(1, totals.single().count)
            assertEquals(
                SyncState.PENDING,
                f.db
                    .transactions()
                    .syncInfo(deleted)!!
                    .syncState,
            )
        }

    @Test
    fun writesArePendingWithTheClockTime() =
        runBlocking {
            val food = f.category("Comida")
            f.categories.rename(food, "Comidas")
            val row =
                f.db
                    .categories()
                    .observeAll()
                    .first()
                    .single()
            assertEquals("Comidas", row.name)
            assertEquals(f.clock.now(), row.updatedAt)
            assertEquals(SyncState.PENDING, row.syncState)
        }
}
