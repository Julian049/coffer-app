package com.dev.julian09.financeapp.domain.finance

import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.CardPeriod
import com.dev.julian09.financeapp.domain.model.CardStatement
import com.dev.julian09.financeapp.domain.model.CreditCard
import com.dev.julian09.financeapp.domain.model.Installment
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.PaymentBreakdown
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Periods, what is due and how a payment splits (REQ-TC-08, 10 to 13, detailed-design §6.9.3). A cut on the 29th to
 * 31st falls on the last day of shorter months.
 */
object CardPeriodCalculator {
    /** [day] of [month], or its last day. */
    fun dayIn(
        month: YearMonth,
        day: Int,
    ): LocalDate = month.atDay(minOf(day, month.lengthOfMonth()))

    /** The period [date] belongs to: from the day after the previous cut to the next cut, both included. */
    fun periodOf(
        date: LocalDate,
        cutOffDay: Int,
        paymentDay: Int,
    ): CardPeriod {
        val month = YearMonth.from(date)
        val cutThisMonth = dayIn(month, cutOffDay)
        val end = if (!date.isAfter(cutThisMonth)) cutThisMonth else dayIn(month.plusMonths(1), cutOffDay)
        val start = dayIn(YearMonth.from(end).minusMonths(1), cutOffDay).plusDays(1)
        return CardPeriod(start, end, dueDateAfter(end, paymentDay))
    }

    /** The first payment day after the cut: cut 20 and payment 5 → the 5th of the next month (detailed-design §6.9.3). */
    fun dueDateAfter(
        cut: LocalDate,
        paymentDay: Int,
    ): LocalDate {
        val sameMonth = dayIn(YearMonth.from(cut), paymentDay)
        return if (sameMonth.isAfter(cut)) sameMonth else dayIn(YearMonth.from(cut).plusMonths(1), paymentDay)
    }

    /** REQ-TC-07: a purchase's first installment is due on its period's payment date. */
    fun firstDueDate(
        purchase: LocalDate,
        cutOffDay: Int,
        paymentDay: Int,
    ): LocalDate = periodOf(purchase, cutOffDay, paymentDay).dueDate

    /** Purchases between the cut and the payment carry no extra interest (PO, 2026-10-19, SRS §10.2). */
    fun interestForGracePeriod(): Money = Money.ZERO

    private class Charges(
        val onePayment: Money,
        val principal: Money,
        val interest: Money,
        val fees: Money,
    ) {
        val due: Money get() = onePayment + principal + interest + fees
    }

    /**
     * The statement to pay now: the last closed period while it is not covered, then the open one. Payments made after
     * a cut cover the period that cut closed; what is left over goes to the open period.
     */
    fun statement(
        card: CreditCard,
        account: Account,
        debt: Money,
        transactions: List<Transaction>,
        installments: List<Installment>,
        today: LocalDate,
        zone: ZoneId,
    ): CardStatement {
        val c = card.cutOffDay
        val p = card.paymentDay
        val open = periodOf(today, c, p)
        val closed = periodOf(open.start.minusDays(1), c, p)
        val created = account.createdAt?.atZone(zone)?.toLocalDate() ?: today

        fun dateOf(t: Transaction) = t.occurredAt.atZone(zone).toLocalDate()

        fun inside(
            date: LocalDate,
            period: CardPeriod,
        ) = !date.isBefore(period.start) && !date.isAfter(period.end)

        fun charges(period: CardPeriod): Charges {
            // REQ-TC-13: the debt the card had when it was created is a purchase in one installment of that period
            val initial = if (inside(created, period)) account.initialBalance else Money.ZERO
            val onePayment =
                transactions
                    .filter {
                        it.type == TransactionType.EXPENSE &&
                            it.accountId == card.accountId &&
                            it.installments <= 1 &&
                            inside(dateOf(it), period)
                    }.fold(initial) { sum, t -> sum + t.amount }
            val due = installments.filter { it.status != InstallmentStatus.CANCELLED && it.dueDate == period.dueDate }
            return Charges(
                onePayment,
                due.fold(Money.ZERO) { sum, i -> sum + i.principal },
                due.fold(Money.ZERO) { sum, i -> sum + i.interest },
                due.fold(Money.ZERO) { sum, i -> sum + i.otherCosts },
            )
        }

        val payments =
            transactions.filter {
                it.type == TransactionType.TRANSFER &&
                    it.destinationAccountId == card.accountId &&
                    dateOf(it).isAfter(closed.end) &&
                    !dateOf(it).isAfter(open.end)
            }
        val paid = payments.fold(Money.ZERO) { sum, t -> sum + t.amount }
        val paidInterest = payments.fold(Money.ZERO) { sum, t -> sum + (t.interest ?: Money.ZERO) }
        val paidFees = payments.fold(Money.ZERO) { sum, t -> sum + (t.fees ?: Money.ZERO) }
        val owed = if (debt.isNegative) Money.ZERO else debt

        val last = charges(closed)
        if (paid < last.due) {
            return CardStatement(
                closed,
                last.onePayment,
                last.principal,
                last.interest,
                last.fees,
                paid,
                owed,
                pendingInterest = positive(last.interest - paidInterest),
                pendingFees = positive(last.fees - paidFees),
            )
        }
        val now = charges(open)
        return CardStatement(
            open,
            now.onePayment,
            now.principal,
            now.interest,
            now.fees,
            paid - last.due,
            owed,
            pendingInterest = positive(now.interest - positive(paidInterest - last.interest)),
            pendingFees = positive(now.fees - positive(paidFees - last.fees)),
        )
    }

    /** REQ-TC-11: an amount covers fees first, then interest, then principal. */
    fun breakdown(
        statement: CardStatement,
        amount: Money,
    ): PaymentBreakdown {
        val fees = minOf(amount, statement.pendingFees)
        val interest = minOf(amount - fees, statement.pendingInterest)
        return PaymentBreakdown(amount - fees - interest, interest, fees)
    }

    private fun positive(value: Money) = if (value.isNegative) Money.ZERO else value

    private fun minOf(
        a: Money,
        b: Money,
    ) = if (a < b) a else b
}
