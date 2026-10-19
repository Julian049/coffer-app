package com.dev.julian09.financeapp.ui.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.theme.CofferTheme

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
    googleViewModel: GoogleSignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val google by googleViewModel.state.collectAsStateWithLifecycle()
    RegisterContent(
        state,
        viewModel::onChange,
        viewModel::submit,
        onBack,
        onGoogle = rememberGoogleSignIn(googleViewModel),
        google = google,
    )
}

/** Registro (Figma "Crear cuenta"). After success the root opens the app (S3 adds the initial download). */
@Composable
fun RegisterContent(
    state: RegisterUiState,
    onChange: (AuthField, String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onGoogle: (() -> Unit)? = null,
    google: GoogleSignInState = GoogleSignInState(),
) {
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Crear cuenta", onBack = onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Crea tu cuenta para respaldar tus finanzas en la nube y recuperarlas si cambias de teléfono.",
                style = MaterialTheme.typography.bodyMedium,
                color = CofferTheme.colors.textSecondary,
            )
            val next = KeyboardOptions(imeAction = ImeAction.Next)
            AppTextField(
                value = state.displayName,
                onValueChange = { onChange(AuthField.DisplayName, it) },
                label = "Nombre",
                leadingIcon = Icons.Outlined.Person,
                error = state.errors[AuthField.DisplayName],
                keyboardOptions = next.copy(capitalization = KeyboardCapitalization.Words),
                autofill = ContentType.PersonFullName,
            )
            AppTextField(
                value = state.email,
                onValueChange = { onChange(AuthField.Email, it) },
                label = "Correo electrónico",
                leadingIcon = Icons.Outlined.Mail,
                error = state.errors[AuthField.Email],
                keyboardOptions = next.copy(keyboardType = KeyboardType.Email),
                autofill = ContentType.EmailAddress,
            )
            AppTextField(
                value = state.username,
                onValueChange = { onChange(AuthField.Username, it) },
                label = "Nombre de usuario",
                leadingIcon = Icons.Outlined.AlternateEmail,
                error = state.errors[AuthField.Username],
                help = "Minúsculas, números, punto o guion bajo.",
                keyboardOptions = next.copy(keyboardType = KeyboardType.Ascii),
                autofill = ContentType.NewUsername,
            )
            AppTextField(
                value = state.password,
                onValueChange = { onChange(AuthField.Password, it) },
                label = "Contraseña",
                leadingIcon = Icons.Outlined.Lock,
                error = state.errors[AuthField.Password],
                help = "Mínimo 8 caracteres.",
                password = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                autofill = ContentType.NewPassword,
            )
            FormError(state.formError)
            AppButton(
                text = if (state.loading) "Creando tu cuenta…" else "Crear cuenta",
                onClick = onSubmit,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            )
            AuthAlternatives(
                separator = "o",
                question = "¿Ya tienes una cuenta?",
                link = "Inicia sesión",
                onLink = onBack,
                onGoogle = onGoogle,
                google = google,
            )
        }
    }
}
