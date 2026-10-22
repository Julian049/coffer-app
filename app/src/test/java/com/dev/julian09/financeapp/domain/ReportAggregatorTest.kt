package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.finance.ReportAggregator
import com.dev.julian09.financeapp.domain.finance.ReportPeriod
import com.dev.julian09.financeapp.domain.finance.ReportSpan
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.DeferredPurchase
import com.dev.julian09.financeapp.domain.model.Installment
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/** S9-QA01: detailed-design §6.9.15. */
class ReportAggregatorTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 20)
    private val cash = UUID.randomUUID()
    private val card = UUID.randomUUID()
    private val food = Category(UUID.randomUUID(), "Comida", CategoryKind.EXPENSE)
    private val restaurants = Category(UUID.randomUUID(), "Restaurantes", CategoryKind.EXPENSE, parentId = food.id)
    private val salary = Category(UUID.randomUUID(), "Salario", CategoryKind.INCOME)
    private val savings = Category(UUID.randomUUID(), "Ahorros", CategoryKind.EXPENSE, excludeFromReports = true)
    private val pocket = Category(UUID.randomUUID(), "Bolsillo", CategoryKind.EXPENSE, parentId = savings.id)
    private val charges =
        Category(UUID.randomUUID(), "Intereses y cargos financieros", CategoryKind.EXPENSE, systemKey = "FINANCIAL_CHARGES")
    private val categories = listOf(food, restaurants, salary, savings, pocket, charges)
    private val october = ReportAggregator.period(ReportSpan.MONTH, today)

    private fun tx(
        type: TransactionType,
        pesos: Long,
        date: String,
        category: Category? = null,
        account: UUID = cash,
        destination: UUID? = null,
        installments: Int = 1,
        interest: Long? = null,
    ) = Transaction(
        id = UUID.randomUUID(),
        type = type,
        amount = Money.ofPesos(pesos),
        occurredAt = LocalDate.parse(date).atTime(12, 0).toInstant(zone),
        accountId = account,
        destinationAccountId = destination,
        categoryId = category?.id,
        installments = installments,
        interest = interest?.let(Money::ofPesos),
    )

    private fun report(
        transactions: List<Transaction>,
        deferred: List<DeferredPurchase> = emptyList(),
        installments: List<Installment> = emptyList(),
        span: ReportSpan = ReportSpan.MONTH,
        period: ReportPeriod = october,
    ) = ReportAggregator.aggregate(
        span,
        period,
        today,
        zone,
        transactions,
        categories,
        deferred,
        installments,
        transactions.filter { it.installments > 1 },
    )

    @Test
    fun transfersAreNotIncomeNorExpenseButCountAsActivity() {
        val r =
            report(
                listOf(
                    tx(TransactionType.INCOME, 3_000_000, "2026-10-01", salary),
                    tx(TransactionType.EXPENSE, 50_000, "2026-10-02", restaurants),
                    tx(TransactionType.EXPENSE, 30_000, "2026-10-03", food),
                    tx(TransactionType.TRANSFER, 200_000, "2026-10-04", destination = card),
                    tx(TransactionType.EXPENSE, 99_000, "2026-09-30", food),
                ),
            )
        assertEquals(Money.ofPesos(3_000_000), r.income)
        assertEquals(Money.ofPesos(80_000), r.expense)
        assertEquals(Money.ofPesos(2_920_000), r.balance)
        assertEquals(4, r.count)
        // Subcategories add to their main category
        val main = r.expenseByCategory.single()
        assertEquals(food, main.category)
        assertEquals(Money.ofPesos(80_000), main.total)
        assertEquals(listOf(restaurants to Money.ofPesos(50_000)), main.children)
    }

    @Test
    fun aDeferredPurchaseCountsByInstallmentAndInterestByCard() {
        val purchase = tx(TransactionType.EXPENSE, 300_000, "2026-08-10", food, account = card, installments = 3)
        val deferred =
            DeferredPurchase(
                UUID.randomUUID(),
                card,
                purchase.id,
                purchase.amount,
                3,
                BigDecimal("0.02"),
                RateType.MV,
                false,
                LocalDate.of(2026, 9, 5),
            )
        val plan =
            listOf("2026-09-05", "2026-10-05", "2026-11-05").mapIndexed { i, due ->
                Installment(UUID.randomUUID(), deferred.id, i + 1, LocalDate.parse(due), Money.ofPesos(100_000), Money.ofPesos(6_000))
            }
        val payment = tx(TransactionType.TRANSFER, 112_000, "2026-10-05", destination = card, interest = 12_000)
        // REQ-TC-09: a refund on the card is income (PO, 2026-10-20)
        val refund = tx(TransactionType.INCOME, 20_000, "2026-10-06", salary, account = card)
        val r = report(listOf(purchase, payment, refund), listOf(deferred), plan)
        // Only October's installment, plus the interest of the payment
        assertEquals(Money.ofPesos(112_000), r.expense)
        assertEquals(Money.ofPesos(20_000), r.income)
        assertEquals(mapOf(card to Money.ofPesos(12_000)), r.interestByCard)
        assertEquals(listOf(food, charges), r.expenseByCategory.map { it.category })
        // The November installment is future (REQ-REP-03); the September one is in the evolution
        assertEquals(Money.ofPesos(100_000), r.evolution[4].expense)
    }

    @Test
    fun aPayoffCountsTheCancelledInstallmentsOnTheDayOfThePayment() {
        val purchase = tx(TransactionType.EXPENSE, 200_000, "2026-09-10", food, account = card, installments = 2)
        val deferred =
            DeferredPurchase(
                UUID.randomUUID(),
                card,
                purchase.id,
                purchase.amount,
                2,
                BigDecimal("0.02"),
                RateType.MV,
                false,
                LocalDate.of(2026, 11, 5),
            )
        val paidAt = LocalDate.of(2026, 10, 15).atTime(9, 0).toInstant(zone)
        val plan =
            (1..2).map {
                Installment(
                    UUID.randomUUID(),
                    deferred.id,
                    it,
                    LocalDate.of(2026, 10 + it, 5),
                    Money.ofPesos(100_000),
                    Money.ZERO,
                    status = InstallmentStatus.CANCELLED,
                    paidAt = paidAt,
                )
            }
        assertEquals(Money.ofPesos(200_000), report(listOf(purchase), listOf(deferred), plan).expense)
    }

    @Test
    fun anExcludedCategoryAndItsSubcategoriesAreLeftOutOfEverything() {
        val r =
            report(
                listOf(
                    tx(TransactionType.EXPENSE, 50_000, "2026-10-02", food),
                    tx(TransactionType.EXPENSE, 500_000, "2026-10-02", savings),
                    tx(TransactionType.EXPENSE, 300_000, "2026-10-03", pocket),
                ),
            )
        assertEquals(Money.ofPesos(50_000), r.expense)
        assertEquals(listOf(food), r.expenseByCategory.map { it.category })
        assertEquals(1, r.count)
        assertEquals(Money.ofPesos(50_000), r.evolution.last().expense)
    }

    @Test
    fun weeksGoFromMondayToSundayAndTheEvolutionEndsWithThePeriod() {
        val week = ReportAggregator.period(ReportSpan.WEEK, today)
        assertEquals(ReportPeriod(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 25)), week)
        val months = ReportAggregator.evolution(ReportSpan.MONTH, october)
        assertEquals(6, months.size)
        assertEquals(LocalDate.of(2026, 5, 1), months.first().from)
        assertEquals(october, months.last())
        val range = ReportAggregator.period(ReportSpan.RANGE, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1))
        assertEquals(ReportPeriod(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10)), range)
        assertEquals(october, ReportAggregator.evolution(ReportSpan.RANGE, range).last())
    }

    @Test
    fun theChangeComparesExpensesWithThePreviousPeriod() {
        val r =
            report(
                listOf(
                    tx(TransactionType.EXPENSE, 100_000, "2026-09-10", food),
                    tx(TransactionType.EXPENSE, 103_000, "2026-10-10", food),
                ),
            )
        assertEquals(3, r.expenseChange)
        assertEquals(BigDecimal("0.0"), r.perDay)
        assertNull(report(listOf(tx(TransactionType.EXPENSE, 1_000, "2026-10-10", food))).expenseChange)
    }

    @Test
    fun thePaceComparesWhatWasSpentByTodayWithTheMonthBefore() {
        // September: 10.000 on the 5th and 20.000 on the 22nd; October, by the 20th: 25.000
        val r =
            report(
                listOf(
                    tx(TransactionType.EXPENSE, 10_000, "2026-09-05", restaurants),
                    tx(TransactionType.EXPENSE, 20_000, "2026-09-22", restaurants),
                    tx(TransactionType.EXPENSE, 25_000, "2026-10-03", restaurants),
                ),
            )
        val pace = r.pace!!
        assertEquals(20, pace.today)
        assertEquals(Money.ofPesos(25_000), pace.spent)
        // September reached 25.000 on the 22nd: two days ahead
        assertEquals(22, pace.matchDay)
        assertEquals(2, pace.daysAhead)
        assertEquals(30, pace.previous.size)
    }

    @Test
    fun changesCompareEachMainCategoryToTheSameDayOfTheMonthBefore() {
        val r =
            report(
                listOf(
                    tx(TransactionType.EXPENSE, 10_000, "2026-09-05", restaurants),
                    // After the 20th of September: left out of the comparison
                    tx(TransactionType.EXPENSE, 90_000, "2026-09-25", restaurants),
                    tx(TransactionType.EXPENSE, 4_000, "2026-09-10", charges),
                    tx(TransactionType.EXPENSE, 12_000, "2026-10-04", restaurants),
                ),
            )
        val food = r.changes.first { it.category.id == food.id }
        assertEquals(Money.ofPesos(12_000), food.now)
        assertEquals(Money.ofPesos(10_000), food.before)
        assertEquals(20, food.percent)
        val interest = r.changes.first { it.category.id == charges.id }
        assertEquals(Money.ofPesos(-4_000), interest.delta)
        // Ordered by how much they changed: 4.000 down before 2.000 up
        assertEquals(
            charges.id,
            r.changes
                .first()
                .category.id,
        )
        // 12.000 against 14.000: down 14 %
        assertEquals(-14, r.changePercent)
    }

    @Test
    fun theSavingRateAndItsAverageComeFromTheClosedMonths() {
        val r =
            report(
                listOf(
                    tx(TransactionType.INCOME, 1_000_000, "2026-09-01", salary),
                    tx(TransactionType.EXPENSE, 700_000, "2026-09-02", restaurants),
                    tx(TransactionType.INCOME, 2_000_000, "2026-10-01", salary),
                    tx(TransactionType.EXPENSE, 1_160_000, "2026-10-02", restaurants),
                ),
            )
        assertEquals(42, r.savingsRate)
        // Only September closed with income: 300.000 of 1.000.000
        assertEquals(30, r.averageRate)
        assertEquals(false, r.closed)
    }
}
