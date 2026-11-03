package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.finance.Report
import com.dev.julian09.financeapp.domain.finance.ReportAggregator
import com.dev.julian09.financeapp.domain.finance.ReportPeriod
import com.dev.julian09.financeapp.domain.finance.ReportSpan
import com.dev.julian09.financeapp.domain.model.DeferredPurchase
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.CardRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject

/**
 * Reportes' «Cuentas ▾» and «Movimientos ▾» (NAV-APP-008): which accounts and which kind of movement the whole report
 * counts. Empty accounts and a null type mean everything. A deferred purchase counts through its card and is an expense.
 */
data class ReportFilter(
    val accountIds: Set<UUID> = emptySet(),
    val type: TransactionType? = null,
) {
    fun keeps(t: Transaction): Boolean =
        (type == null || t.type == type) &&
            (accountIds.isEmpty() || t.accountId in accountIds || t.destinationAccountId in accountIds)

    fun deferred(plans: List<DeferredPurchase>): List<DeferredPurchase> =
        if (type != null && type != TransactionType.EXPENSE) {
            emptyList()
        } else {
            plans.filter { accountIds.isEmpty() || it.cardAccountId in accountIds }
        }
}

/** S9-DOM01: the report of [period] with its evolution, recalculated on every change of the ledger. */
class ObserveReport
    @Inject
    constructor(
        private val transactions: TransactionRepository,
        private val categories: CategoryRepository,
        private val cards: CardRepository,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) {
        operator fun invoke(
            span: ReportSpan,
            period: ReportPeriod,
            filter: ReportFilter = ReportFilter(),
        ): Flow<Report> {
            val zone = clock.zone()
            val first = minOf(period.from, ReportAggregator.evolution(span, period).first().from)
            val end = maxOf(period.to, ReportAggregator.evolution(span, period).last().to).plusDays(1)
            return combine(
                transactions.observeBetween(first.atStartOfDay(zone).toInstant(), end.atStartOfDay(zone).toInstant()),
                categories.observeAll(),
                cards.observeDeferred(),
                combine(cards.observeInstallments(), settings.observe()) { installments, s -> installments to s.loanPaymentCategoryId },
                transactions.observeDeferredPurchases(),
            ) { rows, all, deferred, (installments, loanCategory), purchases ->
                val plans = filter.deferred(deferred)
                val planIds = plans.mapTo(HashSet()) { it.id }
                ReportAggregator.aggregate(
                    span,
                    period,
                    clock.today(),
                    zone,
                    rows.filter(filter::keeps),
                    all,
                    plans,
                    installments.filter { it.deferredPurchaseId in planIds },
                    purchases.filter(filter::keeps),
                    loanCategory,
                )
            }
        }
    }
