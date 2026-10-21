package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.model.ChangeAction
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.DeleteTransaction
import com.dev.julian09.financeapp.domain.usecase.ObserveChangeHistory
import com.dev.julian09.financeapp.domain.usecase.PurgeChangeHistory
import com.dev.julian09.financeapp.domain.usecase.RegisterTransaction
import com.dev.julian09.financeapp.domain.usecase.RestoreTransaction
import com.dev.julian09.financeapp.domain.usecase.RevertEdit
import com.dev.julian09.financeapp.domain.usecase.TransactionValidator
import com.dev.julian09.financeapp.domain.usecase.UpdateTransaction
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
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** S11-QA01: AT-HU12-1 and 2 (REQ-HIS-01 to 04). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChangeLogRepositoryTest {
    private val f = LedgerFixture()
    private val sync = FakeSyncScheduler()
    private val validator = TransactionValidator(f.accounts, f.categories)
    private val history = ObserveChangeHistory(f.changeLog, f.clock)

    @After
    fun close() = f.db.close()

    private fun later(minutes: Long = 1) {
        f.clock.instant = f.clock.instant.plus(Duration.ofMinutes(minutes))
    }

    private suspend fun lunch(): Transaction {
        val cash = f.account("Efectivo", 100_000)
        val tx =
            Transaction(
                UUID.randomUUID(),
                TransactionType.EXPENSE,
                Money.ofPesos(28_000),
                f.clock.now(),
                cash,
                categoryId = f.category("Comida"),
                note = "Almuerzo",
            )
        RegisterTransaction(f.transactions, validator, sync)(tx)
        return tx
    }

    private suspend fun cashBalance(tx: Transaction) =
        f.accounts
            .observeBalances()
            .first()
            .single { it.account.id == tx.accountId }
            .balance

    @Test
    fun aDeletedMovementComesBackWithItsDataFromTheHistory() =
        runBlocking {
            val tx = lunch()
            later()
            DeleteTransaction(f.transactions, sync)(tx.id)
            assertEquals(Money.ofPesos(100_000), cashBalance(tx))

            val deleted = history().first().first()
            assertEquals(ChangeAction.DELETE, deleted.entry.action)
            assertTrue(deleted.canRestore)
            // Only the latest change of a movement can be undone
            assertFalse(history().first().last().canRestore)

            later()
            assertTrue(RestoreTransaction(f.transactions, validator, sync)(deleted.entry).isEmpty())
            val back = f.transactions.observe(tx.id).first()!!
            assertEquals(Money.ofPesos(28_000), back.amount)
            assertEquals("Almuerzo", back.note)
            assertEquals(Money.ofPesos(72_000), cashBalance(tx))
            assertEquals(
                ChangeAction.RESTORE,
                history()
                    .first()
                    .first()
                    .entry.action,
            )
            assertFalse(history().first().any { it.canRestore })
        }

    @Test
    fun anEditGoesBackToTheValuesBeforeIt() =
        runBlocking {
            val tx = lunch()
            later()
            UpdateTransaction(f.transactions, validator, sync)(tx.copy(amount = Money.ofPesos(14_000)))

            val edit = history().first().first()
            assertEquals(ChangeAction.UPDATE, edit.entry.action)
            assertTrue(edit.canRevert)
            later()
            assertTrue(RevertEdit(UpdateTransaction(f.transactions, validator, sync))(edit.entry).isEmpty())
            assertEquals(
                Money.ofPesos(28_000),
                f.transactions
                    .observe(tx.id)
                    .first()!!
                    .amount,
            )
        }

    @Test
    fun aRestoreIsRefusedWhenTheAccountIsGone() =
        runBlocking {
            val tx = lunch()
            later()
            DeleteTransaction(f.transactions, sync)(tx.id)
            f.db.accounts().upsert(
                listOf(
                    f.db
                        .accounts()
                        .get(tx.accountId)!!
                        .copy(active = false),
                ),
            )

            assertFalse(RestoreTransaction(f.transactions, validator, sync)(history().first().first().entry).isEmpty())
            assertNull(f.transactions.observe(tx.id).first())
        }

    @Test
    fun entriesOlderThanNinetyDaysLeaveTheHistory() =
        runBlocking {
            lunch()
            f.clock.instant = f.clock.instant.plus(Duration.ofDays(91))
            assertTrue(history().first().isEmpty())
            PurgeChangeHistory(f.changeLog, f.clock)()
            assertTrue(
                f.db
                    .changeLog()
                    .observeSince(Instant.EPOCH)
                    .first()
                    .isEmpty(),
            )
        }
}
