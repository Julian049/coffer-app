package com.dev.julian09.financeapp.ui.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.finance.Report
import com.dev.julian09.financeapp.domain.finance.ReportAggregator
import com.dev.julian09.financeapp.domain.finance.ReportPeriod
import com.dev.julian09.financeapp.domain.finance.ReportSpan
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.TagRepository
import com.dev.julian09.financeapp.domain.usecase.ObserveReport
import com.dev.julian09.financeapp.ui.feature.tags.TagLine
import com.dev.julian09.financeapp.ui.feature.tags.tagLines
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

data class ReportsState(
    val span: ReportSpan,
    val period: ReportPeriod,
    val report: Report,
    /** Card names for "Intereses y cargos financieros". */
    val accounts: Map<UUID, String>,
    /** Por etiqueta (S13): the tags with movements in the month, the largest first. */
    val tags: List<TagLine> = emptyList(),
    /** From the Clock, so «Del 1 al 21 · hoy» is the same in the screenshots on any day. */
    val today: LocalDate = LocalDate.now(),
)

private data class Selection(
    val span: ReportSpan,
    val period: ReportPeriod,
)

/** Reportes (S9-UI01): the current month by default. */
@HiltViewModel
class ReportsViewModel
    @Inject
    constructor(
        private val observeReport: ObserveReport,
        accounts: AccountRepository,
        private val tags: TagRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val today = clock.today()
        private val selection = MutableStateFlow(Selection(ReportSpan.MONTH, ReportAggregator.period(ReportSpan.MONTH, today)))

        @OptIn(ExperimentalCoroutinesApi::class)
        val state: StateFlow<ReportsState?> =
            combine(
                selection.flatMapLatest { s -> observeReport(s.span, s.period).map { s to it } },
                accounts.observeBalances().map { list -> list.associate { it.account.id to it.account.name } },
                selection.flatMapLatest { s -> tagLines(tags, YearMonth.from(s.period.from), clock.zone()) },
            ) { (s, report), names, lines ->
                val shown = if (s.span == ReportSpan.MONTH) lines.filter { it.count > 0 }.sortedBy { it.net.scaled } else emptyList()
                ReportsState(s.span, s.period, report, names, shown, clock.today())
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        fun selectSpan(span: ReportSpan) =
            selection.update {
                val period =
                    if (span == ReportSpan.RANGE) {
                        ReportAggregator.period(ReportSpan.MONTH, today)
                    } else {
                        ReportAggregator.period(span, minOf(it.period.to, today))
                    }
                Selection(span, period)
            }

        /** Previous (-1) or next (+1) week or month. */
        fun move(step: Long) =
            selection.update {
                val anchor =
                    when (it.span) {
                        ReportSpan.WEEK -> it.period.from.plusWeeks(step)
                        ReportSpan.MONTH -> it.period.from.plusMonths(step)
                        ReportSpan.RANGE -> return@update it
                    }
                it.copy(period = ReportAggregator.period(it.span, anchor))
            }

        fun setRange(
            from: LocalDate = selection.value.period.from,
            to: LocalDate = selection.value.period.to,
        ) = selection.update { Selection(ReportSpan.RANGE, ReportAggregator.period(ReportSpan.RANGE, from, to)) }
    }
