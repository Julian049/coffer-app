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
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.theme.CofferTheme

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ForgotPasswordContent(
        state,
        ForgotPasswordActions(
            onBack = onBack,
            onEmail = viewModel::onEmail,
            onCode = viewModel::onCode,
            onPassword = viewModel::onPassword,
            onSendCode = viewModel::sendCode,
            onChangePassword = viewModel::changePassword,
            onOtherEmail = viewModel::backToEmail,
        ),
    )
}

class ForgotPasswordActions(
    val onBack: () -> Unit = {},
    val onEmail: (String) -> Unit = {},
    val onCode: (String) -> Unit = {},
    val onPassword: (String) -> Unit = {},
    val onSendCode: () -> Unit = {},
    val onChangePassword: () -> Unit = {},
    val onOtherEmail: () -> Unit = {},
)

/**
 * Recuperar contraseña (REQ-AUT-09), functional with the current components; Figma's screens come in part 2 of 2.0.
 * One screen, three steps: email, code and new password, done.
 */
@Composable
fun ForgotPasswordContent(
    state: ForgotPasswordState,
    actions: ForgotPasswordActions,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Recuperar contraseña", onBack = actions.onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state.step) {
                ForgotStep.EMAIL -> {
                    Text(
                        "Escribe el correo de tu cuenta. Te enviaremos un código de 6 dígitos para crear una contraseña nueva.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    AppTextField(
                        value = state.email,
                        onValueChange = actions.onEmail,
                        label = "Correo electrónico",
                        placeholder = "ejemplo@coffer.app",
                        leadingIcon = Icons.Outlined.AlternateEmail,
                        error = state.errors[AuthField.Email],
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { actions.onSendCode() }),
                        autofill = ContentType.EmailAddress,
                    )
                    FormError(state.formError)
                    AppButton(
                        if (state.loading) "Enviando…" else "Enviar código",
                        onClick = actions.onSendCode,
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                ForgotStep.CODE -> {
                    // REQ-AUT-10: the same text whether or not the account exists
                    Text(
                        "Si ${state.email.trim()} tiene una cuenta en Coffer, te llegó un código. Vence en 15 minutos; revisa también el spam.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    AppTextField(
                        value = state.code,
                        onValueChange = actions.onCode,
                        label = "Código",
                        placeholder = "000000",
                        leadingIcon = Icons.Outlined.Pin,
                        error = state.errors[AuthField.Code],
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                        autofill = ContentType.SmsOtpCode,
                    )
                    AppTextField(
                        value = state.password,
                        onValueChange = actions.onPassword,
                        label = "Contraseña nueva",
                        leadingIcon = Icons.Outlined.Lock,
                        error = state.errors[AuthField.Password],
                        password = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { actions.onChangePassword() }),
                        autofill = ContentType.NewPassword,
                    )
                    FormError(state.formError)
                    AppButton(
                        if (state.loading) "Cambiando…" else "Cambiar contraseña",
                        onClick = actions.onChangePassword,
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AppButton(
                        "Reenviar código",
                        onClick = actions.onSendCode,
                        kind = ButtonKind.Ghost,
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AppButton(
                        "Usar otro correo",
                        onClick = actions.onOtherEmail,
                        kind = ButtonKind.Ghost,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                ForgotStep.DONE -> {
                    Text("Listo, cambiaste tu contraseña.", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text(
                        "Por seguridad cerramos la sesión en todos tus teléfonos. Inicia sesión con la contraseña nueva.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    AppButton("Ir a iniciar sesión", onClick = actions.onBack, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
