package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.FixedClock
import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.local.entity.TransactionDraftEntity
import com.dev.julian09.financeapp.domain.model.Frequency
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Recurring
import com.dev.julian09.financeapp.domain.usecase.ApplyDraft
import com.dev.julian09.financeapp.domain.usecase.AutoApplyDrafts
import com.dev.julian09.financeapp.domain.usecase.SuggestDraft
import com.dev.julian09.financeapp.domain.usecase.TransactionValidator
import com.dev.julian09.financeapp.domain.usecase.UndoDraft
import com.dev.julian09.financeapp.ui.FakeDraftPrefs
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import com.dev.julian09.financeapp.ui.draftRecurring
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** roadmap-v2.4 2.4-07: a draft that is the payment of a pending recurring expense pays that one, once, and is learnt. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DraftRecurringTest {
    private val f = LedgerFixture(FixedClock(Instant.parse("2026-11-04T12:00:00Z")))
    private val prefs = FakeDraftPrefs()
    private val sync = FakeSyncScheduler()
    private val dr = draftRecurring(f, prefs, sync)
    private val suggest = SuggestDraft(f.accounts, f.transactions, f.mailRules, prefs)
    private val validator = TransactionValidator(f.accounts, f.categories)
    private val apply = ApplyDraft(f.drafts, suggest, validator, sync, dr)
    private val auto = AutoApplyDrafts(f.drafts, suggest, validator, prefs, sync, dr)
    private val undo = UndoDraft(f.transactions, f.drafts, prefs, f.clock, sync, f.recurring)

    @After
    fun close() = f.db.close()

    private suspend fun card() =
        f.account("Tarjeta", 0, type = "SAVINGS").also {
            f.db.accounts().upsert(
                listOf(
                    f.db
                        .accounts()
                        .get(it)!!
                        .copy(lastDigits = "2309"),
                ),
            )
        }

    private suspend fun claude(
        account: UUID,
        due: String,
    ): Recurring {
        val r =
            Recurring(
                UUID.randomUUID(),
                "Claude Pro",
                Money.ofPesos(100_000),
                account,
                f.category("Suscripciones"),
                LocalDate.parse("2026-01-05"),
                Frequency.MONTHLY,
                LocalDate.parse(due),
            )
        f.recurring.create(r)
        return r
    }

    private suspend fun draft(
        pesos: Long,
        at: String = "2026-11-04T10:00:00Z",
        merchant: String = "ANTHROPIC",
    ): UUID {
        val id = UUID.randomUUID()
        val t = Instant.parse(at)
        f.db.drafts().upsert(
            listOf(
                TransactionDraftEntity(
                    id,
                    "BANCOLOMBIA",
                    "EXPENSE",
                    pesos * 10_000,
                    t,
                    merchant,
                    "2309",
                    t,
                    t,
                    syncState = SyncState.SYNCED,
                ),
            ),
        )
        return id
    }

    @Test
    fun aDraftWithTheSameAmountAccountAndDateIsTheRecurringsPayment() =
        runBlocking {
            val acc = card()
            val r = claude(acc, "2026-11-05")
            val d = f.drafts.get(draft(100_000))!!
            val match = dr.match(d, acc)
            assertEquals(r.id, match?.recurring?.id)
            assertFalse(match!!.linked)
            assertEquals(1L, match.daysLeft)
            assertNull(dr.match(f.drafts.get(draft(100_001))!!, acc))
            assertNull(dr.match(f.drafts.get(draft(100_000, at = "2026-11-20T10:00:00Z"))!!, acc))
        }

    @Test
    fun theTapAsksFirstAndYesPaysTheRecurringWithItsTitleAndLearnsIt() =
        runBlocking {
            val acc = card()
            val r = claude(acc, "2026-11-05")
            val id = draft(100_000)
            // Not known yet: the tick opens Revisar instead of registering a second expense
            assertFalse(apply(id))
            assertEquals(
                0,
                f.transactions
                    .observeRecent(10)
                    .first()
                    .size,
            )

            assertNotNull(dr.pay(id, r.id, acc))

            val paid =
                f.transactions
                    .observeRecent(10)
                    .first()
                    .single()
            assertEquals("Claude Pro", paid.note)
            assertEquals(r.id, paid.recurringId)
            assertEquals(id, paid.draftId)
            assertEquals(
                LocalDate.parse("2026-12-05"),
                f.recurring
                    .observe(r.id)
                    .first()!!
                    .nextDueDate,
            )
            assertNull(f.drafts.get(id))
        }

    @Test
    fun onceLearntTheNextMonthsEmailPaysItByItself() =
        runBlocking {
            val acc = card()
            val r = claude(acc, "2026-11-05")
            assertNotNull(dr.pay(draft(100_000), r.id, acc))
            // A month later (the phone's clock moves with it)
            f.clock.instant = Instant.parse("2026-12-04T12:00:00Z")
            val next = draft(100_000, at = "2026-12-04T10:00:00Z")

            assertEquals(1, auto())

            assertNull(f.drafts.get(next))
            assertEquals(
                2,
                f.transactions
                    .observeRecent(10)
                    .first()
                    .size,
            )
            assertEquals(
                LocalDate.parse("2027-01-05"),
                f.recurring
                    .observe(r.id)
                    .first()!!
                    .nextDueDate,
            )
            assertEquals(1, prefs.observeRegistered().first().size)
        }

    @Test
    fun aNotLearntMatchIsNeverSavedByItself() =
        runBlocking {
            val acc = card()
            claude(acc, "2026-11-05")
            val id = draft(100_000)
            assertEquals(0, auto())
            assertNotNull(f.drafts.get(id))
        }

    @Test
    fun undoingThePaymentBringsTheDateAndTheDraftBack() =
        runBlocking {
            val acc = card()
            val r = claude(acc, "2026-11-05")
            val id = draft(100_000)
            dr.pay(id, r.id, acc)
            val paid =
                f.transactions
                    .observeRecent(10)
                    .first()
                    .single()

            assertTrue(undo(paid))

            assertEquals(
                0,
                f.transactions
                    .observeRecent(10)
                    .first()
                    .size,
            )
            assertEquals(
                LocalDate.parse("2026-11-05"),
                f.recurring
                    .observe(r.id)
                    .first()!!
                    .nextDueDate,
            )
            assertNotNull(f.drafts.get(id))
        }
}
