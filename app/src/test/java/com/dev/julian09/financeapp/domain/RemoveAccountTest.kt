package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RemoveMode
import com.dev.julian09.financeapp.domain.usecase.ReactivateAccount
import com.dev.julian09.financeapp.domain.usecase.RemoveAccount
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Decision table of SEQ-12 (REQ-CTA-03 to 05). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoveAccountTest {
    private val f = LedgerFixture()
    private val scheduler = FakeSyncScheduler()
    private val remove = RemoveAccount(f.accounts, scheduler)

    @After
    fun close() = f.db.close()

    private suspend fun balances() =
        f.accounts
            .observeBalances()
            .first()
            .associateBy { it.account.name }

    @Test
    fun deactivateKeepsMovementsAndCanComeBack() =
        runBlocking {
            val debit = f.account("Débito", 500_000)
            f.tx("EXPENSE", 20_000, debit)

            assertNull(remove(debit, RemoveMode.DEACTIVATE))
            val gone = balances().getValue("Débito")
            assertFalse(gone.account.active)
            assertEquals(f.clock.now(), gone.account.deactivatedAt)
            assertEquals(1, gone.transactions)
            assertEquals(
                SyncState.PENDING,
                f.db
                    .accounts()
                    .get(debit)!!
                    .syncState,
            )
            assertEquals(1, scheduler.syncRequests)

            ReactivateAccount(f.accounts, scheduler)(debit)
            assertTrue(balances().getValue("Débito").account.active)
            assertNull(balances().getValue("Débito").account.deactivatedAt)
        }

    @Test
    fun moveSendsEverythingToTheTargetAndDropsTransfersBetweenBoth() =
        runBlocking {
            val debit = f.account("Débito", 500_000)
            val cash = f.account("Efectivo", 100_000)
            val other = f.account("Nu", 0)
            f.tx("INCOME", 300_000, debit)
            f.tx("TRANSFER", 50_000, debit, destination = cash)
            f.tx("TRANSFER", 10_000, other, destination = debit)

            assertNull(remove(debit, RemoveMode.MOVE, targetId = cash))
            val after = balances()
            assertFalse("Débito" in after)
            // The initial balance of Débito leaves with it: 100.000 + 300.000 of income + 10.000 from Nu
            assertEquals(Money.ofPesos(410_000), after.getValue("Efectivo").balance)
            assertEquals(2, after.getValue("Efectivo").transactions)
            assertEquals(Money.ofPesos(-10_000), after.getValue("Nu").balance)
        }

    @Test
    fun moveNeedsAnotherActiveAccount() =
        runBlocking {
            val debit = f.account("Débito")
            val inactive = f.account("Vieja", active = false)
            assertEquals(LedgerError.TARGET_REQUIRED, remove(debit, RemoveMode.MOVE))
            assertEquals(LedgerError.TARGET_INVALID, remove(debit, RemoveMode.MOVE, targetId = debit))
            assertEquals(LedgerError.TARGET_INVALID, remove(debit, RemoveMode.MOVE, targetId = inactive))
            assertEquals(0, scheduler.syncRequests)
        }

    @Test
    fun deleteAllNeedsTheNameAndTakesTheMovementsWithIt() =
        runBlocking {
            val debit = f.account("Bancolombia Débito", 500_000)
            val cash = f.account("Efectivo", 0)
            f.tx("EXPENSE", 20_000, debit, at = f.clock.now().minusSeconds(86_400 * 40))
            f.tx("TRANSFER", 30_000, debit, destination = cash)

            val impact = f.accounts.impact(debit)
            assertEquals(2, impact.transactions)
            assertEquals(Money.ofPesos(450_000), impact.balance)

            assertEquals(LedgerError.CONFIRMATION_MISMATCH, remove(debit, RemoveMode.DELETE_ALL, confirmation = "Bancolombia"))
            assertNull(remove(debit, RemoveMode.DELETE_ALL, confirmation = "  bancolombia débito "))
            val after = balances()
            assertFalse("Bancolombia Débito" in after)
            assertEquals(Money.ZERO, after.getValue("Efectivo").balance)
            assertEquals(0, f.transactions.observeCount().first())
        }
}
