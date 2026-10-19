package com.dev.julian09.financeapp.ui.feature.home

import com.dev.julian09.financeapp.data.local.FixedClock
import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.finance.FoxState
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.usecase.ObserveHomeDashboard
import com.dev.julian09.financeapp.domain.usecase.ObservePendingPayments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomeViewModelTest {
    // Friday 2026-10-16 at noon UTC
    private val f = LedgerFixture(FixedClock(Instant.parse("2026-10-16T12:00:00Z")))

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        f.db.close()
    }

    private suspend fun state() =
        HomeViewModel(
            ObserveHomeDashboard(f.accounts, f.transactions, ObservePendingPayments(f.recurring, f.settings, f.clock), f.clock),
            f.categories,
        ).state.filterNotNull().first()

    private fun day(date: String) = Instant.parse("${date}T15:00:00Z")

    @Test
    fun onlyActiveIncludedAccountsAddUpAndTheLastFiveDaysAreGroupedByDay() =
        runBlocking {
            val debit = f.account("Débito", 1_000_000)
            f.account("Fuera del total", 500_000, include = false)
            f.account("Desactivada", 700_000, active = false)
            val food = f.category("Comida")
            f.tx("EXPENSE", 20_000, debit, at = day("2026-10-16"), category = food)
            f.tx("EXPENSE", 5_000, debit, at = day("2026-10-14"), category = food)
            f.tx("INCOME", 100_000, debit, at = day("2026-10-12"))
            f.tx("EXPENSE", 1_000, debit, at = day("2026-10-11"))

            val s = state()
            assertEquals(Money.ofPesos(1_074_000), s.dashboard.available)
            assertEquals(
                listOf("Débito", "Fuera del total"),
                s.dashboard.accounts
                    .filter { it.account.active }
                    .map { it.account.name },
            )
            assertEquals(
                listOf(LocalDate.parse("2026-10-16"), LocalDate.parse("2026-10-14"), LocalDate.parse("2026-10-12")),
                s.days.map { it.date },
            )
            assertEquals(
                "Comida",
                s.days
                    .first()
                    .rows
                    .single()
                    .title,
            )
        }

    @Test
    fun theIndicatorComparesWithLastMonthsCloseOfAccountsThatExisted() =
        runBlocking {
            val debit = f.account("Débito", 1_000_000)
            f.tx("INCOME", 100_000, debit, at = day("2026-10-05"))
            // Created this month: it has no previous close (PO, 2026-10-17)
            f.account("Nueva", 400_000, createdAt = day("2026-10-10"))

            val s = state().dashboard
            assertEquals(BigDecimal("50.0"), s.monthlyChange)
            assertEquals(FoxState.GOOD, s.fox.state)
            assertEquals("septiembre", monthName(s.previousMonth))
        }

    @Test
    fun aBrandNewUserSeesNoIndicatorAndTheRestingFox() =
        runBlocking {
            f.account("Efectivo", 150_000, createdAt = day("2026-10-15"))
            val s = state().dashboard
            assertNull(s.monthlyChange)
            assertEquals(FoxState.RESTING, s.fox.state)
        }
}
