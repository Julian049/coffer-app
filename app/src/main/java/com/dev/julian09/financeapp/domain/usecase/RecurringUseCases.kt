package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.finance.RecurrenceCalculator
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.Frequency
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Recurring
import com.dev.julian09.financeapp.domain.model.RecurringOverview
import com.dev.julian09.financeapp.domain.model.RecurringPayment
import com.dev.julian09.financeapp.domain.model.RecurringSummary
import com.dev.julian09.financeapp.domain.model.Urgency
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.RecurringRepository
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import javax.inject.Inject

private const val NAME_MAX = 60
private const val INTERVAL_MAX = 365

typealias RecurringErrors = Map<RecurringField, LedgerError>

enum class RecurringField { NAME, AMOUNT, ACCOUNT, CATEGORY, INTERVAL }

/** REQ-REC-01, 02, 11: the same rules as the server's validator (ck_rec_*), checked before saving. */
class RecurringValidator
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val categories: CategoryRepository,
    ) {
        suspend fun check(r: Recurring): RecurringErrors {
            val errors = mutableMapOf<RecurringField, LedgerError>()
            when {
                r.name.isEmpty() -> errors[RecurringField.NAME] = LedgerError.NAME_REQUIRED
                r.name.length > NAME_MAX -> errors[RecurringField.NAME] = LedgerError.NAME_TOO_LONG
            }
            if (r.estimatedAmount <= Money.ZERO) errors[RecurringField.AMOUNT] = LedgerError.AMOUNT_REQUIRED
            if (accounts.observeActive().first().none { it.id == r.accountId }) {
                errors[RecurringField.ACCOUNT] = LedgerError.ACCOUNT_REQUIRED
            }
            // Only expense categories (REQ-REC-11); the form offers no other
            if (categories.observeAll().first().none { it.id == r.categoryId && it.kind == CategoryKind.EXPENSE }) {
                errors[RecurringField.CATEGORY] = LedgerError.CATEGORY_REQUIRED
            }
            val count = r.intervalCount
            if (r.frequency == Frequency.CUSTOM && (count == null || count !in 1..INTERVAL_MAX || r.intervalUnit == null)) {
                errors[RecurringField.INTERVAL] = LedgerError.INTERVAL_INVALID
            }
            return errors
        }
    }

/** Only CUSTOM carries an interval (server ck_rec_custom). */
private fun Recurring.normalized() =
    copy(
        name = name.trim(),
        intervalCount = intervalCount.takeIf { frequency == Frequency.CUSTOM },
        intervalUnit = intervalUnit.takeIf { frequency == Frequency.CUSTOM },
    )

/** A start in the past begins at the first date from today on (PO, 2026-10-18): no backlog of overdue payments. */
class CreateRecurring
    @Inject
    constructor(
        private val repository: RecurringRepository,
        private val validator: RecurringValidator,
        private val scheduler: SyncScheduler,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(recurring: Recurring): RecurringErrors {
            val clean = recurring.normalized()
            validator.check(clean).takeIf { it.isNotEmpty() }?.let { return it }
            repository.create(
                clean.copy(
                    nextDueDate = RecurrenceCalculator.firstOnOrAfter(clean, clock.today()),
                    lastPaidAmount = null,
                    lastNotifiedOn = null,
                    active = true,
                ),
            )
            scheduler.requestSync()
            return emptyMap()
        }
    }

/** A new start or frequency recomputes the next date like a new one; a new amount replaces the last payment's. */
class UpdateRecurring
    @Inject
    constructor(
        private val repository: RecurringRepository,
        private val validator: RecurringValidator,
        private val scheduler: SyncScheduler,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(recurring: Recurring): RecurringErrors {
            val clean = recurring.normalized()
            validator.check(clean).takeIf { it.isNotEmpty() }?.let { return it }
            val stored = repository.observe(clean.id).first() ?: return emptyMap()
            val rescheduled =
                clean.startDate != stored.startDate ||
                    clean.frequency != stored.frequency ||
                    clean.intervalCount != stored.intervalCount ||
                    clean.intervalUnit != stored.intervalUnit
            repository.update(
                clean.copy(
                    nextDueDate = if (rescheduled) RecurrenceCalculator.firstOnOrAfter(clean, clock.today()) else stored.nextDueDate,
                    lastNotifiedOn = if (rescheduled) null else stored.lastNotifiedOn,
                    lastPaidAmount = stored.lastPaidAmount.takeIf { clean.estimatedAmount == stored.estimatedAmount },
                    active = stored.active,
                ),
            )
            scheduler.requestSync()
            return emptyMap()
        }
    }

/** Gastos recurrentes (S6-UI01): the active ones by due date, this month's payments and the month's summary. */
class ObserveRecurringOverview
    @Inject
    constructor(
        private val recurring: RecurringRepository,
        private val transactions: TransactionRepository,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) {
        operator fun invoke(): Flow<RecurringOverview> {
            val today = clock.today()
            val zone = clock.zone()
            val month = YearMonth.from(today)
            val monthEnd = month.atEndOfMonth()
            val from = month.atDay(1).atStartOfDay(zone).toInstant()
            val to =
                month
                    .plusMonths(1)
                    .atDay(1)
                    .atStartOfDay(zone)
                    .toInstant()
            return combine(
                recurring.observeAll(),
                settings.observe(),
                transactions.observeRecurringPayments(from, to),
            ) { all, s, payments ->
                val active = all.filter { it.active }
                val upcoming = active.map { RecurrenceCalculator.pending(it, today, s.reminderLeadDays) }
                val names = all.associate { it.id to it.name }
                val paid = payments.map { RecurringPayment(it, names[it.recurringId] ?: "Gasto recurrente") }
                // Unpaid dates up to the end of the month, overdue ones from earlier months included
                val unpaid =
                    active.flatMap { r ->
                        generateSequence(r.nextDueDate) { RecurrenceCalculator.next(r, it) }
                            .takeWhile { !it.isAfter(monthEnd) }
                            .map { r.suggestedAmount }
                            .toList()
                    }
                val covered = paid.fold(Money.ZERO) { sum, p -> sum + p.transaction.amount }
                val remaining = unpaid.fold(Money.ZERO) { sum, amount -> sum + amount }
                RecurringOverview(
                    upcoming = upcoming,
                    paid = paid,
                    summary =
                        RecurringSummary(
                            committed = covered + remaining,
                            pendingCount = unpaid.size,
                            remaining = remaining,
                            paidCount = paid.size,
                            covered = covered,
                            overdueCount = upcoming.count { it.urgency == Urgency.OVERDUE },
                        ),
                )
            }
        }
    }
