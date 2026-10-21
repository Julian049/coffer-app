package com.dev.julian09.financeapp.ui.feature.transactions

import androidx.lifecycle.SavedStateHandle
import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.CreateTag
import com.dev.julian09.financeapp.domain.usecase.DeferCardPurchase
import com.dev.julian09.financeapp.domain.usecase.DeleteTransaction
import com.dev.julian09.financeapp.domain.usecase.RegisterTransaction
import com.dev.julian09.financeapp.domain.usecase.TransactionValidator
import com.dev.julian09.financeapp.domain.usecase.UpdateTransaction
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.time.Duration
import java.util.UUID

/** S11-QA06: M5 (#89), the name first with the notes used before. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NoteSuggestionsTest {
    private val f = LedgerFixture()
    private val sync = FakeSyncScheduler()
    private val validator = TransactionValidator(f.accounts, f.categories)
    private val register = RegisterTransaction(f.transactions, validator, sync)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        f.db.close()
    }

    private suspend fun spend(
        note: String,
        category: UUID,
        account: UUID,
        daysAgo: Long,
        type: TransactionType = TransactionType.EXPENSE,
    ) = register(
        Transaction(
            UUID.randomUUID(),
            type,
            Money.ofPesos(20_000),
            f.clock.now() - Duration.ofDays(daysAgo),
            account,
            categoryId = category,
            note = note,
        ),
    )

    @Test
    fun typingSuggestsEarlierNotesAndPickingOneFillsTheCategory() =
        runBlocking {
            val cash = f.account("Efectivo", 500_000)
            val food = f.category("Comida")
            val restaurants = f.category("Restaurantes")
            val refunds = f.category("Reembolsos", "INCOME")
            spend("Almuerzo corrientazo", food, cash, daysAgo = 9)
            spend("Almuerzo", food, cash, daysAgo = 5)
            spend("Almuerzo", restaurants, cash, daysAgo = 1)
            spend("Almuerzo reembolsado", refunds, cash, daysAgo = 2, type = TransactionType.INCOME)

            val found = f.transactions.suggestNotes(TransactionType.EXPENSE, "alm")
            assertEquals(listOf("Almuerzo", "Almuerzo corrientazo"), found.map { it.note })
            // The category of the latest use
            assertEquals(restaurants, found.first().categoryId)

            val vm =
                TransactionFormViewModel(
                    SavedStateHandle(mapOf("type" to "EXPENSE")),
                    f.accounts,
                    f.categories,
                    f.tags,
                    f.transactions,
                    register,
                    UpdateTransaction(f.transactions, validator, sync),
                    CreateTag(f.tags, sync),
                    DeferCardPurchase(f.cards, register, validator, sync, f.clock),
                    DeleteTransaction(f.transactions, sync),
                    f.observeCards,
                    f.clock,
                )
            vm.choices.first { it.categories.isNotEmpty() }
            vm.setNote("Almu")
            val shown = vm.state.first { it.suggestions.isNotEmpty() }.suggestions
            vm.pickSuggestion(shown.first())
            assertEquals("Almuerzo", vm.state.value.note)
            assertEquals(restaurants, vm.state.value.categoryId)
            assertEquals(emptyList<Any>(), vm.state.value.suggestions)
        }
}
