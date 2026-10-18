package com.dev.julian09.financeapp.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.usecase.HomeDashboard
import com.dev.julian09.financeapp.domain.usecase.ObserveHomeDashboard
import com.dev.julian09.financeapp.ui.feature.history.HistoryDay
import com.dev.julian09.financeapp.ui.feature.history.buildDays
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val dashboard: HomeDashboard,
    val days: List<HistoryDay>,
)

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        observeHomeDashboard: ObserveHomeDashboard,
        categories: CategoryRepository,
    ) : ViewModel() {
        val state: StateFlow<HomeUiState?> =
            combine(observeHomeDashboard(), categories.observeAll()) { dashboard, cats ->
                HomeUiState(dashboard, buildDays(dashboard.recent, dashboard.accounts.associate { it.account.id to it.account.name }, cats))
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }
