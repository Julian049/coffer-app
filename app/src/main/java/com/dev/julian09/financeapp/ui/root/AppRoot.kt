package com.dev.julian09.financeapp.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.SessionState
import com.dev.julian09.financeapp.ui.feature.legal.LegalConsentScreen
import com.dev.julian09.financeapp.ui.feature.sync.InitialDownloadScreen
import com.dev.julian09.financeapp.ui.navigation.AppNavHost
import com.dev.julian09.financeapp.ui.navigation.AuthNavHost
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/**
 * Start according to the session (S2-UI02): signed out → sign in; active → Home; expired → sign in again
 * keeping all local data. An active session that has not accepted the current terms goes to Términos y privacidad first (S14-H02). An active session whose data never finished downloading goes to Descargando tus datos.
 */
@Composable
fun AppRoot(viewModel: SessionViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val initialSyncDone by viewModel.initialSyncDone.collectAsStateWithLifecycle()
    val needsLegalConsent by viewModel.needsLegalConsent.collectAsStateWithLifecycle()
    val active = session is SessionState.Active
    val blank =
        session == SessionState.Loading ||
            (active && (needsLegalConsent == null || (needsLegalConsent == false && initialSyncDone == null)))
    // Inicio reports itself once its data is in (HomeScreen)
    val home = active && needsLegalConsent == false && initialSyncDone == true
    if (!blank && !home) SideEffect { FirstContent.shown = true }
    when (val state = session) {
        SessionState.Loading -> Blank()
        SessionState.SignedOut -> AuthNavHost(expiredUser = null)
        is SessionState.Expired -> AuthNavHost(expiredUser = state.user.username)
        is SessionState.Active ->
            when {
                needsLegalConsent == null -> Blank()
                needsLegalConsent == true -> LegalConsentScreen(userId = state.user.id)
                else ->
                    when (initialSyncDone) {
                        null -> Blank()
                        false -> InitialDownloadScreen()
                        true -> AppNavHost(user = state.user)
                    }
            }
    }
}

/** #223: MainActivity keeps the system splash on screen until the first real screen can draw its content. */
object FirstContent {
    @Volatile
    var shown = false
}

@Composable
private fun Blank() {
    Box(Modifier.fillMaxSize().background(CofferTheme.colors.background))
}
