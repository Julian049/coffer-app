package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.FixedClock
import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.domain.model.Frequency
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Recurring
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.model.Urgency
import com.dev.julian09.financeapp.domain.usecase.EndRecurrence
import com.dev.julian09.financeapp.domain.usecase.ObservePendingPayments
import com.dev.julian09.financeapp.domain.usecase.PayPending
import com.dev.julian09.financeapp.domain.usecase.TransactionField
import com.dev.julian09.financeapp.domain.usecase.TransactionValidator
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
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** S6-QA02, AT-HU07-1 to 3 (SEQ-10 §10.1, §10.2). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PayPendingTest {
    // Sunday 2026-10-18
    private val f = LedgerFixture(FixedClock(Instant.parse("2026-10-18T12:00:00Z")))
    private val scheduler = FakeSyncScheduler()
    private val pay = PayPending(f.recurring, TransactionValidator(f.accounts, f.categories), scheduler)
    private val end = EndRecurrence(f.recurring, scheduler)
    private val pending = ObservePendingPayments(f.recurring, f.settings, f.clock)

    @After
    fun close() = f.db.close()

    private companion object {
        val END: Instant = Instant.ofEpochMilli(Long.MAX_VALUE)
    }

    private suspend fun rent(
        account: UUID,
        category: UUID,
        due: String = "2026-10-20",
    ): Recurring {
        val r =
            Recurring(
                UUID.randomUUID(),
                "Arriendo",
                Money.ofPesos(900_000),
                account,
                category,
                LocalDate.parse("2026-01-20"),
                Frequency.MONTHLY,
                LocalDate.parse(due),
                lastNotifiedOn = LocalDate.parse("2026-10-15"),
            )
        f.recurring.create(r)
        return r
    }

    private suspend fun balance() =
        f.accounts
            .observeBalances()
            .first()
            .single()
            .balance

    private suspend fun stored(id: UUID) = f.recurring.observe(id).first()!!

    @Test
    fun aRecurringDueInTheWindowIsPendingAndChangesNoBalance() =
        runBlocking {
            val debit = f.account("Débito", 2_000_000)
            val r = rent(debit, f.category("Hogar"))
            rent(debit, f.category("Servicios"), due = "2026-10-30")

            val all = pending().first()
            assertEquals(listOf(true, false), all.map { it.inWindow })
            assertEquals(Urgency.SOON, all.first().urgency)
            assertEquals(2L, all.first().daysLeft)
            assertEquals(r.id, all.first().recurring.id)
            assertEquals(Money.ofPesos(2_000_000), balance())
        }

    @Test
    fun payingCreatesTheExpenseAdvancesTheDateAndKeepsTheAmount() =
        runBlocking {
            val debit = f.account("Débito", 2_000_000)
            val home = f.category("Hogar")
            val r = rent(debit, home)
            val at = Instant.parse("2026-10-18T13:10:00Z")

            assertEquals(emptyMap<TransactionField, LedgerError>(), pay(r.id, Money.ofPesos(950_000), at, debit))

            val payment =
                f.db
                    .transactions()
                    .observeRecurringPayments(Instant.EPOCH, END)
                    .first()
                    .single()
            assertEquals(TransactionType.EXPENSE.name, payment.type)
            assertEquals(r.id, payment.recurringId)
            assertEquals(home, payment.categoryId)
            assertEquals("Arriendo", payment.note)
            assertEquals(at, payment.occurredAt)
            assertEquals(SyncState.PENDING, payment.syncState)
            assertEquals(
                "CREATE",
                f.db
                    .changeLog()
                    .forTransaction(payment.id)
                    .single()
                    .action,
            )
            val after = stored(r.id)
            assertEquals(LocalDate.parse("2026-11-20"), after.nextDueDate)
            assertEquals(Money.ofPesos(950_000), after.suggestedAmount)
            assertNull(after.lastNotifiedOn)
            assertEquals(Money.ofPesos(1_050_000), balance())
            assertEquals(1, scheduler.syncRequests)
        }

    @Test
    fun aPaymentFromAnInactiveAccountIsRefused() =
        runBlocking {
            val debit = f.account("Débito", 2_000_000)
            val closed = f.account("Cerrada", active = false)
            val r = rent(debit, f.category("Hogar"))
            val errors = pay(r.id, Money.ZERO, f.clock.now(), closed)
            assertEquals(
                mapOf(TransactionField.AMOUNT to LedgerError.AMOUNT_REQUIRED, TransactionField.ACCOUNT to LedgerError.ACCOUNT_REQUIRED),
                errors,
            )
            assertEquals(LocalDate.parse("2026-10-20"), stored(r.id).nextDueDate)
        }

    @Test
    fun aFinalizedRecurringLeavesNoPendingPayments() =
        runBlocking {
            val r = rent(f.account("Débito"), f.category("Hogar"))
            end(r.id)
            assertFalse(stored(r.id).active)
            assertTrue(pending().first().isEmpty())
        }
}
