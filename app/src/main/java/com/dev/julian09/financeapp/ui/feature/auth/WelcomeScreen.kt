package com.dev.julian09.financeapp.ui.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.R
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/** Bienvenida (S15-UI06): the first screen without a session; «Comenzar» creates an account. */
@Composable
fun WelcomeScreen(
    onStart: () -> Unit,
    onLogin: () -> Unit,
    googleViewModel: GoogleSignInViewModel = hiltViewModel(),
) {
    val google by googleViewModel.state.collectAsStateWithLifecycle()
    WelcomeContent(onStart, onLogin, onGoogle = rememberGoogleSignIn(googleViewModel), google = google)
}

@Composable
fun WelcomeContent(
    onStart: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    onGoogle: (() -> Unit)? = null,
    google: GoogleSignInState = GoogleSignInState(),
) {
    val colors = CofferTheme.colors
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Image(
            painterResource(if (isSystemInDarkTheme()) R.drawable.coffer_mark_dark else R.drawable.coffer_mark),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
        Text("Coffer", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Image(painterResource(R.drawable.mascot_welcome), contentDescription = null, modifier = Modifier.size(220.dp))
        AppButton("Comenzar", onClick = onStart, modifier = Modifier.fillMaxWidth())
        AuthAlternatives("o continúa con", "¿Ya tienes una cuenta?", "Inicia sesión", onLogin, onGoogle, google)
    }
}
