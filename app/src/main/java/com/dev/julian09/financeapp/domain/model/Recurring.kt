package com.dev.julian09.financeapp.domain.model

import java.time.LocalDate
import java.util.UUID

/** REQ-REC-02. CUSTOM repeats every [Recurring.intervalCount] [Recurring.intervalUnit]s. */
enum class Frequency { WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM }

enum class IntervalUnit { DAY, WEEK, MONTH }

/** A scheduled expense (REQ-REC-01, 11): a reminder that never moves money until it is paid. */
data class Recurring(
    val id: UUID,
    val name: String,
    val estimatedAmount: Money,
    val accountId: UUID,
    val categoryId: UUID,
    val startDate: LocalDate,
    val frequency: Frequency,
    val nextDueDate: LocalDate,
    val intervalCount: Int? = null,
    val intervalUnit: IntervalUnit? = null,
    val lastPaidAmount: Money? = null,
    val lastNotifiedOn: LocalDate? = null,
    val active: Boolean = true,
) {
    /** What the payment sheet proposes (REQ-REC-07). */
    val suggestedAmount: Money get() = lastPaidAmount ?: estimatedAmount
}

/** REQ-REC-05: grows as the date gets closer; OVERDUE once it passed. */
enum class Urgency { UPCOMING, SOON, OVERDUE }

/** The next occurrence of a recurring expense; computed, never stored. */
data class PendingPayment(
    val recurring: Recurring,
    override val daysLeft: Long,
    override val urgency: Urgency,
    override val inWindow: Boolean,
) : DuePayment {
    override val id: UUID get() = recurring.id
    override val title: String get() = recurring.name
    override val amount: Money get() = recurring.suggestedAmount
    override val dueDate: LocalDate get() = recurring.nextDueDate
}

/** A payment registered from a recurring expense this month. */
data class RecurringPayment(
    val transaction: Transaction,
    val recurringName: String,
)

/** Gastos recurrentes (PO, 2026-10-18): committed = covered + remaining; skipped occurrences count in neither. */
data class RecurringSummary(
    val committed: Money = Money.ZERO,
    val pendingCount: Int = 0,
    val remaining: Money = Money.ZERO,
    val paidCount: Int = 0,
    val covered: Money = Money.ZERO,
    val overdueCount: Int = 0,
)

data class RecurringOverview(
    /** Active recurring expenses by due date. */
    val upcoming: List<PendingPayment>,
    /** Newest first. */
    val paid: List<RecurringPayment>,
    val summary: RecurringSummary,
)

/** REQ-REC-09: one notice when the window opens and one on the due date if still unpaid. */
enum class ReminderKind { WINDOW_OPEN, DUE_TODAY }

/** A notice for a recurring expense or a card statement (S7: same window and notices, PO 2026-10-19). */
data class Reminder(
    val payment: DuePayment,
    val kind: ReminderKind,
)
