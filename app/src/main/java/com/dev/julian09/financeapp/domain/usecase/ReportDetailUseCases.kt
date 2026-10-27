package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.finance.CategoryDetail
import com.dev.julian09.financeapp.domain.finance.PeriodSums
import com.dev.julian09.financeapp.domain.finance.Report
import com.dev.julian09.financeapp.domain.finance.ReportAggregator
import com.dev.julian09.financeapp.domain.finance.ReportDetails
import com.dev.julian09.financeapp.domain.finance.ReportFilter
import com.dev.julian09.financeapp.domain.finance.ReportPeriod
import com.dev.julian09.financeapp.domain.finance.ReportSpan
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.DeferredPurchase
import com.dev.julian09.financeapp.domain.model.Installment
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.CardRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/** What [ReportAggregator.aggregate] reads, for a stretch of days. */
internal data class Ledger(
    val rows: List<Transaction>,
    val categories: List<Category>,
    val deferred: List<DeferredPurchase>,
    val installments: List<Installment>,
    val purchases: List<Transaction>,
    val loanCategoryId: UUID?,
    val firstMovement: LocalDate?,
)

/** The same sources as [ObserveReport], read once for the detail screens of 2.0. */
class ReportLedger
    @Inject
    constructor(
        private val transactions: TransactionRepository,
        private val categories: CategoryRepository,
        private val cards: CardRepository,
        private val settings: SettingsRepository,
        val clock: Clock,
    ) {
        internal fun observe(
            from: LocalDate,
            to: LocalDate,
        ): Flow<Ledger> {
            val zone = clock.zone()
            return combine(
                transactions.observeBetween(from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant()),
                categories.observeAll(),
                combine(cards.observeDeferred(), cards.observeInstallments()) { d, i -> d to i },
                combine(transactions.observeDeferredPurchases(), settings.observe()) { p, s -> p to s.loanPaymentCategoryId },
                transactions.observeFirstOccurredAt(),
            ) { rows, all, (deferred, installments), (purchases, loanCategory), first ->
                Ledger(rows, all, deferred, installments, purchases, loanCategory, first?.atZone(zone)?.toLocalDate())
            }
        }

        internal fun aggregate(
            ledger: Ledger,
            period: ReportPeriod,
            span: ReportSpan = ReportSpan.RANGE,
            periods: List<ReportPeriod> = ReportAggregator.evolution(span, period),
            filter: ReportFilter = ReportFilter(),
        ): Report {
            val byId = ledger.categories.associateBy { it.id }
            return ReportAggregator.aggregate(
                span,
                period,
                clock.today(),
                clock.zone(),
                ledger.rows.filter { filter.matches(it, byId) },
                ledger.categories,
                ledger.deferred,
                ledger.installments,
                ledger.purchases.filter { filter.matches(it, byId) },
                ledger.loanCategoryId,
                periods,
            )
        }
    }

/** Resumen general (REQ-REP-08): one bar per month of the period, and the totals of the tapped month or the whole period. */
data class Summary(
    val months: List<PeriodSums>,
    val shown: Report,
    /** «142 movimientos» of each type in [shown]'s period, after the filters. */
    val counts: Map<TransactionType, Int>,
)

class ObserveSummary
    @Inject
    constructor(
        private val ledger: ReportLedger,
    ) {
        /** [focus] is a month of [period] (Figma's Agosto state); null shows the whole period. */
        operator fun invoke(
            period: ReportPeriod,
            filter: ReportFilter = ReportFilter(),
            focus: ReportPeriod? = null,
        ): Flow<Summary> =
            ledger.observe(period.from, period.to).map { l ->
                val whole = ledger.aggregate(l, period, periods = ReportAggregator.months(period), filter = filter)
                val shown = focus?.let { ledger.aggregate(l, it, periods = listOf(it), filter = filter) } ?: whole
                val zone: ZoneId = ledger.clock.zone()
                val byId = l.categories.associateBy { it.id }
                val counts =
                    l.rows
                        .filter { it.occurredAt.atZone(zone).toLocalDate() in shown.period && filter.matches(it, byId) }
                        .groupingBy { it.type }
                        .eachCount()
                Summary(whole.evolution, shown, counts)
            }
    }

/** Detalle de categoría (REQ-REP-07): the month of [anchor] and the five before it. */
class ObserveCategoryDetail
    @Inject
    constructor(
        private val ledger: ReportLedger,
    ) {
        operator fun invoke(
            categoryId: UUID,
            anchor: LocalDate,
        ): Flow<CategoryDetail?> {
            val month = ReportAggregator.period(ReportSpan.MONTH, anchor)
            val months = ReportAggregator.evolution(ReportSpan.MONTH, month)
            // One month more, so the oldest bar's month before is there for its to-date comparison
            return ledger.observe(months.first().from.minusMonths(1), month.to).map { l ->
                val reports = months.map { ledger.aggregate(l, it, span = ReportSpan.MONTH) }
                ReportDetails.category(categoryId, reports, l.firstMovement)
            }
        }
    }
