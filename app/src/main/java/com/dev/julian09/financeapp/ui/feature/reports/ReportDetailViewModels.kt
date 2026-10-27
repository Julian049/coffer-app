package com.dev.julian09.financeapp.ui.feature.reports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.finance.CategoryDetail
import com.dev.julian09.financeapp.domain.finance.ReportAggregator
import com.dev.julian09.financeapp.domain.finance.ReportFilter
import com.dev.julian09.financeapp.domain.finance.ReportPeriod
import com.dev.julian09.financeapp.domain.finance.ReportSpan
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CalendarAccountsStore
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.TagRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import com.dev.julian09.financeapp.domain.usecase.DayBalance
import com.dev.julian09.financeapp.domain.usecase.ObserveCategoryDetail
import com.dev.julian09.financeapp.domain.usecase.ObserveDailyBalances
import com.dev.julian09.financeapp.domain.usecase.ObserveSummary
import com.dev.julian09.financeapp.domain.usecase.Summary
import com.dev.julian09.financeapp.domain.usecase.defaultCalendarAccounts
import com.dev.julian09.financeapp.ui.feature.history.HistoryDay
import com.dev.julian09.financeapp.ui.feature.history.buildDays
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

/** Rows of a list of movements, with names and tag dots, the same way in every report. */
private fun rows(
    transactions: TransactionRepository,
    accounts: AccountRepository,
    categories: CategoryRepository,
    tags: TagRepository,
    clock: Clock,
    from: LocalDate,
    to: LocalDate,
    keep: (com.dev.julian09.financeapp.domain.model.Transaction) -> Boolean,
): Flow<List<HistoryDay>> {
    val zone = clock.zone()
    return combine(
        transactions.observeBetween(from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant()),
        accounts.observeBalances(),
        categories.observeAll(),
        tags.observeAll(),
    ) { txs, accs, cats, all ->
        buildDays(
            txs.filter(keep).sortedByDescending { it.occurredAt },
            accs.associate { it.account.id to it.account.name },
            cats,
            zone,
            all,
        )
    }
}

data class CategoriesReportState(
    val period: ReportPeriod,
    val income: Boolean,
    val summary: Summary,
)

/** Reportes · Gastos por categoría (Figma `168:187`). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoriesReportViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        observeSummary: ObserveSummary,
    ) : ViewModel() {
        private val period =
            ReportPeriod(LocalDate.parse(savedState.get<String>("from")), LocalDate.parse(savedState.get<String>("to")))
        private val income = MutableStateFlow(false)

        val state: StateFlow<CategoriesReportState?> =
            combine(observeSummary(period), income) { s, i -> CategoriesReportState(period, i, s) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        fun showIncome(on: Boolean) = income.update { on }
    }

data class CategoryReportState(
    val detail: CategoryDetail?,
    val month: ReportPeriod,
    /** Every movement of the category in the month, newest first; the screen shows the last ones. */
    val days: List<HistoryDay>,
    val today: LocalDate,
) {
    val count: Int get() = days.sumOf { it.rows.size }
}

/** Reportes · Detalle de categoría (Figma `168:301`). */
@HiltViewModel
class CategoryReportViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        observeDetail: ObserveCategoryDetail,
        transactions: TransactionRepository,
        accounts: AccountRepository,
        categories: CategoryRepository,
        tags: TagRepository,
        clock: Clock,
    ) : ViewModel() {
        val categoryId: UUID = UUID.fromString(savedState.get<String>("categoryId"))
        val month: ReportPeriod = ReportAggregator.period(ReportSpan.MONTH, LocalDate.parse(savedState.get<String>("month")))

        val state: StateFlow<CategoryReportState?> =
            combine(
                observeDetail(categoryId, month.from),
                categories.observeAll().map { all ->
                    all.filter { it.id == categoryId || it.parentId == categoryId }.map(Category::id).toSet()
                },
                rows(transactions, accounts, categories, tags, clock, month.from, month.to) { true },
            ) { detail, ids, days ->
                // Only this category and its subcategories
                val mine =
                    days.mapNotNull { d ->
                        d.rows
                            .filter { it.categoryId in ids }
                            .takeIf { it.isNotEmpty() }
                            ?.let { HistoryDay(d.date, it) }
                    }
                CategoryReportState(detail, month, mine, clock.today())
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

/** Hoja · Filtros del resumen, Periodo (Figma `169:462`); the default is the last six months, as the bars. */
enum class SummaryRange(
    val label: String,
) {
    THIS_MONTH("Este mes"),
    LAST_3("Últimos 3 meses"),
    LAST_6("Últimos 6 meses"),
    THIS_YEAR("Este año"),
    CUSTOM("Rango personalizado"),
    ;

    fun period(
        today: LocalDate,
        custom: ReportPeriod?,
    ): ReportPeriod =
        when (this) {
            THIS_MONTH -> ReportPeriod(today.withDayOfMonth(1), today)
            LAST_3 -> ReportPeriod(today.withDayOfMonth(1).minusMonths(2), today)
            LAST_6 -> ReportPeriod(today.withDayOfMonth(1).minusMonths(5), today)
            THIS_YEAR -> ReportPeriod(today.withDayOfYear(1), today)
            CUSTOM -> custom ?: LAST_6.period(today, null)
        }
}

data class SummaryFilters(
    val range: SummaryRange = SummaryRange.LAST_6,
    val custom: ReportPeriod? = null,
    val filter: ReportFilter = ReportFilter(),
) {
    /** «Filtros · n»: the ones not at their default (changes-ui-2.0 §3). */
    val count: Int get() = filter.count + if (range != SummaryRange.LAST_6) 1 else 0
}

data class SummaryChoices(
    val categories: List<Category> = emptyList(),
    val accounts: List<AccountBalance> = emptyList(),
    val tags: List<Tag> = emptyList(),
)

data class SummaryState(
    val filters: SummaryFilters,
    val period: ReportPeriod,
    /** The tapped month (Figma `169:326`), or null for the whole period. */
    val focus: YearMonth?,
    val summary: Summary,
    val choices: SummaryChoices,
    val today: LocalDate,
)

/** Reportes · Resumen general (Figma `169:193`): every number of the screen follows the same filters and month. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SummaryViewModel
    @Inject
    constructor(
        observeSummary: ObserveSummary,
        categories: CategoryRepository,
        accounts: AccountRepository,
        tags: TagRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val filters = MutableStateFlow(SummaryFilters())
        private val focus = MutableStateFlow<YearMonth?>(null)

        val state: StateFlow<SummaryState?> =
            combine(
                combine(filters, focus) { f, m -> f to m }.flatMapLatest { (f, m) ->
                    val period = f.range.period(clock.today(), f.custom)
                    val month = m?.let { ReportAggregator.months(period).firstOrNull { p -> YearMonth.from(p.from) == it } }
                    observeSummary(period, f.filter, month).map { Triple(f, m, it) to period }
                },
                combine(categories.observeAll(), accounts.observeBalances(), tags.observeAll()) { c, a, t ->
                    SummaryChoices(c, a.filter { it.account.active }, t)
                },
            ) { (fms, period), choices ->
                val (f, m, s) = fms
                SummaryState(f, period, m, s, choices, clock.today())
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        fun apply(new: SummaryFilters) {
            filters.value = new
            focus.value = null
        }

        /** Tapping the same month again goes back to the whole period. */
        fun toggleMonth(month: YearMonth) = focus.update { if (it == month) null else month }
    }

data class CalendarState(
    val month: YearMonth,
    val days: List<DayBalance>,
    val selected: LocalDate?,
    val selectedDay: HistoryDay?,
    val accounts: List<AccountBalance>,
    val chosen: Set<UUID>,
    val today: LocalDate,
)

/** Reportes · Calendario (Figma `171:186`): the balance at the end of each day of the chosen accounts. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel
    @Inject
    constructor(
        private val dailyBalances: ObserveDailyBalances,
        private val store: CalendarAccountsStore,
        transactions: TransactionRepository,
        private val accounts: AccountRepository,
        categories: CategoryRepository,
        tags: TagRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val month = MutableStateFlow(YearMonth.from(clock.today()))
        private val selected = MutableStateFlow<LocalDate?>(clock.today())

        private val chosen: Flow<Pair<List<AccountBalance>, Set<UUID>>> =
            combine(accounts.observeBalances(), store.observe()) { all, saved ->
                val shown = all.filter { it.account.active }
                shown to (saved ?: defaultCalendarAccounts(shown.map { it.account }))
            }

        val state: StateFlow<CalendarState?> =
            combine(
                // Any change of the ledger moves the balances, so they are recomputed with it
                combine(month, chosen) { m, c -> m to c }.mapLatest { (m, c) -> Triple(m, c, dailyBalances(m, c.second)) },
                selected.flatMapLatest { day ->
                    if (day == null) {
                        kotlinx.coroutines.flow.flowOf(emptyList())
                    } else {
                        chosen.flatMapLatest { (_, ids) ->
                            rows(transactions, accounts, categories, tags, clock, day, day) {
                                it.accountId in ids || it.destinationAccountId in ids
                            }
                        }
                    }
                },
                selected,
            ) { (m, c, days), rows, day ->
                CalendarState(m, days, day, rows.firstOrNull(), c.first, c.second, clock.today())
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        fun move(step: Long) {
            month.update { it.plusMonths(step) }
            selected.value = null
        }

        fun select(day: LocalDate) {
            selected.value = day
        }

        fun choose(ids: Set<UUID>) {
            viewModelScope.launch { store.save(ids) }
        }
    }
