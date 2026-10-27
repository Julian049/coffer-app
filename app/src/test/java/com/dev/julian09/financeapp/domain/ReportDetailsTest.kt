package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.finance.ReportAggregator
import com.dev.julian09.financeapp.domain.finance.ReportDetails
import com.dev.julian09.financeapp.domain.finance.ReportFilter
import com.dev.julian09.financeapp.domain.finance.ReportSpan
import com.dev.julian09.financeapp.domain.finance.shareOf
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/** S20 (REQ-REP-06 to 08, roadmap-v2.0 §8): the numbers behind the part-2 screens. */
class ReportDetailsTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 20)
    private val cash = UUID.randomUUID()
    private val visa = UUID.randomUUID()
    private val food = Category(UUID.randomUUID(), "Comida", CategoryKind.EXPENSE)
    private val cafes = Category(UUID.randomUUID(), "Cafeterías", CategoryKind.EXPENSE, parentId = food.id)
    private val home = Category(UUID.randomUUID(), "Hogar", CategoryKind.EXPENSE)
    private val categories = listOf(food, cafes, home)
    private val trip = UUID.randomUUID()

    private fun tx(
        pesos: Long,
        date: String,
        category: Category,
        account: UUID = cash,
        tags: List<UUID> = emptyList(),
    ) = Transaction(
        UUID.randomUUID(),
        TransactionType.EXPENSE,
        Money.ofPesos(pesos),
        LocalDate.parse(date).atTime(12, 0).toInstant(zone),
        account,
        categoryId = category.id,
        tagIds = tags,
    )

    private fun month(
        date: String,
        all: List<Transaction>,
    ) = ReportAggregator.period(ReportSpan.MONTH, LocalDate.parse(date)).let { p ->
        ReportAggregator.aggregate(ReportSpan.MONTH, p, today, zone, all, categories, emptyList(), emptyList(), emptyList())
    }

    private val all =
        listOf(
            tx(300_000, "2026-08-05", food),
            tx(200_000, "2026-09-05", food),
            tx(100_000, "2026-09-06", cafes),
            tx(250_000, "2026-10-03", food),
            tx(128_000, "2026-10-04", cafes),
            tx(622_000, "2026-10-10", home, account = visa, tags = listOf(trip)),
        )

    @Test
    fun theSharesOfTheMonthAddUpToItsTotal() {
        val october = month("2026-10-01", all)
        assertEquals(Money.ofPesos(1_000_000), october.expense)
        assertEquals(october.expense, october.expenseByCategory.fold(Money.ZERO) { s, c -> s + c.total })
        assertEquals(BigDecimal("62.2"), shareOf(Money.ofPesos(622_000), october.expense))
        assertEquals(BigDecimal("37.8"), shareOf(october.expenseByCategory.single { it.category == food }.total, october.expense))
        assertNull(shareOf(Money.ofPesos(1), Money.ZERO))
    }

    @Test
    fun aCategoryDetailComparesWithTheMonthBeforeAndTheAverageSinceTheFirstMovement() {
        val reports = (5L downTo 0L).map { back -> month(today.minusMonths(back).withDayOfMonth(1).toString(), all) }
        val detail = ReportDetails.category(food.id, reports, firstMovement = LocalDate.of(2026, 8, 5))!!

        assertEquals(Money.ofPesos(378_000), detail.spent)
        assertEquals(Money.ofPesos(300_000), detail.previous)
        // August and September only: May to July were before the first movement
        assertEquals(3, detail.months.size)
        assertEquals(Money.ofPesos(300_000), detail.average)
        assertEquals(Money.ofPesos(78_000), detail.vsAverage)
        assertEquals(BigDecimal("37.8"), detail.weight)
        assertEquals(listOf(cafes to Money.ofPesos(128_000)), detail.subcategories)
        assertNull(ReportDetails.category(UUID.randomUUID(), reports, null))
    }

    @Test
    fun theSummaryFiltersCombineAndACategoryTakesInItsSubcategories() {
        val byId = categories.associateBy { it.id }
        val onlyFood = ReportFilter(categoryIds = setOf(food.id))
        assertEquals(5, all.count { onlyFood.matches(it, byId) })
        val tripOnCard = ReportFilter(accountIds = setOf(visa), tagIds = setOf(trip), minAmount = Money.ofPesos(500_000))
        assertEquals(1, all.count { tripOnCard.matches(it, byId) })
        assertEquals(3, tripOnCard.count)
        assertFalse(ReportFilter(maxAmount = Money.ofPesos(100_000)).matches(all[0], byId))
        assertTrue(ReportFilter().matches(all[0], byId))
    }
}
