package com.dev.julian09.financeapp.ui.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.CardDue
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.RecurringOverview
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.usecase.ObserveCardDues
import com.dev.julian09.financeapp.domain.usecase.ObserveRecurringOverview
import com.dev.julian09.financeapp.domain.usecase.SkipPending
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

enum class RecurringFilter { ALL, UPCOMING, OVERDUE, PAID }

data class RecurringUiState(
    val overview: RecurringOverview,
    val categories: List<Category>,
    val month: YearMonth,
    val filter: RecurringFilter = RecurringFilter.ALL,
    /** Card statements to pay (S7): listed with the recurring expenses, not part of their summary. */
    val cards: List<CardDue> = emptyList(),
)

@HiltViewModel
class RecurringViewModel
    @Inject
    constructor(
        observeOverview: ObserveRecurringOverview,
        categories: CategoryRepository,
        cardDues: ObserveCardDues,
        private val skipPending: SkipPending,
        clock: Clock,
    ) : ViewModel() {
        private val filter = MutableStateFlow(RecurringFilter.ALL)
        private val month = YearMonth.from(clock.today())

        val state: StateFlow<RecurringUiState?> =
            combine(observeOverview(), categories.observeAll(), filter, cardDues()) { overview, cats, f, cards ->
                RecurringUiState(overview, cats, month, f, cards)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        fun setFilter(value: RecurringFilter) = filter.update { value }

        fun skip(id: UUID) {
            viewModelScope.launch { skipPending(id) }
        }
    }
