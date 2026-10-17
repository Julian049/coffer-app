package com.dev.julian09.financeapp.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.SessionState
import com.dev.julian09.financeapp.domain.usecase.ObserveSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SessionViewModel
    @Inject
    constructor(
        observeSession: ObserveSession,
    ) : ViewModel() {
        val session: StateFlow<SessionState> =
            observeSession().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)
    }
