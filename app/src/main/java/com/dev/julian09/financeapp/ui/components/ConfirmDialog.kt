package com.dev.julian09.financeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/** Confirmation (sign out, delete...). `destructive` paints the confirm action in the danger color. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Cancelar",
    destructive: Boolean = false,
) {
    val colors = CofferTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CofferShapes.dialog,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    AppButton(dismissText, onDismiss, kind = ButtonKind.Ghost)
                    AppButton(confirmText, onConfirm, kind = if (destructive) ButtonKind.Danger else ButtonKind.Primary)
                }
            }
        }
    }
}
