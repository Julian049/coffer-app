package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.finance.RecurrenceCalculator
import com.dev.julian09.financeapp.domain.model.Frequency
import com.dev.julian09.financeapp.domain.model.IntervalUnit
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Recurring
import com.dev.julian09.financeapp.domain.model.Urgency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

/** S6-QA01: boundary dates of every frequency (detailed-design §6.9.6). */
class RecurrenceCalculatorTest {
    private fun date(text: String) = LocalDate.parse(text)

    private fun schedule(
        start: String,
        frequency: Frequency,
        count: Int? = null,
        unit: IntervalUnit? = null,
    ) = Recurring(
        UUID.randomUUID(),
        "Arriendo",
        Money.ofPesos(900_000),
        UUID.randomUUID(),
        UUID.randomUUID(),
        date(start),
        frequency,
        date(start),
        count,
        unit,
    )

    private fun dates(
        r: Recurring,
        n: Int,
    ) = generateSequence(r.startDate) { RecurrenceCalculator.next(r, it) }.take(n).map(LocalDate::toString).toList()

    @Test
    fun `a monthly expense on the 31st falls on the last day of short months and comes back`() {
        val rent = schedule("2026-01-31", Frequency.MONTHLY)
        assertEquals(listOf("2026-01-31", "2026-02-28", "2026-03-31", "2026-04-30", "2026-05-31"), dates(rent, 5))
        assertEquals(listOf("2028-01-31", "2028-02-29", "2028-03-31"), dates(schedule("2028-01-31", Frequency.MONTHLY), 3))
    }

    @Test
    fun `a yearly expense on February 29 waits for the next leap year to be on the 29th again`() {
        assertEquals(
            listOf("2028-02-29", "2029-02-28", "2030-02-28", "2031-02-28", "2032-02-29"),
            dates(schedule("2028-02-29", Frequency.YEARLY), 5),
        )
    }

    @Test
    fun `every frequency and custom interval`() {
        assertEquals(listOf("2026-10-30", "2026-11-06", "2026-11-13"), dates(schedule("2026-10-30", Frequency.WEEKLY), 3))
        assertEquals(listOf("2026-12-25", "2027-01-08"), dates(schedule("2026-12-25", Frequency.BIWEEKLY), 2))
        assertEquals(listOf("2026-11-30", "2027-02-28", "2027-05-30"), dates(schedule("2026-11-30", Frequency.QUARTERLY), 3))
        assertEquals(
            listOf("2026-10-05", "2026-10-15", "2026-10-25"),
            dates(schedule("2026-10-05", Frequency.CUSTOM, 10, IntervalUnit.DAY), 3),
        )
        assertEquals(listOf("2026-10-05", "2026-10-19"), dates(schedule("2026-10-05", Frequency.CUSTOM, 2, IntervalUnit.WEEK), 2))
        assertEquals(
            listOf("2026-08-31", "2026-10-31", "2026-12-31"),
            dates(schedule("2026-08-31", Frequency.CUSTOM, 2, IntervalUnit.MONTH), 3),
        )
    }

    @Test
    fun `a start in the past begins at the first date from today on`() {
        val rent = schedule("2024-01-05", Frequency.MONTHLY)
        assertEquals(date("2026-10-05"), RecurrenceCalculator.firstOnOrAfter(rent, date("2026-10-05")))
        assertEquals(date("2026-11-05"), RecurrenceCalculator.firstOnOrAfter(rent, date("2026-10-06")))
        assertEquals(date("2027-03-01"), RecurrenceCalculator.firstOnOrAfter(schedule("2027-03-01", Frequency.WEEKLY), date("2026-10-18")))
        val weekly = schedule("2026-01-01", Frequency.WEEKLY)
        assertEquals(date("2026-10-22"), RecurrenceCalculator.firstOnOrAfter(weekly, date("2026-10-18")))
    }

    @Test
    fun `the occurrences of a month`() {
        val weekly = schedule("2026-10-02", Frequency.WEEKLY)
        assertEquals(5, RecurrenceCalculator.between(weekly, date("2026-10-01"), date("2026-10-31")).size)
        assertEquals(
            emptyList<LocalDate>(),
            RecurrenceCalculator.between(schedule("2026-11-01", Frequency.MONTHLY), date("2026-10-01"), date("2026-10-31")),
        )
    }

    @Test
    fun `urgency and the reminder window`() {
        val today = date("2026-10-18")
        assertEquals(Urgency.OVERDUE, RecurrenceCalculator.urgency(date("2026-10-17"), today))
        assertEquals(Urgency.SOON, RecurrenceCalculator.urgency(today, today))
        assertEquals(Urgency.SOON, RecurrenceCalculator.urgency(date("2026-10-20"), today))
        assertEquals(Urgency.UPCOMING, RecurrenceCalculator.urgency(date("2026-10-21"), today))
        assertTrue(RecurrenceCalculator.inWindow(date("2026-10-23"), today, 5))
        assertFalse(RecurrenceCalculator.inWindow(date("2026-10-24"), today, 5))
        assertTrue(RecurrenceCalculator.inWindow(date("2026-10-01"), today, 5)) // overdue stays visible
        assertFalse(RecurrenceCalculator.inWindow(date("2026-10-19"), today, 0))
        assertTrue(RecurrenceCalculator.inWindow(today, today, 0))
    }
}
