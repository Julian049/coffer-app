package com.dev.julian09.financeapp.domain.finance

import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.DeferredPurchase
import com.dev.julian09.financeapp.domain.model.Installment
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.model.excludedFromReports
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID

enum class ReportSpan { WEEK, MONTH, RANGE }

/** Days [from] to [to], both included. */
data class ReportPeriod(
    val from: LocalDate,
    val to: LocalDate,
) {
    val days: Long get() = to.toEpochDay() - from.toEpochDay() + 1

    operator fun contains(date: LocalDate) = date in from..to
}

data class PeriodSums(
    val period: ReportPeriod,
    val income: Money = Money.ZERO,
    val expense: Money = Money.ZERO,
)

/** A main category with its subcategories; [total] includes them. Movements without a category are left out. */
data class CategorySum(
    val category: Category,
    val total: Money,
    val children: List<Pair<Category, Money>>,
)

/**
 * Ritmo de gasto (S13-DOM01): what was spent day by day, added up, this month up to [today] and the whole month before.
 * [matchDay] is the first day of the month before that reached today's spending; null when it never did.
 */
data class Pace(
    val today: Int,
    val current: List<Money>,
    val previous: List<Money>,
    val matchDay: Int?,
) {
    val spent: Money get() = current.lastOrNull() ?: Money.ZERO

    /** Positive: spending runs ahead of the month before (more spent by today); negative: behind. */
    val daysAhead: Int? get() = matchDay?.let { it - today }
}

/** Cambios por categoría (S13-DOM01): a main category this period and the one before, both up to the same day. */
data class CategoryChange(
    val category: Category,
    val now: Money,
    val before: Money,
) {
    val delta: Money get() = now - before

    /** Whole percent; null when there was nothing before. */
    val percent: Int? get() = percentOf(delta, before)
}

/** Whole percent of [part] over [whole], rounded half up; null when [whole] is zero. */
fun percentOf(
    part: Money,
    whole: Money,
): Int? =
    if (whole == Money.ZERO) {
        null
    } else {
        part
            .toBigDecimal()
            .multiply(BigDecimal(100))
            .divide(whole.toBigDecimal().abs(), 0, RoundingMode.HALF_UP)
            .toInt()
    }

data class Report(
    val period: ReportPeriod,
    val income: Money,
    val expense: Money,
    /** Oldest first, ending with [period] (REQ-REP-02). */
    val evolution: List<PeriodSums>,
    val expenseByCategory: List<CategorySum>,
    val incomeByCategory: List<CategorySum>,
    /** Interest and fees paid per card account (REQ-REP-04; loans arrive in S10). */
    val interestByCard: Map<UUID, Money>,
    /** Movements registered in the period, transfers included (REQ-REP-05). */
    val count: Int,
    /** A month only (S13). */
    val pace: Pace? = null,
    /** Main expense categories by how much they changed against the period before, to the same day (S13). */
    val changes: List<CategoryChange> = emptyList(),
    /** False while [period] is still running (S13: «en curso»). */
    val closed: Boolean = true,
) {
    /** Ahorro del mes (S13): what is left of the income. */
    val savingsRate: Int? get() = if (income > Money.ZERO) percentOf(balance, income) else null

    /** The periods before this one that already closed, for the average saving. */
    private val history: List<PeriodSums> get() = evolution.dropLast(1).filter { it.income > Money.ZERO || it.expense > Money.ZERO }

    val averageSavings: Money?
        get() =
            history
                .takeIf { it.isNotEmpty() }
                ?.let { list ->
                    Money.of(
                        list
                            .fold(
                                BigDecimal.ZERO,
                            ) { s, p -> s + (p.income - p.expense).toBigDecimal() }
                            .divide(BigDecimal(list.size), 0, RoundingMode.HALF_UP),
                    )
                }

    val averageRate: Int?
        get() {
            val rated = history.filter { it.income > Money.ZERO }
            if (rated.isEmpty()) return null
            val income = rated.fold(Money.ZERO) { s, p -> s + p.income }
            return percentOf(rated.fold(Money.ZERO) { s, p -> s + p.income - p.expense }, income)
        }

    /** Total change of the expenses to the same day, for «… explican la subida del 3 %». */
    val changePercent: Int?
        get() = percentOf(changes.fold(Money.ZERO) { s, c -> s + c.delta }, changes.fold(Money.ZERO) { s, c -> s + c.before })

    val balance: Money get() = income - expense
    val interest: Money get() = interestByCard.values.fold(Money.ZERO, Money::plus)
    val perDay: BigDecimal get() = BigDecimal(count).divide(BigDecimal(period.days), 1, RoundingMode.HALF_UP)

    /** Whole percent of expenses against the previous period; null without a previous expense. */
    val expenseChange: Int?
        get() {
            val previous = evolution.getOrNull(evolution.size - 2)?.expense ?: return null
            if (previous == Money.ZERO) return null
            return (expense - previous)
                .toBigDecimal()
                .multiply(BigDecimal(100))
                .divide(previous.toBigDecimal(), 0, RoundingMode.HALF_UP)
                .toInt()
        }
}

/** Reglas de detailed-design §6.9.15 (S9-DOM01): pure, so the same numbers come out of tests and screens. */
object ReportAggregator {
    const val EVOLUTION = 6

    fun period(
        span: ReportSpan,
        anchor: LocalDate,
        to: LocalDate = anchor,
    ): ReportPeriod =
        when (span) {
            ReportSpan.WEEK -> anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { ReportPeriod(it, it.plusDays(6)) }
            ReportSpan.MONTH -> anchor.withDayOfMonth(1).let { ReportPeriod(it, it.plusMonths(1).minusDays(1)) }
            ReportSpan.RANGE -> ReportPeriod(minOf(anchor, to), maxOf(anchor, to))
        }

    /** The [EVOLUTION] periods ending with [period]; a range compares the months ending in its last day. */
    fun evolution(
        span: ReportSpan,
        period: ReportPeriod,
    ): List<ReportPeriod> =
        (EVOLUTION - 1 downTo 0).map { back ->
            when (span) {
                ReportSpan.WEEK -> period(ReportSpan.WEEK, period.from.minusWeeks(back.toLong()))
                ReportSpan.MONTH -> period(ReportSpan.MONTH, period.from.minusMonths(back.toLong()))
                ReportSpan.RANGE -> period(ReportSpan.MONTH, period.to.minusMonths(back.toLong()))
            }
        }

    /**
     * [transactions] must cover the whole evolution; [purchases] are the deferred purchases' expenses, whenever they
     * happened, because their installments count on each due date (REQ-TC-09). [loanCategoryId] receives the principal of loan
     * payments (S15-H06).
     */
    fun aggregate(
        span: ReportSpan,
        period: ReportPeriod,
        today: LocalDate,
        zone: ZoneId,
        transactions: List<Transaction>,
        categories: List<Category>,
        deferred: List<DeferredPurchase>,
        installments: List<Installment>,
        purchases: List<Transaction>,
        loanCategoryId: UUID? = null,
    ): Report {
        val byId = categories.associateBy { it.id }
        val financialCharges = categories.firstOrNull { it.systemKey == FINANCIAL_CHARGES }?.id
        val entries = mutableListOf<Entry>()
        transactions.forEach { t ->
            val date = t.occurredAt.atZone(zone).toLocalDate()
            when {
                t.type == TransactionType.TRANSFER -> {
                    // S15-DOM05: the principal of an installment and an extra payment is an expense of the loans' category
                    if (t.paymentKind in
                        LOAN_KINDS
                    ) {
                        entries += Entry(date, TransactionType.EXPENSE, t.principal ?: t.amount, loanCategoryId)
                    }
                    val charges = (t.interest ?: Money.ZERO) + (t.fees ?: Money.ZERO)
                    if (charges > Money.ZERO) {
                        entries += Entry(date, TransactionType.EXPENSE, charges, financialCharges, card = t.destinationAccountId)
                    }
                }
                // A deferred purchase counts through its installments
                t.installments > 1 -> Unit
                else -> entries += Entry(date, t.type, t.amount, t.categoryId)
            }
        }
        val purchaseById = purchases.associateBy { it.id }
        val deferredById = deferred.associateBy { it.id }
        installments.forEach { i ->
            val purchase = deferredById[i.deferredPurchaseId]?.let { purchaseById[it.purchaseTransactionId] } ?: return@forEach
            // A payoff cancels the future installments: their principal was paid that day (REQ-TC-12)
            val date =
                when (i.status) {
                    InstallmentStatus.CANCELLED -> i.paidAt?.atZone(zone)?.toLocalDate() ?: return@forEach
                    else -> i.dueDate.takeIf { it <= today } ?: return@forEach
                }
            entries += Entry(date, TransactionType.EXPENSE, i.principal, purchase.categoryId)
        }
        val counted = entries.filter { e -> e.categoryId?.let { byId[it] }?.excludedFromReports(byId) != true }

        val evolution =
            evolution(span, period).map { p ->
                val inside = counted.filter { it.date in p }
                PeriodSums(p, inside.sum(TransactionType.INCOME), inside.sum(TransactionType.EXPENSE))
            }
        val current = counted.filter { it.date in period }
        val count =
            transactions.count { t ->
                t.occurredAt.atZone(zone).toLocalDate() in period &&
                    t.categoryId?.let { byId[it] }?.excludedFromReports(byId) != true
            }
        val previous = evolution.getOrNull(evolution.size - 2)?.period
        val pace =
            if (span == ReportSpan.MONTH &&
                previous != null &&
                today >= period.from
            ) {
                pace(counted, period, previous, today)
            } else {
                null
            }
        val until = minOf(today, period.to)
        val sameDay = previous?.let { minOf(it.from.plusDays((until.dayOfMonth - 1).toLong()), it.to) }
        val changes =
            if (span == ReportSpan.MONTH && previous != null && sameDay != null) {
                val now = byCategory(current.filter { it.type == TransactionType.EXPENSE && it.date <= until }, byId)
                val before =
                    byCategory(counted.filter { it.type == TransactionType.EXPENSE && it.date in previous && it.date <= sameDay }, byId)
                val mains = (now.map { it.category } + before.map { it.category }).distinctBy { it.id }
                mains
                    .map { c ->
                        CategoryChange(
                            c,
                            now.firstOrNull { it.category.id == c.id }?.total ?: Money.ZERO,
                            before.firstOrNull { it.category.id == c.id }?.total ?: Money.ZERO,
                        )
                    }.filter { it.delta != Money.ZERO }
                    .sortedByDescending { kotlin.math.abs(it.delta.scaled) }
            } else {
                emptyList()
            }
        return Report(
            pace = pace,
            changes = changes,
            closed = today > period.to,
            period = period,
            income = current.sum(TransactionType.INCOME),
            expense = current.sum(TransactionType.EXPENSE),
            evolution = evolution,
            expenseByCategory = byCategory(current.filter { it.type == TransactionType.EXPENSE }, byId),
            incomeByCategory = byCategory(current.filter { it.type == TransactionType.INCOME }, byId),
            interestByCard =
                current
                    .filter { it.card != null }
                    .groupBy { it.card!! }
                    .mapValues { (_, list) -> list.fold(Money.ZERO) { s, e -> s + e.amount } },
            count = count,
        )
    }

    private fun pace(
        counted: List<Entry>,
        period: ReportPeriod,
        previous: ReportPeriod,
        today: LocalDate,
    ): Pace {
        fun cumulative(
            p: ReportPeriod,
            days: Int,
        ): List<Money> {
            val byDay = counted.filter { it.type == TransactionType.EXPENSE && it.date in p }.groupBy { it.date.dayOfMonth }
            var sum = Money.ZERO
            return (1..days).map { d ->
                sum += byDay[d]?.sumAll() ?: Money.ZERO
                sum
            }
        }
        val day = (if (today > period.to) period.to else today).dayOfMonth
        val current = cumulative(period, day)
        val before = cumulative(previous, previous.days.toInt())
        val spent = current.last()
        val match = before.indexOfFirst { it >= spent }.takeIf { it >= 0 && spent > Money.ZERO }?.plus(1)
        return Pace(day, current, before, match)
    }

    private fun byCategory(
        entries: List<Entry>,
        byId: Map<UUID, Category>,
    ): List<CategorySum> {
        val sums = entries.filter { it.categoryId != null }.groupBy { it.categoryId!! }.mapValues { (_, l) -> l.sumAll() }
        return sums.keys
            .mapNotNull { byId[it] }
            .groupBy { it.parentId?.let(byId::get) ?: it }
            .map { (main, members) ->
                val children =
                    members
                        .filter { it.id != main.id }
                        .map {
                            it to
                                sums.getValue(
                                    it.id,
                                )
                        }.sortedByDescending { it.second.scaled }
                CategorySum(main, children.fold(sums[main.id] ?: Money.ZERO) { s, it -> s + it.second }, children)
            }.sortedByDescending { it.total.scaled }
    }

    private data class Entry(
        val date: LocalDate,
        val type: TransactionType,
        val amount: Money,
        val categoryId: UUID?,
        val card: UUID? = null,
    )

    private fun List<Entry>.sum(type: TransactionType) = filter { it.type == type }.sumAll()

    private fun List<Entry>.sumAll() = fold(Money.ZERO) { s, e -> s + e.amount }

    private const val FINANCIAL_CHARGES = "FINANCIAL_CHARGES"
    private val LOAN_KINDS = setOf("LOAN_INSTALL", "LOAN_EXTRA")
}
