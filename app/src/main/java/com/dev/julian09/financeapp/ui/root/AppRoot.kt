package com.dev.julian09.financeapp.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.SessionState
import com.dev.julian09.financeapp.ui.navigation.AppNavHost
import com.dev.julian09.financeapp.ui.navigation.AuthNavHost
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/**
 * Start according to the session (S2-UI02): signed out → sign in; active → Home; expired → sign in again
 * keeping all local data. Sprint 3 inserts the initial download after the first sign-in.
 */
@Composable
fun AppRoot(viewModel: SessionViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    when (val state = session) {
        SessionState.Loading -> Box(Modifier.fillMaxSize().background(CofferTheme.colors.background))
        SessionState.SignedOut -> AuthNavHost(expiredUser = null)
        is SessionState.Expired -> AuthNavHost(expiredUser = state.user.username)
        is SessionState.Active -> AppNavHost(user = state.user)
    }
}
