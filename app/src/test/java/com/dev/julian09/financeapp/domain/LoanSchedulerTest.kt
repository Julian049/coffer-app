package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.finance.LoanScheduler
import com.dev.julian09.financeapp.domain.model.Amortization
import com.dev.julian09.financeapp.domain.model.ExtraPaymentMode
import com.dev.julian09.financeapp.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** S10-QA01: detailed-design §6.9.4 with the example of HU-14 ($5.000.000 in 24 installments at 1,5 % M.V.). */
class LoanSchedulerTest {
    private val amount = Money.ofPesos(5_000_000)
    private val rate = BigDecimal("0.015")
    private val first = LocalDate.of(2026, 11, 5)

    private fun plan(amortization: Amortization) =
        LoanScheduler.plan(amount, 24, rate, first, 5, amortization, Money.ofPesos(8_000), Money.ZERO)

    @Test
    fun fixedPrincipalKeepsTheCapitalAndTheInstallmentGoesDown() {
        val plan = plan(Amortization.FIXED_PRINCIPAL)
        assertEquals(24, plan.size)
        // As in the Figma: capital $208.333 + interest $75.000 + insurance $8.000
        assertEquals(Money.ofPesos(208_333), plan[0].principal)
        assertEquals(Money.ofPesos(75_000), plan[0].interest)
        assertEquals(Money.ofPesos(291_333), plan[0].total)
        assertTrue(plan[23].total < plan[0].total)
        assertEquals(amount, plan.fold(Money.ZERO) { s, i -> s + i.principal })
        assertEquals(LocalDate.of(2028, 10, 5), plan.last().dueDate)
    }

    @Test
    fun fixedPaymentKeepsTheInstallmentAndTheCapitalGoesUp() {
        val plan = plan(Amortization.FIXED_PAYMENT)
        // c = 5.000.000 × 0,015 / (1 − 1,015^−24) = 249.620,52…
        assertEquals(Money.ofPesos(249_621), LoanScheduler.fixedPayment(amount, 24, rate))
        assertEquals(Money.ofPesos(249_621), plan[0].principal + plan[0].interest)
        assertEquals(Money.ofPesos(249_621), plan[12].principal + plan[12].interest)
        assertEquals(Money.ofPesos(75_000), plan[0].interest)
        assertTrue(plan[23].principal > plan[0].principal)
        assertEquals(amount, plan.fold(Money.ZERO) { s, i -> s + i.principal })
        assertEquals(Money.ofPesos(8_000), plan[5].insurance)
    }

    @Test
    fun noInterestSplitsTheAmountEvenly() {
        assertEquals(Money.ofPesos(500_000), LoanScheduler.fixedPayment(Money.ofPesos(6_000_000), 12, BigDecimal.ZERO))
    }

    @Test
    fun anExtraPaymentReducesTheTermOrThePayment() {
        val balance = Money.ofPesos(4_375_000)

        fun after(
            amortization: Amortization,
            mode: ExtraPaymentMode,
        ) = LoanScheduler.afterExtraPayment(
            balance = balance,
            extra = Money.ofPesos(1_000_000),
            remaining = 21,
            currentPrincipal = Money.ofPesos(208_333),
            currentPayment = LoanScheduler.fixedPayment(amount, 24, rate),
            monthlyRate = rate,
            nextDueDate = LocalDate.of(2027, 2, 5),
            paymentDay = 5,
            amortization = amortization,
            mode = mode,
            firstNumber = 4,
        )
        // Fixed principal: 3.375.000 / 208.333 → 17 installments (the Figma's "quedan 16" rounds differently)
        val shorter = after(Amortization.FIXED_PRINCIPAL, ExtraPaymentMode.REDUCE_TERM)
        assertEquals(17, shorter.size)
        assertEquals(4, shorter.first().number)
        assertEquals(Money.ofPesos(3_375_000), shorter.fold(Money.ZERO) { s, i -> s + i.principal })
        val lower = after(Amortization.FIXED_PRINCIPAL, ExtraPaymentMode.REDUCE_PAYMENT)
        assertEquals(21, lower.size)
        assertTrue(lower.first().principal < Money.ofPesos(208_333))
        // Fixed payment: the payment stays about the same and the term shrinks
        val french = after(Amortization.FIXED_PAYMENT, ExtraPaymentMode.REDUCE_TERM)
        assertTrue(french.size < 21)
        assertTrue(french.first().principal + french.first().interest <= LoanScheduler.fixedPayment(amount, 24, rate))
        assertTrue(after(Amortization.FIXED_PAYMENT, ExtraPaymentMode.REDUCE_PAYMENT).size == 21)
        // Paying everything leaves no plan
        assertTrue(
            LoanScheduler
                .afterExtraPayment(
                    balance,
                    balance,
                    21,
                    Money.ofPesos(208_333),
                    Money.ofPesos(283_333),
                    rate,
                    LocalDate.of(2027, 2, 5),
                    5,
                    Amortization.FIXED_PRINCIPAL,
                    ExtraPaymentMode.REDUCE_TERM,
                ).isEmpty(),
        )
    }
}
