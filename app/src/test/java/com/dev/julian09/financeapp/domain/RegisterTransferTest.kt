package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.RegisterTransaction
import com.dev.julian09.financeapp.domain.usecase.TransactionField
import com.dev.julian09.financeapp.domain.usecase.TransactionValidator
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

/** SEQ-05, AT-HU03-2 and 3. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RegisterTransferTest {
    private val f = LedgerFixture()
    private val register = RegisterTransaction(f.transactions, TransactionValidator(f.accounts, f.categories), FakeSyncScheduler())

    @After
    fun close() = f.db.close()

    private fun transfer(
        from: UUID,
        to: UUID?,
        category: UUID? = null,
    ) = Transaction(UUID.randomUUID(), TransactionType.TRANSFER, Money.ofPesos(100_000), f.clock.now(), from, to, category)

    @Test
    fun aTransferMovesMoneyWithoutBeingExpenseOrIncome() =
        runBlocking {
            val debit = f.account("Débito", 500_000)
            val cash = f.account("Efectivo", 0)
            val food = f.category("Comida")
            val tx = transfer(debit, cash, category = food)

            assertEquals(emptyMap<TransactionField, LedgerError>(), register(tx))
            assertNull(
                f.db
                    .transactions()
                    .get(tx.id)!!
                    .categoryId,
            ) // a transfer never carries a category
            val balances =
                f.accounts
                    .observeBalances()
                    .first()
                    .associate { it.account.name to it.balance }
            assertEquals(Money.ofPesos(400_000), balances["Débito"])
            assertEquals(Money.ofPesos(100_000), balances["Efectivo"])
            // AT-HU03-3: the month totals do not see it
            val month = f.transactions.observeTotals(Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z")).first()
            assertEquals(Money.ZERO, month.income)
            assertEquals(Money.ZERO, month.expense)
        }

    @Test
    fun theSameAccountOrNoDestinationIsRefused() =
        runBlocking {
            val debit = f.account("Débito")
            assertEquals(LedgerError.SAME_ACCOUNT, register(transfer(debit, debit))[TransactionField.DESTINATION])
            assertEquals(LedgerError.ACCOUNT_REQUIRED, register(transfer(debit, null))[TransactionField.DESTINATION])
            assertEquals(0, f.transactions.observeCount().first())
        }
}
