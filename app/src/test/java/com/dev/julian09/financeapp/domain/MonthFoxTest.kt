package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.finance.FoxState
import com.dev.julian09.financeapp.domain.usecase.ObserveHomeDashboard
import com.dev.julian09.financeapp.domain.usecase.ObserveMonthFox
import com.dev.julian09.financeapp.domain.usecase.ObservePendingPayments
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

/** S11-QA05: M14, the mascot of a month in Historial (PO, 2026-10-21: a past month shows how it closed). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MonthFoxTest {
    private val f = LedgerFixture()
    private val home =
        ObserveHomeDashboard(
            f.accounts,
            f.transactions,
            ObservePendingPayments(f.recurring, f.settings, f.clock),
            f.cardDues,
            f.loanDues,
            f.clock,
        )
    private val monthFox = ObserveMonthFox(f.accounts, f.transactions, home, f.clock)

    @After
    fun close() = f.db.close()

    private fun day(date: String) = Instant.parse("${date}T15:00:00Z")

    @Test
    fun aPastMonthShowsTheStateItClosedWith() =
        runBlocking {
            val bank = f.account("Bancolombia", 2_000_000)
            val salary = f.category("Salario", "INCOME")
            val food = f.category("Comida")
            // August: more income than expenses; September: spent more than what came in
            f.tx("INCOME", 3_000_000, bank, day("2026-08-01"), salary)
            f.tx("EXPENSE", 1_000_000, bank, day("2026-08-20"), food)
            f.tx("INCOME", 1_000_000, bank, day("2026-09-01"), salary)
            f.tx("EXPENSE", 1_500_000, bank, day("2026-09-15"), food)
            // October looks fine today, and September keeps its own state
            f.tx("INCOME", 3_000_000, bank, day("2026-10-01"), salary)

            assertEquals(FoxState.WORRIED, monthFox(YearMonth.of(2026, 9)).first().state)
            assertEquals(FoxState.GOOD, monthFox(YearMonth.of(2026, 8)).first().state)
            assertEquals(FoxState.RESTING, monthFox(YearMonth.of(2026, 7)).first().state)
            assertEquals(home().first().fox, monthFox(YearMonth.of(2026, 10)).first())
        }
}
