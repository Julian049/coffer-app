package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.finance.FoxMood
import com.dev.julian09.financeapp.domain.finance.FoxState
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountFlows
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.CardOverview
import com.dev.julian09.financeapp.domain.model.CardPeriod
import com.dev.julian09.financeapp.domain.model.CardStatement
import com.dev.julian09.financeapp.domain.model.CreditCard
import com.dev.julian09.financeapp.domain.model.DeferredPurchase
import com.dev.julian09.financeapp.domain.model.Installment
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.NetBalanceMode
import com.dev.julian09.financeapp.domain.model.NetChoices
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.usecase.HomeDashboard
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/** 2.5 (#253): Saldo neto «Personalizado» by card, with its installment purchases. */
class CustomNetBalanceByCardTest {
    private val due = LocalDate.parse("2026-12-20")
    internal val visaId = UUID(1, 1)
    private val loanId = UUID(1, 2)
    private val phone = UUID(2, 1)
    internal val trip = UUID(2, 2)

    private fun pesos(n: Long) = Money.ofPesos(n)

    private fun debt(
        id: UUID,
        name: String,
        type: AccountType,
        owed: Long,
    ) = AccountBalance(Account(id, name, type, Money.ZERO), AccountFlows(expense = pesos(owed)))

    private fun purchase(
        id: UUID,
        amount: Long,
    ) = DeferredPurchase(id, visaId, UUID.randomUUID(), pesos(amount), 3, BigDecimal.ZERO, RateType.MV, false, due)

    // Each purchase: one installment due this period and one later; plus a paid one that never counts
    private fun plan(
        purchase: UUID,
        principal: Long,
        interest: Long,
    ) = listOf(
        Installment(UUID.randomUUID(), purchase, 1, due.minusMonths(1), pesos(principal), Money.ZERO, status = InstallmentStatus.PAID),
        Installment(UUID.randomUUID(), purchase, 2, due, pesos(principal), pesos(interest)),
        Installment(UUID.randomUUID(), purchase, 3, due.plusMonths(1), pesos(principal), pesos(interest)),
    )

    private val visa = debt(visaId, "Tarjeta Visa", AccountType.CREDIT_CARD, 2_000_000)
    private val loan = debt(loanId, "Préstamo carro", AccountType.LOAN, 10_000_000)
    private val overview =
        CardOverview(
            CreditCard(visaId, pesos(5_000_000), 5, 20, BigDecimal.ZERO, RateType.MV),
            visa,
            CardStatement(
                CardPeriod(due.minusMonths(1), due.minusDays(15), due),
                onePayment = pesos(200_000),
                installmentsPrincipal = pesos(300_000),
                interest = pesos(15_000),
                fees = Money.ZERO,
                paid = Money.ZERO,
                debt = pesos(2_000_000),
                pendingInterest = Money.ZERO,
                pendingFees = Money.ZERO,
            ),
            listOf(purchase(phone, 600_000), purchase(trip, 300_000)),
            plan(phone, 200_000, 10_000) + plan(trip, 100_000, 5_000),
            mapOf(phone to "Celular", trip to "Tiquetes Cartagena"),
        )

    internal fun dashboard(choices: NetChoices) =
        HomeDashboard(
            available = pesos(20_000_000),
            monthlyChange = null,
            previousMonth = YearMonth.of(2026, 11),
            accounts = listOf(visa, loan),
            recent = emptyList(),
            fox = FoxMood(FoxState.entries.first()),
            debts = listOf(visa, loan),
            cards = mapOf(visaId to overview),
            choices = choices,
        )

    @Test
    fun byDefaultEveryCardAndLoanSubtractsItsWholeDebt() {
        val d = dashboard(NetChoices())
        assertEquals(pesos(2_000_000), d.customDebt(visa))
        assertEquals(pesos(10_000_000), d.customDebt(loan))
        assertEquals(pesos(8_000_000), d.net(NetBalanceMode.CUSTOM))
    }

    @Test
    fun aPurchaseLeftOutTakesAwayWhatIsLeftToPayOfIt() {
        // The phone has 2 unpaid installments of 200.000 in principal; interest is not debt yet
        assertEquals(pesos(1_600_000), dashboard(NetChoices(excludedPurchases = setOf(phone))).customDebt(visa))
    }

    @Test
    fun soloElPeriodoSubtractsThisPeriodsPaymentLessTheInstallmentsLeftOut() {
        val period = NetChoices(periodCards = setOf(visaId))
        assertEquals(pesos(515_000), dashboard(period).customDebt(visa))
        // The trip's installment of this period, with its interest
        assertEquals(pesos(410_000), dashboard(period.copy(excludedPurchases = setOf(trip))).customDebt(visa))
        // The loan is never by period
        assertEquals(pesos(10_000_000), dashboard(period).customDebt(loan))
    }

    @Test
    fun itNeverGoesBelowZero() {
        val all = NetChoices(periodCards = setOf(visaId), excludedPurchases = setOf(phone, trip))
        assertEquals(pesos(200_000), dashboard(all).customDebt(visa))
        val paid = overview.copy(statement = overview.statement.copy(paid = pesos(515_000)))
        val d = dashboard(all).copy(cards = mapOf(visaId to paid))
        assertEquals(Money.ZERO, d.customDebt(visa))
    }

    @Test
    fun anUnmarkedDebtIsNotSubtracted() {
        val d = dashboard(NetChoices()).copy(debts = listOf(visa, loan.copy(account = loan.account.copy(includeInNet = false))))
        assertEquals(pesos(18_000_000), d.net(NetBalanceMode.CUSTOM))
    }
}
