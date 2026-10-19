package com.dev.julian09.financeapp.domain.finance

import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.PendingPayment
import com.dev.julian09.financeapp.domain.model.PeriodTotals
import java.math.BigDecimal
import java.time.LocalDate

/** Mascot states that Home can show. */
enum class FoxState { RESTING, DEBT, ATTENTIVE, WORRIED, GOAL, GOOD, NORMAL }

data class FoxMood(
    val state: FoxState,
    /** GOAL: what was saved last month; DEBT: the negative available balance. */
    val amount: Money? = null,
    /** WORRIED: this month's totals. */
    val month: PeriodTotals? = null,
    /** GOOD: percent above last month's close. */
    val change: BigDecimal? = null,
    /** ATTENTIVE: the closest payment. */
    val payment: PendingPayment? = null,
)

/** Rules approved by the PO on 2026-10-17: the first that holds wins. */
object FoxRules {
    private const val GOAL_DAYS = 7

    fun evaluate(
        hasMovements: Boolean,
        available: Money,
        monthlyChange: BigDecimal?,
        month: PeriodTotals,
        previousMonth: PeriodTotals,
        today: LocalDate,
        pending: List<PendingPayment> = emptyList(),
    ): FoxMood {
        val saved = previousMonth.income - previousMonth.expense
        val closest = pending.minByOrNull { it.daysLeft }
        return when {
            !hasMovements -> FoxMood(FoxState.RESTING)
            available.isNegative -> FoxMood(FoxState.DEBT, amount = available)
            // A payment due in ≤ 2 days, or already overdue
            closest != null && closest.daysLeft <= RecurrenceCalculator.SOON_DAYS -> FoxMood(FoxState.ATTENTIVE, payment = closest)
            // Only once some income arrived, so the days before payday do not look alarming
            month.income > Money.ZERO && month.expense > month.income -> FoxMood(FoxState.WORRIED, month = month)
            today.dayOfMonth <= GOAL_DAYS && saved > Money.ZERO -> FoxMood(FoxState.GOAL, amount = saved)
            monthlyChange != null && monthlyChange.signum() > 0 -> FoxMood(FoxState.GOOD, change = monthlyChange)
            else -> FoxMood(FoxState.NORMAL)
        }
    }
}
