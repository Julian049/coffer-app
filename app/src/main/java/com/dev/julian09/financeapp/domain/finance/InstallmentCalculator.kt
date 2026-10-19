package com.dev.julian09.financeapp.domain.finance

import com.dev.julian09.financeapp.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

data class PlannedInstallment(
    val number: Int,
    val dueDate: LocalDate,
    val principal: Money,
    val interest: Money,
)

/**
 * REQ-TC-03, 07, 15, detailed-design §6.9.2: fixed principal plus interest on the remaining balance with a 30-day
 * commercial month (the monthly rate applies whole to each period). Amounts in whole pesos (COP); the last
 * installment absorbs the rounding, so the principal always adds up to the amount.
 */
object InstallmentCalculator {
    fun plan(
        amount: Money,
        count: Int,
        monthlyRate: BigDecimal,
        firstDueDate: LocalDate,
        paymentDay: Int,
        firstInterestFree: Boolean,
    ): List<PlannedInstallment> {
        require(count >= 1) { "count must be at least 1" }
        val base = amount.toBigDecimal().divide(BigDecimal(count), 0, RoundingMode.HALF_EVEN)
        var balance = amount.toBigDecimal()
        return (1..count).map { k ->
            // min: a tiny amount in many installments would otherwise round the balance below zero
            val principal = if (k == count) balance else base.min(balance)
            val interest =
                if ((k == 1 && firstInterestFree) || monthlyRate.signum() == 0) {
                    BigDecimal.ZERO
                } else {
                    balance.multiply(monthlyRate).setScale(0, RoundingMode.HALF_EVEN)
                }
            balance -= principal
            PlannedInstallment(k, dueDate(firstDueDate, k - 1, paymentDay), Money.of(principal), Money.of(interest))
        }
    }

    /** The payment day [offset] months after [first], on the last day of shorter months. */
    fun dueDate(
        first: LocalDate,
        offset: Int,
        paymentDay: Int,
    ): LocalDate = CardPeriodCalculator.dayIn(YearMonth.from(first).plusMonths(offset.toLong()), paymentDay)
}
