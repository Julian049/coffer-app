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
) {
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
     * happened, because their installments count on each due date (REQ-TC-09).
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
    ): Report {
        val byId = categories.associateBy { it.id }
        val financialCharges = categories.firstOrNull { it.systemKey == FINANCIAL_CHARGES }?.id
        val entries = mutableListOf<Entry>()
        transactions.forEach { t ->
            val date = t.occurredAt.atZone(zone).toLocalDate()
            when {
                t.type == TransactionType.TRANSFER -> {
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
        return Report(
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
}
