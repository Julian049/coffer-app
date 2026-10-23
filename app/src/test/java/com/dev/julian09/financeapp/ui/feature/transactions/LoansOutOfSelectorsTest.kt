package com.dev.julian09.financeapp.ui.feature.transactions

import androidx.lifecycle.SavedStateHandle
import com.dev.julian09.financeapp.data.local.LedgerFixture
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/** S15-QA02: the loans are only seen in Gestión de cuentas (S15-DOM02). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LoansOutOfSelectorsTest {
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

    private fun form(vararg args: Pair<String, String>) =
        TransactionFormViewModel(
            SavedStateHandle(mapOf("type" to "EXPENSE") + args),
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

    @Test
    fun aNewMovementNeverOffersALoanNorDefaultsToIt() =
        runBlocking {
            val loan = f.account("Préstamo", type = "LOAN")
            val cash = f.account("Efectivo", 100_000)
            // The only movement so far is on the loan: it was the last account used
            f.tx("EXPENSE", 5, loan)

            val vm = form()
            val offered =
                vm.choices
                    .first { it.accounts.isNotEmpty() }
                    .accounts
                    .map { it.account.id }
            assertEquals(listOf(cash), offered)
            assertEquals(cash, vm.state.first { it.accountId != null }.accountId)
        }

    @Test
    fun editingAMovementOfALoanStillShowsIt() =
        runBlocking {
            val loan = f.account("Préstamo", type = "LOAN")
            f.account("Efectivo", 100_000)
            val id: UUID = f.tx("EXPENSE", 5, loan)

            val vm = form("id" to id.toString())
            val offered =
                vm.choices
                    .first { c -> c.accounts.any { it.account.id == loan } }
                    .accounts
                    .map { it.account.id }
            assertTrue(loan in offered)
            assertFalse(
                form()
                    .choices.value.accounts
                    .any { it.account.id == loan },
            )
        }
}
