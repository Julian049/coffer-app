package com.dev.julian09.financeapp.domain.finance

import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/** «37,8 % del gasto» (REQ-REP-06): one decimal, half up; null when [whole] is zero. */
fun shareOf(
    part: Money,
    whole: Money,
): BigDecimal? =
    if (whole == Money.ZERO) {
        null
    } else {
        part.toBigDecimal().multiply(BigDecimal(100)).divide(whole.toBigDecimal().abs(), 1, RoundingMode.HALF_UP)
    }

/** One month of a category's bars (REQ-REP-07). */
data class CategoryMonth(
    val month: YearMonth,
    val spent: Money,
)

/**
 * Detalle de categoría (REQ-REP-07, roadmap-v2.0 §8): this month, the month before, the average of the months that had
 * data, the share of the month's spending and the subcategories.
 */
data class CategoryDetail(
    val category: Category,
    /** Oldest first, ending with the month shown. */
    val months: List<CategoryMonth>,
    val subcategories: List<Pair<Category, Money>>,
    /** Whole spending of the month, every category, for [weight]. */
    val monthExpense: Money,
) {
    val spent: Money get() = months.last().spent
    val previous: Money? get() = months.getOrNull(months.size - 2)?.spent

    /** Over the months since the first movement only, so a new user is not compared with empty months. */
    val average: Money? get() = averageOf(months.dropLast(1).map { it.spent })

    val vsPrevious: Money? get() = previous?.let { spent - it }
    val vsAverage: Money? get() = average?.let { spent - it }
    val weight: BigDecimal? get() = shareOf(spent, monthExpense)

    companion object {
        private fun averageOf(values: List<Money>): Money? =
            values.takeIf { it.isNotEmpty() }?.let { list ->
                Money.of(list.fold(BigDecimal.ZERO) { s, m -> s + m.toBigDecimal() }.divide(BigDecimal(list.size), 0, RoundingMode.HALF_UP))
            }
    }
}

object ReportDetails {
    /**
     * [reports] are monthly, oldest first, ending with the month shown; months before [firstMovement] are left out of the
     * bars' average (roadmap-v2.0 §4.16). Null when the category is not a main one with spending in those months.
     */
    fun category(
        categoryId: UUID,
        reports: List<Report>,
        firstMovement: LocalDate?,
    ): CategoryDetail? {
        val current = reports.lastOrNull() ?: return null
        val sums = reports.map { r -> r to r.expenseByCategory.firstOrNull { it.category.id == categoryId } }
        val category = sums.firstNotNullOfOrNull { it.second?.category } ?: return null
        val months =
            sums
                .filter { (r, _) -> firstMovement == null || !r.period.to.isBefore(firstMovement) }
                .map { (r, sum) -> CategoryMonth(YearMonth.from(r.period.from), sum?.total ?: Money.ZERO) }
                .ifEmpty { listOf(CategoryMonth(YearMonth.from(current.period.from), Money.ZERO)) }
        val subcategories =
            sums
                .last()
                .second
                ?.children
                .orEmpty()
        return CategoryDetail(category, months, subcategories, current.expense)
    }
}

/**
 * Filtros del resumen (REQ-REP-08): empty sets mean any; [minAmount] and [maxAmount] are both included. A category
 * filter takes in its subcategories, as in Historial.
 */
data class ReportFilter(
    val types: Set<TransactionType> = emptySet(),
    val categoryIds: Set<UUID> = emptySet(),
    val accountIds: Set<UUID> = emptySet(),
    val tagIds: Set<UUID> = emptySet(),
    val minAmount: Money? = null,
    val maxAmount: Money? = null,
) {
    /** How many filters are set, for «Filtros · 2». */
    val count: Int
        get() =
            listOf(
                types.isNotEmpty(),
                categoryIds.isNotEmpty(),
                accountIds.isNotEmpty(),
                tagIds.isNotEmpty(),
                minAmount != null || maxAmount != null,
            ).count { it }

    fun matches(
        t: Transaction,
        categories: Map<UUID, Category>,
    ): Boolean {
        val category = t.categoryId?.let(categories::get)
        return (types.isEmpty() || t.type in types) &&
            (categoryIds.isEmpty() || t.categoryId in categoryIds || category?.parentId in categoryIds) &&
            (accountIds.isEmpty() || t.accountId in accountIds || t.destinationAccountId in accountIds) &&
            (tagIds.isEmpty() || t.tagIds.any { it in tagIds }) &&
            (minAmount == null || t.amount >= minAmount) &&
            (maxAmount == null || t.amount <= maxAmount)
    }
}
