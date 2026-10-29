package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.finance.FoxRules
import com.dev.julian09.financeapp.domain.finance.FoxState
import com.dev.julian09.financeapp.domain.finance.RecurrenceCalculator
import com.dev.julian09.financeapp.domain.model.Frequency
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.PeriodTotals
import com.dev.julian09.financeapp.domain.model.Recurring
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

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
        assertEquals(FoxState.GOAL, state(previous = saved, today = firstWeek, month = saved))
        // 2.2: GOOD when this month's income beats its expenses; NORMAL while no income came in
        assertEquals(FoxState.GOOD, state(month = saved))
        assertEquals(FoxState.NORMAL, state(month = PeriodTotals(expense = pesos(100_000))))
        assertEquals(FoxState.NORMAL, state())
    }

    private fun dueIn(days: Long) =
        RecurrenceCalculator.pending(
            Recurring(
                UUID.randomUUID(),
                "Arriendo",
                pesos(900_000),
                UUID.randomUUID(),
                UUID.randomUUID(),
                midMonth,
                Frequency.MONTHLY,
                midMonth.plusDays(days),
            ),
            midMonth,
            5,
        )

    @Test
    fun `a payment due in two days or less makes the fox attentive, after debt`() {
        val month = PeriodTotals(pesos(2_100_000), pesos(2_480_000))

        fun state(
            vararg pending: Long,
            available: Long = 1_000,
        ) = FoxRules.evaluate(true, pesos(available), null, month, PeriodTotals(), midMonth, pending.map(::dueIn)).state
        assertEquals(FoxState.ATTENTIVE, state(2))
        assertEquals(FoxState.ATTENTIVE, state(10, -3))
        assertEquals(FoxState.WORRIED, state(3))
        assertEquals(FoxState.DEBT, state(0, available = -1))
        val mood = FoxRules.evaluate(true, pesos(1), null, PeriodTotals(), PeriodTotals(), midMonth, listOf(dueIn(5), dueIn(1)))
        assertEquals(1L, mood.payment!!.daysLeft)
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
