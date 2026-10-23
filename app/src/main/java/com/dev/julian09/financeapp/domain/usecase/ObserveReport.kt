package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.finance.Report
import com.dev.julian09.financeapp.domain.finance.ReportAggregator
import com.dev.julian09.financeapp.domain.finance.ReportPeriod
import com.dev.julian09.financeapp.domain.finance.ReportSpan
import com.dev.julian09.financeapp.domain.port.CardRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

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
                ReportAggregator.aggregate(span, period, clock.today(), zone, rows, all, deferred, installments, purchases, loanCategory)
            }
        }
    }
