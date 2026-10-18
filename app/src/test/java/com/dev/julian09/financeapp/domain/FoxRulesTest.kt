package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.finance.FoxRules
import com.dev.julian09.financeapp.domain.finance.FoxState
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.PeriodTotals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** The order approved by the PO (2026-10-17): the first rule that holds wins. */
class FoxRulesTest {
    private fun pesos(value: Long) = Money.ofPesos(value)

    private val midMonth = LocalDate.of(2026, 10, 17)
    private val firstWeek = LocalDate.of(2026, 10, 3)
    private val saved = PeriodTotals(income = pesos(3_000_000), expense = pesos(2_350_000))

    private fun state(
        hasMovements: Boolean = true,
        available: Long = 1_000_000,
        change: BigDecimal? = null,
        month: PeriodTotals = PeriodTotals(),
        previous: PeriodTotals = PeriodTotals(),
        today: LocalDate = midMonth,
    ) = FoxRules.evaluate(hasMovements, pesos(available), change, month, previous, today).state

    @Test
    fun `each state and its precedence`() {
        assertEquals(FoxState.RESTING, state(hasMovements = false, available = -5))
        assertEquals(FoxState.DEBT, state(available = -1, month = PeriodTotals(pesos(1), pesos(9))))
        assertEquals(FoxState.WORRIED, state(month = PeriodTotals(pesos(2_100_000), pesos(2_480_000)), previous = saved, today = firstWeek))
        assertEquals(FoxState.GOAL, state(previous = saved, today = firstWeek, change = BigDecimal.TEN))
        assertEquals(FoxState.GOOD, state(previous = saved, change = BigDecimal("8.4")))
        assertEquals(FoxState.NORMAL, state(change = BigDecimal("-5.2")))
        assertEquals(FoxState.NORMAL, state(change = null))
    }

    @Test
    fun `spending before any income this month is not worrying (option B)`() {
        assertEquals(FoxState.NORMAL, state(month = PeriodTotals(Money.ZERO, pesos(400_000))))
    }

    @Test
    fun `the closed month only counts in the first 7 days and when it saved`() {
        assertEquals(FoxState.GOAL, state(previous = saved, today = LocalDate.of(2026, 10, 7)))
        assertEquals(FoxState.NORMAL, state(previous = saved, today = LocalDate.of(2026, 10, 8)))
        assertEquals(FoxState.NORMAL, state(previous = PeriodTotals(pesos(1), pesos(1)), today = firstWeek))
        val goal = FoxRules.evaluate(true, pesos(1), null, PeriodTotals(), saved, firstWeek)
        assertEquals(pesos(650_000), goal.amount)
    }
}
