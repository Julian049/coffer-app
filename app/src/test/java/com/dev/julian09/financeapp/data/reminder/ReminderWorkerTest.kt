package com.dev.julian09.financeapp.data.reminder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.dev.julian09.financeapp.data.local.FixedClock
import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.data.local.entity.SettingsEntity
import com.dev.julian09.financeapp.domain.model.CardDue
import com.dev.julian09.financeapp.domain.model.Frequency
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.model.Recurring
import com.dev.julian09.financeapp.domain.model.ReminderKind
import com.dev.julian09.financeapp.domain.model.Settings
import com.dev.julian09.financeapp.domain.usecase.CardDraft
import com.dev.julian09.financeapp.domain.usecase.CreateCard
import com.dev.julian09.financeapp.domain.usecase.ObserveCardDues
import com.dev.julian09.financeapp.domain.usecase.ObserveCards
import com.dev.julian09.financeapp.domain.usecase.SendReminders
import com.dev.julian09.financeapp.ui.FakeReminderNotifier
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** S6-QA03 with a simulated clock: the window notice, the due-day notice, no duplicates, and off means off. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReminderWorkerTest {
    private val f = LedgerFixture()
    private val notifier = FakeReminderNotifier()
    private val scheduler = FakeSyncScheduler()

    @After
    fun close() = f.db.close()

    private fun on(day: String): SendReminders {
        val clock = FixedClock(Instant.parse("${day}T13:00:00Z"))
        val dues = ObserveCardDues(ObserveCards(f.accounts, f.cards, clock), f.settings, clock)
        return SendReminders(f.recurring, f.cards, dues, f.settings, notifier, scheduler, clock)
    }

    private suspend fun recurring(due: String): Recurring {
        val r =
            Recurring(
                UUID.randomUUID(),
                "Arriendo",
                Money.ofPesos(900_000),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.parse("2026-01-23"),
                Frequency.MONTHLY,
                LocalDate.parse(due),
            )
        f.recurring.create(r)
        return r
    }

    private suspend fun settings(settings: Settings) {
        // A synced row, as the first sync leaves it; the repository only updates an existing one
        f.db.settings().upsert(
            SettingsEntity(
                UUID.randomUUID(),
                settings.reminderLeadDays,
                settings.notificationsEnabled,
                settings.homeBalanceView,
                settings.amountDecimals,
                "[]",
                Instant.EPOCH,
                Instant.EPOCH,
            ),
        )
    }

    @Test
    fun oneNoticeWhenTheWindowOpensAndOneOnTheDueDate() =
        runBlocking {
            val r = recurring(due = "2026-10-23")
            // Lead of 5 days: the window opens on the 18th
            assertEquals(0, on("2026-10-17")())
            assertEquals(1, on("2026-10-18")())
            assertEquals(ReminderKind.WINDOW_OPEN, notifier.sent.single().kind)
            assertEquals(0, on("2026-10-18")())
            assertEquals(0, on("2026-10-20")())
            assertEquals(1, on("2026-10-23")())
            assertEquals(ReminderKind.DUE_TODAY, notifier.sent.last().kind)
            assertEquals(0, on("2026-10-23")())
            assertEquals(0, on("2026-10-24")()) // overdue: no third notice (REQ-REC-09)
            assertEquals(
                LocalDate.parse("2026-10-23"),
                f.db
                    .recurring()
                    .get(r.id)!!
                    .lastNotifiedOn,
            )
            assertEquals(2, scheduler.syncRequests)
        }

    @Test
    fun aWindowThatOpenedOnADayWithoutCheckGetsItsNoticeLater() =
        runBlocking {
            recurring(due = "2026-10-21")
            assertEquals(1, on("2026-10-19")())
            assertEquals(ReminderKind.WINDOW_OPEN, notifier.sent.single().kind)
        }

    @Test
    fun aCardStatementGetsTheSameNoticesAsARecurringExpense() =
        runBlocking {
            // A card created with $300.000 of debt on Oct 7: due on Nov 5 (cut 20, payment 5)
            CreateCard(f.accounts, f.cards, scheduler)(
                CardDraft("Visa", Money.ofPesos(1_000_000), Money.ofPesos(700_000), 20, 5, BigDecimal("0.02"), RateType.MV, false),
            )
            assertEquals(0, on("2026-10-30")())
            assertEquals(1, on("2026-10-31")())
            val notice = notifier.sent.single()
            assertEquals(ReminderKind.WINDOW_OPEN, notice.kind)
            assertTrue(notice.payment is CardDue)
            assertEquals(Money.ofPesos(300_000), notice.payment.amount)
            assertEquals(0, on("2026-11-01")())
            assertEquals(1, on("2026-11-05")())
            assertEquals(ReminderKind.DUE_TODAY, notifier.sent.last().kind)
        }

    @Test
    fun nothingIsSentWhenTurnedOffOrWithoutPermission() =
        runBlocking {
            recurring(due = "2026-10-18")
            notifier.allowed = false
            assertEquals(0, on("2026-10-18")())
            notifier.allowed = true
            settings(Settings(notificationsEnabled = false))
            assertEquals(0, on("2026-10-18")())
            assertEquals(emptyList<Any>(), notifier.sent)
        }

    @Test
    fun theWorkerRunsTheCheckAndSucceeds() =
        runBlocking {
            recurring(due = "2026-10-18")
            val send = on("2026-10-18")
            val worker =
                TestListenableWorkerBuilder<ReminderWorker>(ApplicationProvider.getApplicationContext())
                    .setWorkerFactory(
                        object : WorkerFactory() {
                            override fun createWorker(
                                appContext: Context,
                                workerClassName: String,
                                workerParameters: WorkerParameters,
                            ): ListenableWorker = ReminderWorker(appContext, workerParameters, send)
                        },
                    ).build()
            assertEquals(ListenableWorker.Result.success(), worker.doWork())
            assertEquals(ReminderKind.DUE_TODAY, notifier.sent.single().kind)
        }
}
