package com.dev.julian09.financeapp.ui.feature.history

import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.SearchTransactions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HistoryViewModelTest {
    // The fixture's clock is 2026-10-16 in UTC
    private val f = LedgerFixture()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        f.db.close()
    }

    private fun viewModel() =
        HistoryViewModel(SearchTransactions(f.transactions, f.categories), f.transactions, f.accounts, f.categories, f.tags, f.clock)

    // Room emits on its own thread: wait for the state built from what was just asked
    private suspend fun HistoryViewModel.current(ready: (HistoryUiState) -> Boolean = { true }) =
        withTimeout(5_000) { state.filterNotNull().first(ready) }

    @Test
    fun `the selected month by day, with names of deactivated accounts and the month summary`() =
        runBlocking {
            val cash = f.account("Efectivo")
            val bank = f.account("Nu", active = false)
            val food = f.category("Comida")
            f.tx("TRANSFER", 500_000, bank, at = Instant.parse("2026-10-16T15:00:00Z"), destination = cash)
            f.tx("EXPENSE", 28_000, cash, at = Instant.parse("2026-10-15T15:00:00Z"), category = food)
            f.tx("INCOME", 1_000, cash, at = Instant.parse("2026-08-02T15:00:00Z"))

            val vm = viewModel()
            val s = vm.current()
            assertEquals(listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 9), YearMonth.of(2026, 8)), s.months)
            assertEquals(listOf(LocalDate.parse("2026-10-16"), LocalDate.parse("2026-10-15")), s.days.map { it.date })
            assertEquals("Nu → Efectivo", s.days[0].rows[0].subtitle)
            assertEquals("Comida", s.days[1].rows[0].title)
            assertEquals(Money.ofPesos(28_000), s.totals.expense)
            assertEquals(Money.ZERO, s.totals.income) // the transfer is not income (AT-HU03-3)

            vm.selectMonth(YearMonth.of(2026, 8))
            assertEquals(Money.ofPesos(1_000), vm.current { it.month == YearMonth.of(2026, 8) }.totals.income)
        }

    @Test
    fun `type chips, search and a custom range`() =
        runBlocking {
            val cash = f.account("Efectivo")
            f.tx("EXPENSE", 1, cash, at = Instant.parse("2026-10-10T15:00:00Z"))
            f.tx("INCOME", 2, cash, at = Instant.parse("2026-09-10T15:00:00Z"))
            val vm = viewModel()

            vm.setType(TransactionType.INCOME)
            assertEquals(0, vm.current { it.filters.types.isNotEmpty() }.days.size)
            val range = HistoryFilters(from = LocalDate.parse("2026-09-01"), to = LocalDate.parse("2026-10-31"))
            vm.applyFilters(range)
            assertEquals(2, vm.current { it.filters == range }.days.size) // the range replaced the month and the type filter
            vm.setQuery("efect")
            assertEquals(2, vm.current { it.query == "efect" }.totals.count)
            vm.selectMonth(YearMonth.of(2026, 10))
            // Without a range, a search looks in the whole history, not just the month (E5)
            assertEquals(2, vm.current { !it.filters.customRange && it.query == "efect" }.totals.count)
            vm.setQuery("")
            assertEquals(listOf(LocalDate.parse("2026-10-10")), vm.current { !it.filters.customRange && it.query.isEmpty() }.days.map { it.date })
        }

    @Test
    fun `day labels say Hoy, Ayer, Antier and then the date`() {
        val today = LocalDate.parse("2026-10-16")
        assertEquals(listOf("Hoy", "Ayer", "Antier"), (0L..2L).map { dayLabel(today.minusDays(it), today) })
        assertEquals("Martes 13 de oct", dayLabel(LocalDate.parse("2026-10-13"), today))
        assertEquals("Miércoles 31 de dic de 2025", dayLabel(LocalDate.parse("2025-12-31"), today))
    }
}
