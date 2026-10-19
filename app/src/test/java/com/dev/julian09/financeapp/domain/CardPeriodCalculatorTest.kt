package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.finance.CardPeriodCalculator
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.CardPeriod
import com.dev.julian09.financeapp.domain.model.CreditCard
import com.dev.julian09.financeapp.domain.model.Installment
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/** S7-QA01, REQ-TC-08, 10 to 13, detailed-design §6.9.3: cuts from the 28th to the 31st, February and leap years. */
class CardPeriodCalculatorTest {
    private fun d(text: String) = LocalDate.parse(text)

    private fun period(
        start: String,
        end: String,
        due: String,
    ) = CardPeriod(d(start), d(end), d(due))

    @Test
    fun aPurchaseAfterTheCutGoesToTheNextPeriod() {
        assertEquals(period("2026-09-21", "2026-10-20", "2026-11-05"), CardPeriodCalculator.periodOf(d("2026-10-20"), 20, 5))
        assertEquals(period("2026-10-21", "2026-11-20", "2026-12-05"), CardPeriodCalculator.periodOf(d("2026-10-21"), 20, 5))
    }

    @Test
    fun cutsFrom28To31FollowShortMonths() {
        assertEquals(period("2026-02-01", "2026-02-28", "2026-03-15"), CardPeriodCalculator.periodOf(d("2026-02-15"), 31, 15))
        assertEquals(period("2026-03-01", "2026-03-31", "2026-04-15"), CardPeriodCalculator.periodOf(d("2026-03-01"), 31, 15))
        assertEquals(period("2026-01-31", "2026-02-28", "2026-03-10"), CardPeriodCalculator.periodOf(d("2026-02-28"), 30, 10))
        assertEquals(period("2028-01-30", "2028-02-29", "2028-03-10"), CardPeriodCalculator.periodOf(d("2028-02-29"), 29, 10))
        assertEquals(period("2026-01-29", "2026-02-28", "2026-03-05"), CardPeriodCalculator.periodOf(d("2026-02-10"), 28, 5))
    }

    @Test
    fun thePaymentIsTheFirstPaymentDayAfterTheCut() {
        assertEquals(d("2026-11-05"), CardPeriodCalculator.dueDateAfter(d("2026-10-20"), 5))
        assertEquals(d("2026-10-20"), CardPeriodCalculator.dueDateAfter(d("2026-10-05"), 20))
        assertEquals(d("2026-11-15"), CardPeriodCalculator.dueDateAfter(d("2026-10-15"), 15))
        assertEquals(d("2027-02-28"), CardPeriodCalculator.dueDateAfter(d("2027-01-31"), 30))
        assertEquals(d("2026-11-05"), CardPeriodCalculator.firstDueDate(d("2026-10-07"), 20, 5))
    }

    @Test
    fun purchasesBetweenTheCutAndThePaymentCarryNoExtraInterest() {
        assertEquals(Money.ZERO, CardPeriodCalculator.interestForGracePeriod())
    }

    private val cardId = UUID.randomUUID()
    private val card = CreditCard(cardId, Money.ofPesos(5_000_000), 20, 5, BigDecimal("0.02"), RateType.MV)

    private fun account(
        initial: Long = 0,
        created: String = "2026-09-01",
    ) = Account(cardId, "Visa", AccountType.CREDIT_CARD, Money.ofPesos(initial), createdAt = Instant.parse("${created}T12:00:00Z"))

    private fun purchase(
        pesos: Long,
        on: String,
        installments: Int = 1,
    ) = Transaction(
        UUID.randomUUID(),
        TransactionType.EXPENSE,
        Money.ofPesos(pesos),
        Instant.parse("${on}T12:00:00Z"),
        cardId,
        categoryId = UUID.randomUUID(),
        installments = installments,
    )

    private fun payment(
        pesos: Long,
        on: String,
        interest: Long = 0,
    ) = Transaction(
        UUID.randomUUID(),
        TransactionType.TRANSFER,
        Money.ofPesos(pesos),
        Instant.parse("${on}T12:00:00Z"),
        UUID.randomUUID(),
        destinationAccountId = cardId,
        principal = Money.ofPesos(pesos - interest),
        interest = Money.ofPesos(interest),
        fees = Money.ZERO,
    )

    private val installment =
        Installment(UUID.randomUUID(), UUID.randomUUID(), 1, d("2026-11-05"), Money.ofPesos(100_000), Money.ofPesos(24_000))

    private fun statement(
        transactions: List<Transaction>,
        debt: Long,
        today: String = "2026-10-25",
        account: Account = account(),
    ) = CardPeriodCalculator.statement(
        card,
        account,
        Money.ofPesos(debt),
        transactions,
        listOf(installment),
        d(today),
        ZoneOffset.UTC,
    )

    @Test
    fun theClosedPeriodIsDueWithItsOneInstallmentPurchasesAndInstallments() {
        val moves = listOf(purchase(50_000, "2026-10-10"), purchase(1_200_000, "2026-10-07", 12), purchase(30_000, "2026-10-22"))
        val s = statement(moves, debt = 1_280_000)
        assertEquals(d("2026-11-05"), s.period.dueDate)
        assertEquals(Money.ofPesos(50_000), s.onePayment)
        assertEquals(Money.ofPesos(174_000), s.minimum)
        assertEquals(Money.ofPesos(1_304_000), s.total)
        // REQ-TC-11: interest first, then principal
        val split = CardPeriodCalculator.breakdown(s, Money.ofPesos(100_000))
        assertEquals(Money.ofPesos(24_000), split.interest)
        assertEquals(Money.ofPesos(76_000), split.principal)
    }

    @Test
    fun onceThePeriodIsPaidTheOpenOneIsNext() {
        val moves =
            listOf(
                purchase(50_000, "2026-10-10"),
                purchase(1_200_000, "2026-10-07", 12),
                purchase(30_000, "2026-10-22"),
                payment(174_000, "2026-10-26", interest = 24_000),
            )
        val s = statement(moves, debt = 1_130_000, today = "2026-10-27")
        assertEquals(d("2026-12-05"), s.period.dueDate)
        assertEquals(Money.ofPesos(30_000), s.minimum)
        assertEquals(Money.ZERO, s.pendingInterest)
    }

    @Test
    fun theInitialDebtIsAPurchaseInOneInstallmentOfTheCreationPeriod() {
        val s = statement(emptyList(), debt = 800_000, today = "2026-10-25", account = account(800_000, created = "2026-10-12"))
        assertEquals(d("2026-11-05"), s.period.dueDate)
        assertEquals(Money.ofPesos(800_000), s.onePayment)
        assertEquals(Money.ofPesos(924_000), s.minimum)
    }
}
