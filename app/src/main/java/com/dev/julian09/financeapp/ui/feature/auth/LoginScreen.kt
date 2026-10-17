package com.dev.julian09.financeapp.ui.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.R
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme

@Composable
fun LoginScreen(
    onRegister: () -> Unit,
    expiredUser: String?,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    LaunchedEffect(expiredUser) { expiredUser?.let(viewModel::prefill) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginContent(state, sessionExpired = expiredUser != null, viewModel::onIdentifier, viewModel::onPassword, viewModel::submit, onRegister)
}

/** Iniciar Sesión (Figma 1:1911). "¿Olvidaste tu contraseña?" is out of scope for v1.0 (08-ui README §3). */
@Composable
fun LoginContent(
    state: LoginUiState,
    sessionExpired: Boolean,
    onIdentifier: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSubmit: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(72.dp))
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painterResource(if (isSystemInDarkTheme()) R.drawable.coffer_mark_dark else R.drawable.coffer_mark),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
            )
            Text("Coffer", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        }
        Spacer(Modifier.height(24.dp))
        if (sessionExpired) {
            Surface(shape = CofferShapes.card, color = colors.background, border = BorderStroke(1.dp, colors.warning)) {
                Text(
                    "Tu sesión terminó. Inicia sesión de nuevo; tus datos siguen en este teléfono.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
        AppTextField(
            value = state.identifier,
            onValueChange = onIdentifier,
            label = "Correo electrónico o usuario",
            placeholder = "ejemplo@coffer.app",
            leadingIcon = Icons.Outlined.AlternateEmail,
            error = state.errors[AuthField.Identifier],
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            autofill = ContentType.Username,
        )
        AppTextField(
            value = state.password,
            onValueChange = onPassword,
            label = "Contraseña",
            leadingIcon = Icons.Outlined.Lock,
            error = state.errors[AuthField.Password],
            password = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            autofill = ContentType.Password,
        )
        FormError(state.formError)
        AppButton(
            text = if (state.loading) "Entrando…" else "Iniciar sesión",
            onClick = onSubmit,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        )
        AuthAlternatives(separator = "o continúa con", question = "¿No tienes una cuenta?", link = "Regístrate", onLink = onRegister)
    }
}
