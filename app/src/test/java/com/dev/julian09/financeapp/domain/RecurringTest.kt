package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.FixedClock
import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.domain.model.Frequency
import com.dev.julian09.financeapp.domain.model.IntervalUnit
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Recurring
import com.dev.julian09.financeapp.domain.usecase.CreateRecurring
import com.dev.julian09.financeapp.domain.usecase.ObserveRecurringOverview
import com.dev.julian09.financeapp.domain.usecase.RecurringField
import com.dev.julian09.financeapp.domain.usecase.RecurringValidator
import com.dev.julian09.financeapp.domain.usecase.UpdateRecurring
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
import java.time.LocalDate
import java.util.UUID

/** S6-H01: what a recurring expense saves, when it starts and what the month's summary adds up (PO, 2026-10-18). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecurringTest {
    // Sunday 2026-10-18
    private val f = LedgerFixture(FixedClock(Instant.parse("2026-10-18T12:00:00Z")))
    private val scheduler = FakeSyncScheduler()
    private val validator = RecurringValidator(f.accounts, f.categories)
    private val create = CreateRecurring(f.recurring, validator, scheduler, f.clock)
    private val update = UpdateRecurring(f.recurring, validator, scheduler, f.clock)

    @After
    fun close() = f.db.close()

    private fun recurring(
        account: UUID,
        category: UUID,
        start: String = "2026-10-05",
        frequency: Frequency = Frequency.MONTHLY,
        pesos: Long = 900_000,
    ) = Recurring(
        UUID.randomUUID(),
        " Arriendo ",
        Money.ofPesos(pesos),
        account,
        category,
        LocalDate.parse(start),
        frequency,
        LocalDate.parse(start),
        intervalCount = 3,
        intervalUnit = IntervalUnit.WEEK,
    )

    @Test
    fun aStartInThePastBeginsAtTheNextDateAndSyncs() =
        runBlocking {
            val r = recurring(f.account("Débito"), f.category("Hogar"), start = "2024-01-05")

            assertEquals(emptyMap<RecurringField, LedgerError>(), create(r))
            val saved = f.recurring.observe(r.id).first()!!
            assertEquals("Arriendo", saved.name)
            assertEquals(LocalDate.parse("2026-11-05"), saved.nextDueDate)
            // Only CUSTOM keeps an interval (server ck_rec_custom)
            assertNull(saved.intervalCount)
            assertNull(saved.intervalUnit)
            assertEquals(
                SyncState.PENDING,
                f.db
                    .recurring()
                    .get(r.id)!!
                    .syncState,
            )
            assertEquals(1, scheduler.syncRequests)
        }

    @Test
    fun onlyExpenseCategoriesActiveAccountsAndAValidIntervalAreAccepted() =
        runBlocking {
            val debit = f.account("Débito")
            val closed = f.account("Cerrada", active = false)
            val salary = f.category("Salario", kind = "INCOME")
            val home = f.category("Hogar")
            val errors = create(recurring(closed, salary, pesos = 0).copy(name = " "))
            assertEquals(
                mapOf(
                    RecurringField.NAME to LedgerError.NAME_REQUIRED,
                    RecurringField.AMOUNT to LedgerError.AMOUNT_REQUIRED,
                    RecurringField.ACCOUNT to LedgerError.ACCOUNT_REQUIRED,
                    RecurringField.CATEGORY to LedgerError.CATEGORY_REQUIRED,
                ),
                errors,
            )
            val custom = recurring(debit, home, frequency = Frequency.CUSTOM).copy(intervalCount = 0)
            assertEquals(mapOf(RecurringField.INTERVAL to LedgerError.INTERVAL_INVALID), create(custom))
            assertEquals(emptyMap<RecurringField, LedgerError>(), create(custom.copy(intervalCount = 2)))
            assertEquals(
                LocalDate.parse("2026-10-19"),
                f.recurring
                    .observe(custom.id)
                    .first()!!
                    .nextDueDate,
            )
        }

    @Test
    fun anEditKeepsTheNextDateUnlessTheScheduleChanges() =
        runBlocking {
            val r = recurring(f.account("Débito"), f.category("Hogar"))
            create(r)
            // A payment sets lastPaidAmount; a new estimate replaces it
            f.recurring.update(
                f.recurring
                    .observe(r.id)
                    .first()!!
                    .copy(lastPaidAmount = Money.ofPesos(950_000)),
            )

            update(r.copy(name = "Arriendo apto"))
            val renamed = f.recurring.observe(r.id).first()!!
            assertEquals(LocalDate.parse("2026-11-05"), renamed.nextDueDate)
            assertEquals(Money.ofPesos(950_000), renamed.lastPaidAmount)

            update(r.copy(startDate = LocalDate.parse("2026-10-25"), estimatedAmount = Money.ofPesos(1_000_000)))
            val moved = f.recurring.observe(r.id).first()!!
            assertEquals(LocalDate.parse("2026-10-25"), moved.nextDueDate)
            assertNull(moved.lastPaidAmount)
            assertEquals(Money.ofPesos(1_000_000), moved.suggestedAmount)
        }

    @Test
    fun theMonthSummaryIsWhatWasPaidPlusWhatIsLeft() =
        runBlocking {
            val debit = f.account("Débito")
            val home = f.category("Hogar")
            val rent = recurring(debit, home, start = "2026-10-25")
            val gym = recurring(debit, home, start = "2026-10-20", frequency = Frequency.WEEKLY, pesos = 20_000).copy(name = "Gimnasio")
            create(rent)
            create(gym)
            // A payment of this month from a finalized recurring expense still counts as covered
            val old = recurring(debit, home, start = "2026-10-01", pesos = 85_000)
            create(old)
            f.recurring.update(
                f.recurring
                    .observe(old.id)
                    .first()!!
                    .copy(active = false),
            )
            val paid = f.tx("EXPENSE", 85_000, debit, at = Instant.parse("2026-10-01T15:00:00Z"), category = home)
            f.db.transactions().upsert(
                listOf(
                    f.db
                        .transactions()
                        .get(paid)!!
                        .copy(recurringId = old.id),
                ),
            )

            val overview = ObserveRecurringOverview(f.recurring, f.transactions, f.settings, f.clock)().first()
            // Gym on the 20th and 27th, rent on the 25th
            assertEquals(3, overview.summary.pendingCount)
            assertEquals(Money.ofPesos(940_000), overview.summary.remaining)
            assertEquals(1, overview.summary.paidCount)
            assertEquals(Money.ofPesos(85_000), overview.summary.covered)
            assertEquals(Money.ofPesos(1_025_000), overview.summary.committed)
            assertEquals(listOf("Gimnasio", "Arriendo"), overview.upcoming.map { it.recurring.name })
            assertEquals("Arriendo", overview.paid.single().recurringName)
            assertEquals(0, overview.summary.overdueCount)
        }
}
