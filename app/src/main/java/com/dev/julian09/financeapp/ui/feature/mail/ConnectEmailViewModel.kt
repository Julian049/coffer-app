package com.dev.julian09.financeapp.ui.feature.mail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.AuthFailure
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.usecase.RefreshProfile
import com.dev.julian09.financeapp.domain.usecase.UpdateSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectEmailState(
    val loading: Boolean = true,
    /** cofferapp1+<code>@gmail.com; null without connection the first time. */
    val address: String? = null,
    /** Gmail's code to accept the forward, once it arrived. */
    val confirmation: String? = null,
    val notifyDrafts: Boolean = true,
    val offline: Boolean = false,
)

/** Conectar correo (REQ-COR-01): the forwarding address comes from GET /me, refreshed on every visit. */
@HiltViewModel
class ConnectEmailViewModel
    @Inject
    constructor(
        private val refreshProfile: RefreshProfile,
        settings: SettingsRepository,
        private val updateSettings: UpdateSettings,
    ) : ViewModel() {
        private val profile = MutableStateFlow(ConnectEmailState())

        val state: StateFlow<ConnectEmailState> =
            combine(profile, settings.observe()) { p, s -> p.copy(notifyDrafts = s.notifyDrafts) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectEmailState())

        init {
            refresh()
        }

        /** Also after creating the forward in Gmail, to see its confirmation code. */
        fun refresh() {
            profile.update { it.copy(loading = true) }
            viewModelScope.launch {
                val result = refreshProfile()
                val user = result.getOrNull()
                profile.update {
                    it.copy(
                        loading = false,
                        address = user?.forwardingAddress ?: it.address,
                        confirmation = user?.forwardingConfirmation ?: it.confirmation,
                        offline = result.exceptionOrNull() == AuthFailure.NoConnection,
                    )
                }
            }
        }

        fun setNotifyDrafts(enabled: Boolean) {
            viewModelScope.launch { updateSettings { it.copy(notifyDrafts = enabled) } }
        }
    }
