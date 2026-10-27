package com.dev.julian09.financeapp.ui.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.CardDue
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.DuePayment
import com.dev.julian09.financeapp.domain.model.LoanDue
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RecurringOverview
import com.dev.julian09.financeapp.domain.model.RecurringSummary
import com.dev.julian09.financeapp.domain.model.withDues
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.usecase.ObserveCardDues
import com.dev.julian09.financeapp.domain.usecase.ObserveLoanDues
import com.dev.julian09.financeapp.domain.usecase.ObserveRecurringOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject

enum class RecurringFilter { ALL, UPCOMING, OVERDUE, PAID }

data class RecurringUiState(
    val overview: RecurringOverview,
    val categories: List<Category>,
    val month: YearMonth,
    val filter: RecurringFilter = RecurringFilter.ALL,
    /** Card statements to pay (S7) and loan installments (S13, #131), listed and counted with the recurring expenses. */
    val cards: List<CardDue> = emptyList(),
    val loans: List<LoanDue> = emptyList(),
) {
    val dues: List<DuePayment> get() = (cards + loans).filter { it.amount > Money.ZERO }.sortedBy { it.dueDate }

    val summary: RecurringSummary get() = overview.summary.withDues(dues, month.atEndOfMonth())
}

@HiltViewModel
class RecurringViewModel
    @Inject
    constructor(
        observeOverview: ObserveRecurringOverview,
        categories: CategoryRepository,
        cardDues: ObserveCardDues,
        loanDues: ObserveLoanDues,
        clock: Clock,
    ) : ViewModel() {
        private val filter = MutableStateFlow(RecurringFilter.ALL)
        private val month = YearMonth.from(clock.today())

        val state: StateFlow<RecurringUiState?> =
            combine(observeOverview(), categories.observeAll(), filter, cardDues(), loanDues()) { overview, cats, f, cards, loans ->
                RecurringUiState(overview, cats, month, f, cards, loans)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        fun setFilter(value: RecurringFilter) = filter.update { value }
    }
