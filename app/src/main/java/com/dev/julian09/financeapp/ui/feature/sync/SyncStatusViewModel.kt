package com.dev.julian09.financeapp.ui.feature.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.SyncStatus
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Feeds the SyncBadge of Home and Settings (S3-UI02); null until the first value arrives. */
@HiltViewModel
class SyncStatusViewModel
    @Inject
    constructor(
        scheduler: SyncScheduler,
    ) : ViewModel() {
        val status: StateFlow<SyncStatus?> = scheduler.observeStatus().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }
