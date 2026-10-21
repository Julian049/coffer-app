package com.dev.julian09.financeapp.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.Settings
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.usecase.UpdateSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Ajustes → Avisos (S6-UI03). */
@HiltViewModel
class ReminderSettingsViewModel
    @Inject
    constructor(
        settings: SettingsRepository,
        private val update: UpdateSettings,
    ) : ViewModel() {
        val settings: StateFlow<Settings?> = settings.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        fun setNotifications(enabled: Boolean) {
            viewModelScope.launch { update { it.copy(notificationsEnabled = enabled) } }
        }

        fun setLeadDays(days: Int) {
            viewModelScope.launch { update { it.copy(reminderLeadDays = days) } }
        }
    }
