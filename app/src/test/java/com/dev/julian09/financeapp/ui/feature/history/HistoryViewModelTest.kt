package com.dev.julian09.financeapp.ui.feature.history

import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.TransactionType
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HistoryViewModelTest {
    private val f = LedgerFixture()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        f.db.close()
    }

    @Test
    fun `groups the newest first by day and names deactivated accounts too`() =
        runBlocking {
            val cash = f.account("Efectivo")
            val bank = f.account("Nu", active = false)
            val food = f.category("Comida")
            // 15:00 UTC falls on the same day from UTC−12 to UTC+8, whatever zone runs the test
            f.tx("TRANSFER", 500_000, bank, at = Instant.parse("2026-10-16T15:00:00Z"), destination = cash)
            f.tx("EXPENSE", 28_000, cash, at = Instant.parse("2026-10-15T15:00:00Z"), category = food)

            val days = HistoryViewModel(f.transactions, f.accounts, f.categories).days.filterNotNull().first()

            assertEquals(listOf(LocalDate.parse("2026-10-16"), LocalDate.parse("2026-10-15")), days.map { it.date })
            assertEquals(HistoryRow("Transferencia", "Nu → Efectivo", Money.ofPesos(500_000), TransactionType.TRANSFER), days[0].rows[0])
            assertEquals(HistoryRow("Comida", "Efectivo", Money.ofPesos(28_000), TransactionType.EXPENSE), days[1].rows[0])
            assertEquals(Money.ofPesos(-28_000), days[1].net)
        }

    @Test
    fun `day labels say Hoy, Ayer, Antier and then the date`() {
        val today = LocalDate.parse("2026-10-16")
        assertEquals(listOf("Hoy", "Ayer", "Antier"), (0L..2L).map { dayLabel(today.minusDays(it), today) })
        assertEquals("Martes 13 de oct", dayLabel(LocalDate.parse("2026-10-13"), today))
        assertEquals("Miércoles 31 de dic de 2025", dayLabel(LocalDate.parse("2025-12-31"), today))
    }
}
