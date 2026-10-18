package com.dev.julian09.financeapp.ui.feature.history

import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val cash = Account(UUID.randomUUID(), "Efectivo", AccountType.CASH, Money.ofPesos(0))
    private val bank = Account(UUID.randomUUID(), "Nu", AccountType.SAVINGS, Money.ofPesos(0))
    private val food = Category(UUID.randomUUID(), "Comida", CategoryKind.EXPENSE)
    private val day1 = LocalDate.of(2026, 10, 15)
    private val day2 = LocalDate.of(2026, 10, 16)

    private fun at(
        date: LocalDate,
        hour: Int,
    ) = date.atTime(LocalTime.of(hour, 0)).atZone(ZoneId.systemDefault()).toInstant()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `groups the newest first by local day with names`() =
        runTest(UnconfinedTestDispatcher()) {
            val txs =
                listOf(
                    Transaction(UUID.randomUUID(), TransactionType.TRANSFER, Money.ofPesos(500_000), at(day2, 9), bank.id, cash.id),
                    Transaction(
                        UUID.randomUUID(),
                        TransactionType.EXPENSE,
                        Money.ofPesos(28_000),
                        at(day1, 20),
                        cash.id,
                        categoryId = food.id,
                    ),
                    Transaction(UUID.randomUUID(), TransactionType.INCOME, Money.ofPesos(10), at(day1, 8), cash.id, note = "Venta"),
                )
            val vm =
                HistoryViewModel(
                    object : TransactionRepository {
                        override fun observeRecent(limit: Int) = flowOf(txs)
                    },
                    object : AccountRepository {
                        override fun observeActive() = flowOf(listOf(cash, bank))
                    },
                    object : CategoryRepository {
                        override fun observeAll() = flowOf(listOf(food))
                    },
                )
            backgroundScope.launch { vm.days.collect {} }

            val days = vm.days.value!!
            assertEquals(listOf(day2, day1), days.map { it.date })
            assertEquals(HistoryRow("Transferencia", "Nu → Efectivo", Money.ofPesos(500_000), TransactionType.TRANSFER), days[0].rows[0])
            assertEquals(listOf("Comida", "Venta"), days[1].rows.map { it.title })
            assertEquals("Efectivo", days[1].rows[0].subtitle)
        }
}
