package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.SearchTotals
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionFilter
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class SearchResult(
    val transactions: List<Transaction>,
    val totals: SearchTotals,
) {
    /** More rows exist than were loaded: the list asks for the next window when it reaches the end. */
    val hasMore: Boolean get() = transactions.size < totals.count
}

/** S5-DOM02: filters and live search combined; choosing a category includes its subcategories (REQ-TRX-09). */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchTransactions
    @Inject
    constructor(
        private val transactions: TransactionRepository,
        private val categories: CategoryRepository,
    ) {
        operator fun invoke(
            filter: TransactionFilter,
            limit: Int,
        ): Flow<SearchResult> =
            categories
                .observeAll()
                .map { all ->
                    if (filter.categoryIds.isEmpty()) {
                        filter
                    } else {
                        filter.copy(categoryIds = filter.categoryIds + all.filter { it.parentId in filter.categoryIds }.map { it.id })
                    }
                }.flatMapLatest { expanded ->
                    combine(transactions.search(expanded, limit), transactions.searchTotals(expanded), ::SearchResult)
                }
    }
