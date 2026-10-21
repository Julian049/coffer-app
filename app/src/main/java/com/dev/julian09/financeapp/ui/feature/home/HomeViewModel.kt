package com.dev.julian09.financeapp.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.TagRepository
import com.dev.julian09.financeapp.domain.usecase.HomeDashboard
import com.dev.julian09.financeapp.domain.usecase.ObserveHomeDashboard
import com.dev.julian09.financeapp.ui.feature.history.HistoryDay
import com.dev.julian09.financeapp.ui.feature.history.buildDays
import com.dev.julian09.financeapp.ui.feature.tags.TagLine
import com.dev.julian09.financeapp.ui.feature.tags.tagLines
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

data class HomeUiState(
    val dashboard: HomeDashboard,
    val days: List<HistoryDay>,
    /** REQ-ETQ-04: pinned tags with their month total. */
    val pinned: List<TagLine> = emptyList(),
)

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        observeHomeDashboard: ObserveHomeDashboard,
        categories: CategoryRepository,
        tags: TagRepository,
        clock: Clock,
    ) : ViewModel() {
        val state: StateFlow<HomeUiState?> =
            combine(
                observeHomeDashboard(),
                categories.observeAll(),
                tagLines(tags, YearMonth.from(clock.today()), clock.zone()),
            ) { dashboard, cats, lines ->
                HomeUiState(
                    dashboard,
                    buildDays(dashboard.recent, dashboard.accounts.associate { it.account.id to it.account.name }, cats),
                    lines.filter { it.tag.pinned },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }
