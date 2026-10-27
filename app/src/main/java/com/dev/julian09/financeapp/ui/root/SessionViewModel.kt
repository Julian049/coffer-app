package com.dev.julian09.financeapp.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.SessionState
import com.dev.julian09.financeapp.domain.port.PushTokens
import com.dev.julian09.financeapp.domain.port.ReminderScheduler
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import com.dev.julian09.financeapp.domain.usecase.ObserveNeedsLegalConsent
import com.dev.julian09.financeapp.domain.usecase.ObserveSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SessionViewModel
    @Inject
    constructor(
        observeSession: ObserveSession,
        observeNeedsLegalConsent: ObserveNeedsLegalConsent,
        private val scheduler: SyncScheduler,
        private val reminders: ReminderScheduler,
        private val pushTokens: PushTokens,
    ) : ViewModel() {
        val session: StateFlow<SessionState> =
            observeSession().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)

        /** True sends an active session to Términos y privacidad before anything else; null while unknown. */
        val needsLegalConsent: StateFlow<Boolean?> =
            observeSession()
                .flatMapLatest { s -> if (s is SessionState.Active) observeNeedsLegalConsent(s.user.id) else flowOf(false) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        /** Null while unknown; false sends an active session to Descargando tus datos (S3-UI01). */
        val initialSyncDone: StateFlow<Boolean?> =
            scheduler.observeInitialSyncDone().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        init {
            // Every start with a session and a finished download: sync now and keep the periodic work (REQ-SYN-01)
            combine(observeSession(), scheduler.observeInitialSyncDone()) { s, done -> s is SessionState.Active && done }
                .distinctUntilChanged()
                .onEach { ready ->
                    if (ready) {
                        scheduler.ensurePeriodic()
                        scheduler.requestSync()
                        reminders.ensureScheduled()
                        viewModelScope.launch { pushTokens.register() }
                    }
                }.launchIn(viewModelScope)
        }
    }
