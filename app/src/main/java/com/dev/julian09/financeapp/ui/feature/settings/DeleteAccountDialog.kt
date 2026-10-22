package com.dev.julian09.financeapp.ui.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/**
 * S14-H01: asks for the password again (or a Google confirmation) before erasing the account for good.
 * [onGoogle] is null when the account has no Google sign-in or the build has no client id.
 */
@Composable
fun DeleteAccountDialog(
    state: DeleteAccountUi,
    onConfirm: (password: String) -> Unit,
    onGoogle: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val colors = CofferTheme.colors
    var password by rememberSaveable { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = CofferShapes.dialog, color = colors.surface, border = BorderStroke(1.dp, colors.border)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("¿Eliminar tu cuenta?", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Text(
                    "Se borran para siempre tu cuenta y todos tus datos: cuentas, movimientos, categorías, etiquetas " +
                        "y ajustes, tanto en la nube como en este teléfono. No se puede deshacer. Necesitas conexión.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Contraseña",
                    password = true,
                    enabled = !state.working,
                    help = if (onGoogle != null) "Si entraste con Google, confirma con el botón de abajo." else null,
                )
                state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.danger) }
                if (onGoogle != null) {
                    AppButton(
                        "Confirmar con Google",
                        onGoogle,
                        kind = ButtonKind.Secondary,
                        enabled = !state.working,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    AppButton("Cancelar", onDismiss, kind = ButtonKind.Ghost, enabled = !state.working)
                    AppButton(
                        if (state.working) "Eliminando…" else "Eliminar",
                        { onConfirm(password) },
                        kind = ButtonKind.Danger,
                        enabled = !state.working && password.isNotEmpty(),
                    )
                }
            }
        }
    }
}
