package com.dev.julian09.financeapp.ui.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.SearchTotals
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.model.TransactionFilter
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.TagRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import com.dev.julian09.financeapp.domain.usecase.SearchTransactions
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
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/** What the filter sheet edits; [from] and [to] (inclusive days) replace the selected month when both are set. */
data class HistoryFilters(
    val types: Set<TransactionType> = emptySet(),
    val categoryIds: Set<UUID> = emptySet(),
    val accountIds: Set<UUID> = emptySet(),
    val tagIds: Set<UUID> = emptySet(),
    val from: LocalDate? = null,
    val to: LocalDate? = null,
) {
    val customRange: Boolean get() = from != null && to != null

    /** Filters besides the type chips, for the badge on the filter button. */
    val count: Int get() = categoryIds.size + accountIds.size + tagIds.size + if (customRange) 1 else 0
}

data class HistoryChoices(
    val categories: List<Category> = emptyList(),
    val accounts: List<AccountBalance> = emptyList(),
    val tags: List<Tag> = emptyList(),
)

data class HistoryUiState(
    val month: YearMonth,
    val months: List<YearMonth>,
    val query: String,
    val filters: HistoryFilters,
    val days: List<HistoryDay>,
    val totals: SearchTotals,
    val hasMore: Boolean,
    val choices: HistoryChoices,
)

private data class Inputs(
    val month: YearMonth,
    val query: String,
    val filters: HistoryFilters,
    val limit: Int,
)

/** Historial (S5-H02): month selector, live search, type chips and combinable filters. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val search: SearchTransactions,
        transactions: TransactionRepository,
        accounts: AccountRepository,
        categories: CategoryRepository,
        tags: TagRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val inputs = MutableStateFlow(Inputs(YearMonth.from(clock.today()), "", HistoryFilters(), PAGE))

        private val choices =
            combine(categories.observeAll(), accounts.observeBalances(), tags.observeAll()) { c, a, t -> HistoryChoices(c, a, t) }

        private val months =
            transactions.observeFirstOccurredAt().map { first ->
                val now = YearMonth.from(clock.today())
                val oldest = first?.let { YearMonth.from(it.atZone(clock.zone())) }?.takeIf { it < now } ?: now
                generateSequence(now) { it.minusMonths(1) }.takeWhile { it >= oldest }.toList()
            }

        val state: StateFlow<HistoryUiState?> =
            combine(inputs.flatMapLatest { i -> search(filter(i), i.limit).map { i to it } }, choices, months) { (i, result), ch, ms ->
                HistoryUiState(
                    month = i.month,
                    months = ms,
                    query = i.query,
                    filters = i.filters,
                    days =
                        buildDays(
                            result.transactions,
                            ch.accounts.associate { it.account.id to it.account.name },
                            ch.categories,
                            clock.zone(),
                        ),
                    totals = result.totals,
                    hasMore = result.hasMore,
                    choices = ch,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private fun filter(i: Inputs): TransactionFilter {
            val zone: ZoneId = clock.zone()
            val f = i.filters
            val (from, to) = if (f.customRange) f.from!! to f.to!! else i.month.atDay(1) to i.month.atEndOfMonth()
            return TransactionFilter(
                from = from.atStartOfDay(zone).toInstant(),
                to = to.plusDays(1).atStartOfDay(zone).toInstant(),
                types = f.types,
                categoryIds = f.categoryIds,
                accountIds = f.accountIds,
                tagIds = f.tagIds,
                query = i.query,
            )
        }

        /** A month replaces a custom range. */
        fun selectMonth(month: YearMonth) =
            inputs.update { it.copy(month = month, filters = it.filters.copy(from = null, to = null), limit = PAGE) }

        fun setQuery(query: String) = inputs.update { it.copy(query = query, limit = PAGE) }

        /** The chips above the list: null is "Todos". */
        fun setType(type: TransactionType?) = inputs.update { it.copy(filters = it.filters.copy(types = setOfNotNull(type)), limit = PAGE) }

        fun applyFilters(filters: HistoryFilters) = inputs.update { it.copy(filters = filters, limit = PAGE) }

        fun loadMore() = inputs.update { it.copy(limit = it.limit + PAGE) }

        private companion object {
            const val PAGE = 150
        }
    }
