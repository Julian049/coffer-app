package com.dev.julian09.financeapp.domain.finance

import com.dev.julian09.financeapp.domain.model.Frequency
import com.dev.julian09.financeapp.domain.model.IntervalUnit
import com.dev.julian09.financeapp.domain.model.PendingPayment
import com.dev.julian09.financeapp.domain.model.Recurring
import com.dev.julian09.financeapp.domain.model.Urgency
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Dates of a recurring expense (detailed-design §6.9.6, REQ-REC-02, 04, 05, 08). Every date is the start plus k steps,
 * never the previous date plus one: a monthly expense on the 31st falls on Feb 28 and is back on Mar 31.
 */
object RecurrenceCalculator {
    /** Two days or less to the due date is SOON (and the fox gets attentive). */
    const val SOON_DAYS = 2L

    private class Step(
        val amount: Long,
        val unit: ChronoUnit,
    )

    private fun step(r: Recurring): Step =
        when (r.frequency) {
            Frequency.WEEKLY -> Step(7, ChronoUnit.DAYS)
            Frequency.BIWEEKLY -> Step(14, ChronoUnit.DAYS)
            Frequency.MONTHLY -> Step(1, ChronoUnit.MONTHS)
            Frequency.QUARTERLY -> Step(3, ChronoUnit.MONTHS)
            Frequency.YEARLY -> Step(12, ChronoUnit.MONTHS)
            Frequency.CUSTOM -> {
                val n = (r.intervalCount ?: 1).toLong()
                when (r.intervalUnit ?: IntervalUnit.DAY) {
                    IntervalUnit.DAY -> Step(n, ChronoUnit.DAYS)
                    IntervalUnit.WEEK -> Step(7 * n, ChronoUnit.DAYS)
                    IntervalUnit.MONTH -> Step(n, ChronoUnit.MONTHS)
                }
            }
        }

    private fun occurrence(
        r: Recurring,
        step: Step,
        k: Long,
    ): LocalDate = r.startDate.plus(k * step.amount, step.unit)

    /** The first date of the schedule on or after [date]; the start itself if [date] is earlier. */
    fun firstOnOrAfter(
        r: Recurring,
        date: LocalDate,
    ): LocalDate {
        if (!date.isAfter(r.startDate)) return r.startDate
        val step = step(r)
        // An estimate from whole steps, then corrected: month lengths make it off by one at most
        var k = step.unit.between(r.startDate, date) / step.amount
        while (occurrence(r, step, k).isBefore(date)) k++
        while (k > 0 && !occurrence(r, step, k - 1).isBefore(date)) k--
        return occurrence(r, step, k)
    }

    /** After paying or skipping [after] (REQ-REC-08). */
    fun next(
        r: Recurring,
        after: LocalDate,
    ): LocalDate = firstOnOrAfter(r, after.plusDays(1))

    /** Every date of the schedule in [from, to], both included. */
    fun between(
        r: Recurring,
        from: LocalDate,
        to: LocalDate,
    ): List<LocalDate> =
        generateSequence(firstOnOrAfter(r, from)) { next(r, it) }
            .takeWhile { !it.isAfter(to) }
            .toList()

    fun urgency(
        due: LocalDate,
        today: LocalDate,
    ): Urgency =
        when {
            today.isAfter(due) -> Urgency.OVERDUE
            ChronoUnit.DAYS.between(today, due) <= SOON_DAYS -> Urgency.SOON
            else -> Urgency.UPCOMING
        }

    /** The reminder window opens [leadDays] before the due date and stays open while it is unpaid (REQ-REC-03, 04). */
    fun inWindow(
        due: LocalDate,
        today: LocalDate,
        leadDays: Int,
    ): Boolean = !today.isBefore(due.minusDays(leadDays.toLong()))

    fun pending(
        r: Recurring,
        today: LocalDate,
        leadDays: Int,
    ) = PendingPayment(
        recurring = r,
        daysLeft = ChronoUnit.DAYS.between(today, r.nextDueDate),
        urgency = urgency(r.nextDueDate, today),
        inWindow = inWindow(r.nextDueDate, today, leadDays),
    )
}
