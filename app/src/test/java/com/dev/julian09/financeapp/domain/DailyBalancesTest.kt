package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.FixedClock
import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.usecase.ObserveDailyBalances
import com.dev.julian09.financeapp.domain.usecase.defaultCalendarAccounts
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.YearMonth

/** S20 (REQ-CAL-01 to 03): the closing balance per day, and today's equals Inicio's for the same accounts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DailyBalancesTest {
    private val f = LedgerFixture(FixedClock(Instant.parse("2026-10-05T18:00:00Z")))

    @After
    fun close() = f.db.close()

    @Test
    fun eachDayClosesWithWhatMovedUpToIt() =
        runBlocking {
            val cash = f.account("Efectivo", 500_000, type = "CASH")
            val visa = f.account("Visa", 0, type = "CREDIT_CARD")
            val food = f.category("Comida")
            f.tx("EXPENSE", 20_000, cash, Instant.parse("2026-10-02T15:00:00Z"), food)
            f.tx("EXPENSE", 80_000, visa, Instant.parse("2026-10-03T15:00:00Z"), food)
            f.tx("EXPENSE", 5_000, cash, Instant.parse("2026-10-04T23:30:00Z"), food)

            val chosen = defaultCalendarAccounts(f.accounts.observeActive().first())
            assertEquals(setOf(cash), chosen)
            val days = ObserveDailyBalances(f.accounts, f.clock)(YearMonth.of(2026, 10), chosen)

            assertEquals(5, days.size)
            assertEquals(Money.ofPesos(500_000), days[0].closing)
            assertEquals(Money.ofPesos(480_000), days[1].closing)
            assertEquals(Money.ofPesos(-20_000), days[1].change)
            // The card is not in the default choice
            assertEquals(Money.ofPesos(480_000), days[2].closing)
            assertEquals(Money.ofPesos(475_000), days[4].closing)
            val home =
                f.accounts
                    .observeBalances()
                    .first()
                    .single { it.account.id == cash }
                    .balance
            assertEquals(home, days.last().closing)
        }
}
