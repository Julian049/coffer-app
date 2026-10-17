package com.dev.julian09.financeapp.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.BuildConfig
import com.dev.julian09.financeapp.domain.model.SessionState
import com.dev.julian09.financeapp.domain.model.UserProfile
import com.dev.julian09.financeapp.domain.usecase.CheckServerStatus
import com.dev.julian09.financeapp.domain.usecase.Logout
import com.dev.julian09.financeapp.domain.usecase.ObserveSession
import com.dev.julian09.financeapp.domain.usecase.RefreshProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ServerState { Checking, Available, Unavailable }

data class SettingsUiState(
    val appVersion: String,
    val server: ServerState = ServerState.Checking,
    val user: UserProfile? = null,
    val confirmingLogout: Boolean = false,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val checkServerStatus: CheckServerStatus,
        observeSession: ObserveSession,
        private val refreshProfile: RefreshProfile,
        private val logout: Logout,
    ) : ViewModel() {
        private val _state = MutableStateFlow(SettingsUiState(appVersion = BuildConfig.VERSION_NAME))
        val state: StateFlow<SettingsUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                observeSession().collect { session ->
                    val user = (session as? SessionState.Active)?.user ?: (session as? SessionState.Expired)?.user
                    _state.update { it.copy(user = user) }
                }
            }
            // The stored profile shows at once; GET /me only refreshes it when online
            viewModelScope.launch { refreshProfile() }
            refreshServer()
        }

        fun refreshServer() {
            _state.update { it.copy(server = ServerState.Checking) }
            viewModelScope.launch {
                val available = checkServerStatus()
                _state.update { it.copy(server = if (available) ServerState.Available else ServerState.Unavailable) }
            }
        }

        fun requestLogout() = _state.update { it.copy(confirmingLogout = true) }

        fun dismissLogout() = _state.update { it.copy(confirmingLogout = false) }

        /** REQ-AUT-06: local data stays; the root switches to sign-in when the session disappears. */
        fun confirmLogout() {
            _state.update { it.copy(confirmingLogout = false) }
            viewModelScope.launch { logout() }
        }
    }
