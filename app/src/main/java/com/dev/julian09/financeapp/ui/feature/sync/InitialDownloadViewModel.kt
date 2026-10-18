package com.dev.julian09.financeapp.ui.feature.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.InitialSyncProgress
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** SEQ-02: starts the initial download and follows its progress; the root leaves this screen when it finishes. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InitialDownloadViewModel
    @Inject
    constructor(
        private val scheduler: SyncScheduler,
    ) : ViewModel() {
        private val work = MutableStateFlow(scheduler.requestInitialSync())

        val progress: StateFlow<InitialSyncProgress> =
            work
                .flatMapLatest(scheduler::observeInitialSync)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InitialSyncProgress())

        /** After a permanent failure; the download resumes from the saved cursor. */
        fun retry() {
            work.value = scheduler.requestInitialSync()
        }
    }
