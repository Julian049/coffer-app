package com.dev.julian09.financeapp.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.BuildConfig
import com.dev.julian09.financeapp.domain.usecase.CheckServerStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ServerState { Checking, Available, Unavailable }

data class SettingsUiState(
    val appVersion: String,
    val server: ServerState = ServerState.Checking,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val checkServerStatus: CheckServerStatus,
    ) : ViewModel() {
        private val _state = MutableStateFlow(SettingsUiState(appVersion = BuildConfig.VERSION_NAME))
        val state: StateFlow<SettingsUiState> = _state.asStateFlow()

        init {
            refreshServer()
        }

        fun refreshServer() {
            _state.value = _state.value.copy(server = ServerState.Checking)
            viewModelScope.launch {
                val available = checkServerStatus()
                _state.value = _state.value.copy(server = if (available) ServerState.Available else ServerState.Unavailable)
            }
        }
    }
