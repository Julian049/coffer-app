package com.dev.julian09.financeapp.domain.finance

import com.dev.julian09.financeapp.domain.model.Amortization
import com.dev.julian09.financeapp.domain.model.ExtraPaymentMode
import com.dev.julian09.financeapp.domain.model.Money
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.ln

data class PlannedLoanInstallment(
    val number: Int,
    val dueDate: LocalDate,
    val principal: Money,
    val interest: Money,
    val insurance: Money,
    val otherCosts: Money,
) {
    val total: Money get() = principal + interest + insurance + otherCosts
}

/**
 * detailed-design §6.9.4 (REQ-PRE-03, 06): the plan of a loan with fixed principal or fixed payment, whole pesos, the
 * last installment absorbing the rounding; insurance and other costs are added to every installment.
 */
object LoanScheduler {
    private val MC = MathContext(20)

    fun plan(
        amount: Money,
        count: Int,
        monthlyRate: BigDecimal,
        firstDueDate: LocalDate,
        paymentDay: Int,
        amortization: Amortization,
        insurance: Money = Money.ZERO,
        otherCosts: Money = Money.ZERO,
        firstNumber: Int = 1,
    ): List<PlannedLoanInstallment> {
        require(count >= 1) { "count must be at least 1" }
        val base =
            when (amortization) {
                Amortization.FIXED_PRINCIPAL ->
                    InstallmentCalculator
                        .plan(amount, count, monthlyRate, firstDueDate, paymentDay, firstInterestFree = false)
                        .map { Triple(it.dueDate, it.principal, it.interest) }
                Amortization.FIXED_PAYMENT -> {
                    val payment = fixedPayment(amount, count, monthlyRate).toBigDecimal()
                    var balance = amount.toBigDecimal()
                    (1..count).map { k ->
                        val interest = balance.multiply(monthlyRate).setScale(0, RoundingMode.HALF_EVEN)
                        val principal = if (k == count) balance else (payment - interest).max(BigDecimal.ZERO).min(balance)
                        balance -= principal
                        Triple(InstallmentCalculator.dueDate(firstDueDate, k - 1, paymentDay), Money.of(principal), Money.of(interest))
                    }
                }
            }
        return base.mapIndexed { k, (due, principal, interest) ->
            PlannedLoanInstallment(firstNumber + k, due, principal, interest, insurance, otherCosts)
        }
    }

    /** c = P·i / (1 − (1 + i)^−n), whole pesos; P / n without interest. */
    fun fixedPayment(
        amount: Money,
        count: Int,
        monthlyRate: BigDecimal,
    ): Money {
        val p = amount.toBigDecimal()
        if (monthlyRate.signum() == 0) return Money.of(p.divide(BigDecimal(count), 0, RoundingMode.HALF_EVEN))
        val factor = BigDecimal.ONE.add(monthlyRate).pow(count, MC)
        val payment = p.multiply(monthlyRate).multiply(factor).divide(factor - BigDecimal.ONE, MC)
        return Money.of(payment.setScale(0, RoundingMode.HALF_EVEN))
    }

    /**
     * REQ-PRE-06: the plan after paying [extra] off a [balance] with [remaining] installments left, the first due on
     * [nextDueDate]. Reducing the term keeps the current principal (fixed principal) or payment (fixed payment);
     * reducing the payment keeps the end date. Empty when the extra pays the whole loan.
     */
    fun afterExtraPayment(
        balance: Money,
        extra: Money,
        remaining: Int,
        currentPrincipal: Money,
        currentPayment: Money,
        monthlyRate: BigDecimal,
        nextDueDate: LocalDate,
        paymentDay: Int,
        amortization: Amortization,
        mode: ExtraPaymentMode,
        insurance: Money = Money.ZERO,
        otherCosts: Money = Money.ZERO,
        firstNumber: Int = 1,
    ): List<PlannedLoanInstallment> {
        val left = balance - extra
        if (left <= Money.ZERO || remaining < 1) return emptyList()
        val count =
            when (mode) {
                ExtraPaymentMode.REDUCE_PAYMENT -> remaining
                ExtraPaymentMode.REDUCE_TERM ->
                    termFor(
                        left,
                        currentPrincipal,
                        currentPayment,
                        monthlyRate,
                        amortization,
                    ).coerceIn(1, remaining)
            }
        return plan(left, count, monthlyRate, nextDueDate, paymentDay, amortization, insurance, otherCosts, firstNumber)
    }

    private fun termFor(
        left: Money,
        principal: Money,
        payment: Money,
        monthlyRate: BigDecimal,
        amortization: Amortization,
    ): Int {
        val s = left.toBigDecimal().toDouble()
        return when {
            amortization == Amortization.FIXED_PRINCIPAL || monthlyRate.signum() == 0 -> {
                val per = (if (amortization == Amortization.FIXED_PRINCIPAL) principal else payment).toBigDecimal().toDouble()
                if (per <= 0) 1 else ceil(s / per).toInt()
            }
            else -> {
                // n = −ln(1 − S·i / c) / ln(1 + i); a count, so a double is fine (money stays in BigDecimal)
                val i = monthlyRate.toDouble()
                val c = payment.toBigDecimal().toDouble()
                val x = 1 - s * i / c
                if (x <= 0) Int.MAX_VALUE else ceil(-ln(x) / ln(1 + i)).toInt()
            }
        }
    }
}
