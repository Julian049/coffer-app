package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.FixedClock
import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.CardPaymentKind
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.CardDraft
import com.dev.julian09.financeapp.domain.usecase.CardField
import com.dev.julian09.financeapp.domain.usecase.CreateCard
import com.dev.julian09.financeapp.domain.usecase.DeferCardPurchase
import com.dev.julian09.financeapp.domain.usecase.DeleteTransaction
import com.dev.julian09.financeapp.domain.usecase.PayCreditCard
import com.dev.julian09.financeapp.domain.usecase.RegisterTransaction
import com.dev.julian09.financeapp.domain.usecase.TransactionField
import com.dev.julian09.financeapp.domain.usecase.TransactionValidator
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import kotlinx.coroutines.flow.first
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

/** S7-QA02 and S7-QA03: AT-HU08-1 to 3 and AT-HU09-1 and 2 over the real repositories (SEQ-06, SEQ-07). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CreditCardTest {
    // Wednesday 2026-10-07: in the period that closes on the 20th, paid on November 5
    private val f = LedgerFixture(FixedClock(Instant.parse("2026-10-07T15:00:00Z")))
    private val scheduler = FakeSyncScheduler()
    private val validator = TransactionValidator(f.accounts, f.categories)
    private val createCard = CreateCard(f.accounts, f.cards, scheduler)
    private val defer = DeferCardPurchase(f.cards, RegisterTransaction(f.transactions, validator, scheduler), validator, scheduler, f.clock)
    private val pay = PayCreditCard(f.observeCards, f.cards, validator, scheduler, f.clock)

    @After
    fun close() = f.db.close()

    private fun draft(
        debt: Long = 0,
        firstFree: Boolean = false,
    ) = CardDraft("Visa", Money.ofPesos(5_000_000), Money.ofPesos(debt), 20, 5, BigDecimal("0.02"), RateType.MV, firstFree)

    private suspend fun card(
        debt: Long = 0,
        firstFree: Boolean = false,
    ): UUID {
        assertEquals(emptyMap<Any, Any>(), createCard(draft(debt, firstFree)))
        return f.accounts
            .observeBalances()
            .first()
            .single { it.account.type == AccountType.CREDIT_CARD }
            .account.id
    }

    private fun purchase(
        card: UUID,
        food: UUID,
        pesos: Long = 1_200_000,
    ) = Transaction(UUID.randomUUID(), TransactionType.EXPENSE, Money.ofPesos(pesos), f.clock.now(), card, categoryId = food)

    private suspend fun overview(card: UUID) = f.observeCards().first().single { it.card.accountId == card }

    @Test
    fun aNewCardStartsWithItsCurrentDebt() =
        runBlocking {
            val id = card(debt = 800_000)
            val o = overview(id)
            assertEquals(Money.ofPesos(-800_000), o.account.balance)
            assertEquals(Money.ofPesos(4_200_000), o.available)
            // REQ-TC-13: due as a purchase in one installment of this period
            assertEquals(Money.ofPesos(800_000), o.statement.minimum)
            assertEquals(LocalDate.parse("2026-11-05"), o.statement.period.dueDate)
            assertEquals(
                SyncState.PENDING,
                f.db
                    .cards()
                    .getCard(id)!!
                    .syncState,
            )
            assertEquals(
                mapOf(CardField.DEBT to LedgerError.OVER_LIMIT),
                createCard(draft().copy(name = "Otra", debt = Money.ofPesos(6_000_000))),
            )
        }

    @Test
    fun aPurchaseIn12InstallmentsRaisesTheDebtButOnlyItsFirstInstallmentIsDue() =
        runBlocking {
            val id = card()
            val food = f.category("Comida")
            val t = purchase(id, food)
            assertEquals(emptyMap<Any, Any>(), defer(t, 12, BigDecimal("0.02"), RateType.MV))
            val o = overview(id)
            assertEquals(Money.ofPesos(-1_200_000), o.account.balance)
            assertEquals(Money.ofPesos(124_000), o.statement.minimum)
            val plan = o.installments.sortedBy { it.number }
            assertEquals(12, plan.size)
            assertEquals(LocalDate.parse("2026-11-05"), plan.first().dueDate)
            assertEquals(Money.ofPesos(24_000), plan.first().interest)
            assertEquals(Money.ofPesos(2_000), plan.last().interest)
            assertEquals(1, o.deferred.size)
            assertEquals(
                12,
                f.transactions
                    .observe(t.id)
                    .first()!!
                    .installments,
            )
        }

    @Test
    fun aZeroRateAgreementAndTheFirstInterestFreeInstallmentAreHonored() =
        runBlocking {
            val id = card(firstFree = true)
            val food = f.category("Comida")
            defer(purchase(id, food, 480_000), 6, BigDecimal.ZERO, RateType.MV)
            assertTrue(overview(id).installments.all { it.interest == Money.ZERO })
            defer(purchase(id, food), 12, BigDecimal("0.02"), RateType.MV)
            assertEquals(Money.ofPesos(80_000 + 100_000), overview(id).statement.minimum)
        }

    @Test
    fun oneInstallmentIsAnOrdinaryExpenseWithoutPlan() =
        runBlocking {
            val id = card()
            val food = f.category("Comida")
            defer(purchase(id, food, 38_000), 1, BigDecimal("0.02"), RateType.MV)
            val o = overview(id)
            assertTrue(o.installments.isEmpty())
            assertEquals(Money.ofPesos(38_000), o.statement.onePayment)
        }

    @Test
    fun aBadRateOrCountIsRefused() =
        runBlocking {
            val id = card()
            val food = f.category("Comida")
            assertEquals(LedgerError.RATE_INVALID, defer(purchase(id, food), 12, BigDecimal(-1), RateType.MV)[TransactionField.RATE])
            assertEquals(
                LedgerError.INSTALLMENTS_INVALID,
                defer(purchase(id, food), 73, BigDecimal("0.02"), RateType.MV)[TransactionField.INSTALLMENTS],
            )
            assertTrue(overview(id).installments.isEmpty())
        }

    @Test
    fun deletingAPurchaseTakesItsPlanWithIt() =
        runBlocking {
            val id = card()
            val food = f.category("Comida")
            val t = purchase(id, food)
            defer(t, 12, BigDecimal("0.02"), RateType.MV)
            DeleteTransaction(f.transactions, scheduler)(t.id)
            val o = overview(id)
            assertTrue(o.installments.isEmpty())
            assertTrue(o.deferred.isEmpty())
            assertEquals(Money.ZERO, o.account.balance)
        }

    @Test
    fun theMinimumPaysTheInstallmentAndOnlyItsPrincipalLowersTheDebt() =
        runBlocking {
            val id = card()
            val debit = f.account("Bancolombia", 2_000_000)
            val food = f.category("Comida")
            defer(purchase(id, food), 12, BigDecimal("0.02"), RateType.MV)
            assertEquals(1, f.cardDues().first().size)

            assertEquals(emptyMap<Any, Any>(), pay(id, CardPaymentKind.MINIMUM, debit, Money.ofPesos(124_000), f.clock.now()))
            val o = overview(id)
            assertEquals(Money.ofPesos(-1_100_000), o.account.balance)
            assertEquals(InstallmentStatus.PAID, o.installments.single { it.number == 1 }.status)
            assertEquals(Money.ZERO, o.statement.minimum)
            assertTrue(f.cardDues().first().isEmpty())
            val payment =
                f.cards
                    .observeCardTransactions()
                    .first()
                    .single { it.type == TransactionType.TRANSFER }
            assertEquals(Money.ofPesos(100_000), payment.principal)
            assertEquals(Money.ofPesos(24_000), payment.interest)
            assertEquals("CARD_MIN", payment.paymentKind)
        }

    @Test
    fun aTransferOutOfTheCardIsDueLikeAPurchase() =
        runBlocking {
            val id = card()
            val debit = f.account("Bancolombia", 0)
            val food = f.category("Comida")
            RegisterTransaction(f.transactions, validator, scheduler)(purchase(id, food, 300_000))
            val advance =
                Transaction(
                    UUID.randomUUID(),
                    TransactionType.TRANSFER,
                    Money.ofPesos(4_000),
                    f.clock.now(),
                    id,
                    destinationAccountId = debit,
                )
            assertEquals(emptyMap<Any, Any>(), RegisterTransaction(f.transactions, validator, scheduler)(advance))

            val o = overview(id)
            assertEquals(Money.ofPesos(-304_000), o.account.balance)
            assertEquals(Money.ofPesos(304_000), o.statement.minimum)
        }

    @Test
    fun theTotalSettlesEverythingWithoutFutureInterest() =
        runBlocking {
            val id = card()
            val debit = f.account("Bancolombia", 2_000_000)
            val food = f.category("Comida")
            defer(purchase(id, food), 12, BigDecimal("0.02"), RateType.MV)
            val total = overview(id).statement.total
            assertEquals(Money.ofPesos(1_224_000), total)

            pay(id, CardPaymentKind.TOTAL, debit, total, f.clock.now())
            val o = overview(id)
            assertEquals(Money.ZERO, o.account.balance)
            assertEquals(1, o.installments.count { it.status == InstallmentStatus.PAID })
            assertEquals(11, o.installments.count { it.status == InstallmentStatus.CANCELLED })
            assertTrue(o.deferred.isEmpty())
        }

    @Test
    fun aSmallerAmountCoversInterestFirstAndSettlesNothing() =
        runBlocking {
            val id = card()
            val debit = f.account("Bancolombia", 2_000_000)
            val food = f.category("Comida")
            defer(purchase(id, food), 12, BigDecimal("0.02"), RateType.MV)
            pay(id, CardPaymentKind.MINIMUM, debit, Money.ofPesos(50_000), f.clock.now())
            val o = overview(id)
            assertEquals(Money.ofPesos(-1_174_000), o.account.balance)
            assertEquals(Money.ofPesos(74_000), o.statement.minimum)
            assertTrue(o.installments.all { it.status == InstallmentStatus.SCHEDULED })
        }
}
