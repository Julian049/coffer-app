package com.dev.julian09.financeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.dev.julian09.financeapp.domain.finance.ReportAggregator
import com.dev.julian09.financeapp.domain.finance.ReportDetails
import com.dev.julian09.financeapp.domain.finance.ReportPeriod
import com.dev.julian09.financeapp.domain.finance.ReportSpan
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountFlows
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.DayBalance
import com.dev.julian09.financeapp.domain.usecase.Summary
import com.dev.julian09.financeapp.ui.feature.history.buildDays
import com.dev.julian09.financeapp.ui.feature.reports.CalendarActions
import com.dev.julian09.financeapp.ui.feature.reports.CalendarContent
import com.dev.julian09.financeapp.ui.feature.reports.CalendarState
import com.dev.julian09.financeapp.ui.feature.reports.CategoriesReportContent
import com.dev.julian09.financeapp.ui.feature.reports.CategoriesReportState
import com.dev.julian09.financeapp.ui.feature.reports.CategoryReportContent
import com.dev.julian09.financeapp.ui.feature.reports.CategoryReportState
import com.dev.julian09.financeapp.ui.feature.reports.SummaryChoices
import com.dev.julian09.financeapp.ui.feature.reports.SummaryContent
import com.dev.julian09.financeapp.ui.feature.reports.SummaryFilters
import com.dev.julian09.financeapp.ui.feature.reports.SummaryState
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

/** Reportes detallados of 2.0 (changes-ui-2.0 §3) with Figma's sample data. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h1400dp-xxhdpi")
class ReportDetailsScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 18)
    private val zone = ZoneOffset.UTC
    private val food = Category(UUID(1, 1), "Comida", CategoryKind.EXPENSE, isBase = true, icon = "food")
    private val restaurants =
        Category(UUID(1, 2), "Restaurantes", CategoryKind.EXPENSE, parentId = food.id, isBase = true, icon = "restaurant")
    private val home = Category(UUID(1, 3), "Hogar", CategoryKind.EXPENSE, isBase = true, icon = "home")
    private val transport = Category(UUID(1, 4), "Transporte", CategoryKind.EXPENSE, isBase = true, icon = "bus")
    private val entertainment = Category(UUID(1, 5), "Entretenimiento", CategoryKind.EXPENSE, isBase = true, icon = "movie")
    private val health = Category(UUID(1, 6), "Salud", CategoryKind.EXPENSE, isBase = true, icon = "health")
    private val pets = Category(UUID(1, 7), "Mascotas", CategoryKind.EXPENSE, icon = "pets")
    private val salary = Category(UUID(1, 8), "Salario", CategoryKind.INCOME, isBase = true, icon = "work")
    private val categories = listOf(food, restaurants, home, transport, entertainment, health, pets, salary)
    private val cash = UUID(2, 1)
    private val trip = Tag(UUID(3, 1), "Viaje Cartagena", color = "#4F6D8A")

    private val rows =
        (0L..5L)
            .flatMap { back ->
                val month = today.minusMonths(back)
                listOf(
                    tx(TransactionType.INCOME, 4_200_000 - back * 150_000, month.withDayOfMonth(1), salary),
                    tx(TransactionType.EXPENSE, 1_100_000, month.withDayOfMonth(2), home),
                    tx(TransactionType.EXPENSE, 300_000 + back * 25_000, month.withDayOfMonth(5), food),
                    tx(TransactionType.EXPENSE, 112_400, month.withDayOfMonth(12), restaurants, tags = listOf(trip.id)),
                    tx(TransactionType.EXPENSE, 210_000, month.withDayOfMonth(8), transport),
                    tx(TransactionType.EXPENSE, 145_000, month.withDayOfMonth(15), entertainment),
                    tx(TransactionType.EXPENSE, 80_000, month.withDayOfMonth(9), health),
                    tx(TransactionType.EXPENSE, 65_000, month.withDayOfMonth(11), pets),
                )
            }.filter { it.occurredAt.atZone(zone).toLocalDate() <= today }

    private fun tx(
        type: TransactionType,
        pesos: Long,
        date: LocalDate,
        category: Category,
        tags: List<UUID> = emptyList(),
    ) = Transaction(
        UUID.randomUUID(),
        type,
        Money.ofPesos(pesos),
        date.atTime(12, 0).toInstant(zone),
        cash,
        categoryId = category.id,
        tagIds = tags,
    )

    private fun aggregate(
        period: ReportPeriod,
        span: ReportSpan = ReportSpan.RANGE,
        periods: List<ReportPeriod> = ReportAggregator.evolution(span, period),
    ) = ReportAggregator.aggregate(span, period, today, zone, rows, categories, emptyList(), emptyList(), emptyList(), periods = periods)

    private fun summary(focus: ReportPeriod? = null): Summary {
        val period = ReportPeriod(today.withDayOfMonth(1).minusMonths(5), today)
        val whole = aggregate(period, periods = ReportAggregator.months(period))
        val shown = focus?.let { aggregate(it, periods = listOf(it)) } ?: whole
        return Summary(whole.evolution, shown, mapOf(TransactionType.EXPENSE to 142, TransactionType.INCOME to 6))
    }

    @Test
    fun categoriesLight() = capture("report_categories_light", dark = false) { Categories() }

    @Test
    fun categoriesDark() = capture("report_categories_dark", dark = true) { Categories() }

    @Test
    fun categoryDetailLight() = capture("report_category_light", dark = false) { CategoryDetail() }

    @Test
    fun summaryLight() = capture("report_summary_light", dark = false) { Summary(null) }

    @Test
    fun summaryAugustLight() = capture("report_summary_august_light", dark = false) { Summary(YearMonth.of(2026, 8)) }

    @Test
    fun calendarLight() = capture("report_calendar_light", dark = false) { Calendar() }

    @Test
    fun calendarDark() = capture("report_calendar_dark", dark = true) { Calendar() }

    private fun capture(
        name: String,
        dark: Boolean,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            CofferTheme(darkTheme = dark) {
                Column(Modifier.background(CofferTheme.colors.background)) { content() }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Composable
    private fun Categories() {
        val month = ReportAggregator.period(ReportSpan.MONTH, today)
        CategoriesReportContent(CategoriesReportState(month, false, summary(month)), {}, {}, {})
    }

    @Composable
    private fun CategoryDetail() {
        val month = ReportAggregator.period(ReportSpan.MONTH, today)
        val reports = ReportAggregator.evolution(ReportSpan.MONTH, month).map { aggregate(it, ReportSpan.MONTH) }
        val days =
            buildDays(
                rows.filter { it.categoryId == food.id || it.categoryId == restaurants.id }.sortedByDescending { it.occurredAt },
                mapOf(cash to "Efectivo"),
                categories,
                zone,
                listOf(trip),
            )
        CategoryReportContent(
            CategoryReportState(ReportDetails.category(food.id, reports, today.minusMonths(5)), month, days, today),
            {},
            {},
            {},
        )
    }

    @Composable
    private fun Summary(focus: YearMonth?) {
        val period = ReportPeriod(today.withDayOfMonth(1).minusMonths(5), today)
        val month = focus?.let { ReportAggregator.months(period).first { p -> YearMonth.from(p.from) == it } }
        SummaryContent(
            SummaryState(SummaryFilters(), period, focus, summary(month), SummaryChoices(categories, emptyList(), listOf(trip)), today),
            {},
            {},
            {},
            {},
        )
    }

    @Composable
    private fun Calendar() {
        val month = YearMonth.from(today)
        var balance = Money.ofPesos(2_100_000)
        val days =
            (1..today.dayOfMonth).map { d ->
                val change =
                    if (d % 4 == 0) {
                        Money.ofPesos(-120_000)
                    } else if (d == 1) {
                        Money.ofPesos(900_000)
                    } else {
                        Money.ZERO
                    }
                balance += change
                DayBalance(month.atDay(d), balance, change)
            }
        val account = Account(cash, "Efectivo", AccountType.CASH, Money.ZERO)
        val visa = Account(UUID(2, 2), "Visa", AccountType.CREDIT_CARD, Money.ZERO)
        val selected = today
        val day =
            buildDays(
                rows.filter {
                    it.occurredAt.atZone(zone).toLocalDate() == selected.minusDays(6)
                },
                mapOf(cash to "Efectivo"),
                categories,
                zone,
                listOf(trip),
            )
        CalendarContent(
            CalendarState(
                month,
                days,
                selected,
                day.firstOrNull(),
                listOf(AccountBalance(account, AccountFlows()), AccountBalance(visa, AccountFlows())),
                setOf(cash),
                today,
            ),
            CalendarActions(),
        )
    }
}
