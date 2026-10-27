package com.dev.julian09.financeapp.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.NetBalanceMode
import com.dev.julian09.financeapp.domain.model.Settings
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.DraftRepository
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.port.TagRepository
import com.dev.julian09.financeapp.domain.usecase.HomeDashboard
import com.dev.julian09.financeapp.domain.usecase.ObserveHomeDashboard
import com.dev.julian09.financeapp.domain.usecase.SetIncludeInNet
import com.dev.julian09.financeapp.domain.usecase.UpdateSettings
import com.dev.julian09.financeapp.ui.feature.history.HistoryDay
import com.dev.julian09.financeapp.ui.feature.history.buildDays
import com.dev.julian09.financeapp.ui.feature.tags.TagLine
import com.dev.julian09.financeapp.ui.feature.tags.tagLines
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

/** user_settings.home_balance_view (REQ-CTA-09). */
enum class BalanceView { AVAILABLE, NET }

data class HomeUiState(
    val dashboard: HomeDashboard,
    val days: List<HistoryDay>,
    /** REQ-ETQ-04: pinned tags with their month total. */
    val pinned: List<TagLine> = emptyList(),
    val settings: Settings = Settings(),
    /** REQ-COR-04: transactions to review; Home only tells how many (changes-ui-2.0 §1). */
    val draftCount: Int = 0,
) {
    val balanceView: BalanceView get() = if (settings.homeBalanceView == BalanceView.NET.name) BalanceView.NET else BalanceView.AVAILABLE

    /** The net balance by the chosen mode (S12-H03). */
    val net get() = dashboard.net(settings.netBalanceMode)
}

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        observeHomeDashboard: ObserveHomeDashboard,
        categories: CategoryRepository,
        tags: TagRepository,
        clock: Clock,
        settings: SettingsRepository,
        private val updateSettings: UpdateSettings,
        private val setIncludeInNet: SetIncludeInNet,
        drafts: DraftRepository,
    ) : ViewModel() {
        val state: StateFlow<HomeUiState?> =
            combine(
                observeHomeDashboard(),
                categories.observeAll(),
                tagLines(tags, YearMonth.from(clock.today()), clock.zone()),
                settings.observe(),
                drafts.observeAll(),
            ) { dashboard, cats, lines, stored, pending ->
                HomeUiState(
                    dashboard,
                    buildDays(
                        dashboard.recent,
                        dashboard.accounts.associate { it.account.id to it.account.name },
                        cats,
                        clock.zone(),
                        lines.map { it.tag },
                    ),
                    lines.filter { it.tag.pinned },
                    stored,
                    pending.size,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        /** Hoja · Detalle del saldo: which view Home shows (REQ-CTA-09). */
        fun setBalanceView(view: BalanceView) {
            viewModelScope.launch { updateSettings { it.copy(homeBalanceView = view.name) } }
        }

        /** Hoja · Detalle del saldo, «Elegir deudas»: marks or unmarks a debt (S15-UI11). */
        fun setIncludeInNet(
            accountId: UUID,
            include: Boolean,
        ) {
            viewModelScope.launch { setIncludeInNet.invoke(accountId, include) }
        }

        /** Hoja · Detalle del saldo: what the net balance subtracts (S12-H03). */
        fun setNetMode(mode: NetBalanceMode) {
            viewModelScope.launch { updateSettings { it.copy(netBalanceMode = mode) } }
        }
    }
