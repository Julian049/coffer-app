package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.data.local.entity.SyncState
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/** SEQ-04, AT-HU01-1 to 3: what the form saves and what it refuses. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RegisterTransactionTest {
    private val f = LedgerFixture()
    private val scheduler = FakeSyncScheduler()
    private val register = RegisterTransaction(f.transactions, TransactionValidator(f.accounts, f.categories), scheduler)

    @After
    fun close() = f.db.close()

    private fun expense(
        account: UUID,
        category: UUID?,
        pesos: Long = 28_000,
    ) = Transaction(
        UUID.randomUUID(),
        TransactionType.EXPENSE,
        Money.ofPesos(pesos),
        f.clock.now(),
        account,
        categoryId = category,
        note = "  ",
    )

    @Test
    fun anExpenseIsSavedPendingWithItsHistoryAndAsksToSync() =
        runBlocking {
            val debit = f.account("Débito", 100_000)
            val food = f.category("Comida")
            val tx = expense(debit, food)

            assertEquals(emptyMap<TransactionField, LedgerError>(), register(tx))
            val saved = f.db.transactions().get(tx.id)!!
            assertEquals(SyncState.PENDING, saved.syncState)
            assertNull(saved.note) // a blank note is no note
            val history =
                f.db
                    .changeLog()
                    .forTransaction(tx.id)
                    .single()
            assertEquals("CREATE", history.action)
            assertNull(history.beforeValue)
            assertTrue(history.afterValue!!.contains("\"amount\":\"28000.0000\""))
            assertEquals(1, scheduler.syncRequests)
            assertEquals(
                Money.ofPesos(72_000),
                f.accounts
                    .observeBalances()
                    .first()
                    .single()
                    .balance,
            )
            // The last account used is offered next time (SEQ-04)
            assertEquals(debit, f.transactions.lastUsedAccountId())
        }

    @Test
    fun anEmptyAmountOrAWrongCategoryIsNotSaved() =
        runBlocking {
            val debit = f.account("Débito")
            val salary = f.category("Salario", kind = "INCOME")
            val errors = register(expense(debit, salary, pesos = 0))
            assertEquals(LedgerError.AMOUNT_REQUIRED, errors[TransactionField.AMOUNT])
            assertEquals(LedgerError.KIND_MISMATCH, errors[TransactionField.CATEGORY])
            assertEquals(LedgerError.CATEGORY_REQUIRED, register(expense(debit, null))[TransactionField.CATEGORY])
            val inactive = f.account("Vieja", active = false)
            assertEquals(LedgerError.ACCOUNT_REQUIRED, register(expense(inactive, f.category("Comida")))[TransactionField.ACCOUNT])
            assertEquals(0, f.transactions.observeCount().first())
            assertEquals(0, scheduler.syncRequests)
        }

    @Test
    fun savingTakesLessThan300Milliseconds() =
        runBlocking {
            val debit = f.account("Débito")
            val food = f.category("Comida")
            register(expense(debit, food)) // warm-up: the first call opens the database
            val start = System.nanoTime()
            register(expense(debit, food))
            val millis = (System.nanoTime() - start) / 1_000_000
            // RNF-03 on the JVM with an in-memory database; the phone adds SQLCipher, checked by hand
            assertTrue("took $millis ms", millis < 300)
        }
}
