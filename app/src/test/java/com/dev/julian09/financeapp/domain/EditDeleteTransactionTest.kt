package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.DeleteTransaction
import com.dev.julian09.financeapp.domain.usecase.RegisterTransaction
import com.dev.julian09.financeapp.domain.usecase.TransactionField
import com.dev.julian09.financeapp.domain.usecase.TransactionValidator
import com.dev.julian09.financeapp.domain.usecase.UpdateTransaction
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/** SEQ-14 §14.1 and S5-QA03: edit and delete, also offline (nothing here touches the network). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EditDeleteTransactionTest {
    private val f = LedgerFixture()
    private val scheduler = FakeSyncScheduler()
    private val validator = TransactionValidator(f.accounts, f.categories)

    @After
    fun close() = f.db.close()

    private suspend fun saved(): Transaction {
        val cash = f.account("Efectivo", 100_000)
        val tx =
            Transaction(
                UUID.randomUUID(),
                TransactionType.EXPENSE,
                Money.ofPesos(24_500),
                f.clock.now(),
                cash,
                categoryId = f.category("Comida"),
            )
        RegisterTransaction(f.transactions, validator, scheduler)(tx)
        return tx
    }

    @Test
    fun anEditKeepsBeforeAndAfterInTheHistory() =
        runBlocking {
            val tx = saved()
            val update = UpdateTransaction(f.transactions, validator, scheduler)

            assertEquals(LedgerError.AMOUNT_REQUIRED, update(tx.copy(amount = Money.ZERO))[TransactionField.AMOUNT])
            assertTrue(update(tx.copy(amount = Money.ofPesos(30_000), note = "Almuerzo")).isEmpty())

            val row = f.db.transactions().get(tx.id)!!
            assertEquals(30_000L * 10_000, row.amount)
            assertEquals(SyncState.PENDING, row.syncState)
            val history = f.db.changeLog().forTransaction(tx.id)
            assertEquals(listOf("CREATE", "UPDATE"), history.map { it.action })
            assertTrue(history[1].beforeValue!!.contains("24500.0000"))
            assertTrue(history[1].afterValue!!.contains("30000.0000"))
            assertEquals(
                Money.ofPesos(70_000),
                f.accounts
                    .observeBalances()
                    .first()
                    .single()
                    .balance,
            )
            assertEquals(
                "Almuerzo",
                f.transactions
                    .observe(tx.id)
                    .first()!!
                    .note,
            )
        }

    @Test
    fun aDeleteIsLogicalLoggedAndLeavesTheBalance() =
        runBlocking {
            val tx = saved()
            DeleteTransaction(f.transactions, scheduler)(tx.id)

            val row = f.db.transactions().get(tx.id)!!
            assertNotNull(row.deletedAt) // logical: the cloud and other devices learn about it (REQ-SYN-03)
            assertEquals(SyncState.PENDING, row.syncState)
            assertNull(f.transactions.observe(tx.id).first())
            val delete =
                f.db
                    .changeLog()
                    .forTransaction(tx.id)
                    .last()
            assertEquals("DELETE", delete.action)
            assertNull(delete.afterValue)
            assertEquals(
                Money.ofPesos(100_000),
                f.accounts
                    .observeBalances()
                    .first()
                    .single()
                    .balance,
            )
            assertEquals(2, scheduler.syncRequests)

            DeleteTransaction(f.transactions, scheduler)(tx.id) // deleting twice logs once
            assertEquals(
                2,
                f.db
                    .changeLog()
                    .forTransaction(tx.id)
                    .size,
            )
        }
}
