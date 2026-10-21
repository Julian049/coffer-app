package com.dev.julian09.financeapp.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.HomeCard
import com.dev.julian09.financeapp.domain.model.homeLayout
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.usecase.CustomizeHome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class CustomizeHomeState(
    val cards: List<HomeCard> = emptyList(),
    /** Active accounts in carousel order (M9). */
    val accounts: List<Account> = emptyList(),
    val loaded: Boolean = false,
)

/** [from] moved to [to]; out of range leaves the list as it is. */
fun <T> List<T>.moved(
    from: Int,
    to: Int,
): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

/** Personalizar inicio (S11-UI04, UI05): each change is saved as it happens. */
@HiltViewModel
class CustomizeHomeViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val accounts: AccountRepository,
        private val customize: CustomizeHome,
    ) : ViewModel() {
        private val _state = MutableStateFlow(CustomizeHomeState())
        val state: StateFlow<CustomizeHomeState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                val cards = homeLayout(settings.observe().first().homeCards)
                _state.value = CustomizeHomeState(cards, accounts.observeActive().first(), loaded = true)
            }
        }

        private val writing = Mutex()

        fun moveCard(
            from: Int,
            to: Int,
        ) = change { it.copy(cards = it.cards.moved(from, to)) }

        fun setVisible(
            key: String,
            visible: Boolean,
        ) = change { s -> s.copy(cards = s.cards.map { if (it.key == key) it.copy(visible = visible) else it }) }

        fun moveAccount(
            from: Int,
            to: Int,
        ) = change { it.copy(accounts = it.accounts.moved(from, to)) }

        /**
         * Every change saves itself (no Guardar). Writes run one at a time and each takes the latest state,
         * so the last one always wins; NonCancellable lets a write started just before leaving the screen finish.
         */
        private fun change(edit: (CustomizeHomeState) -> CustomizeHomeState) {
            _state.update(edit)
            viewModelScope.launch(NonCancellable) {
                writing.withLock {
                    val s = _state.value
                    customize(s.cards, s.accounts.map { it.id })
                }
            }
        }
    }
