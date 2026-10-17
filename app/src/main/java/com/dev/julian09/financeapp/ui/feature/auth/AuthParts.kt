package com.dev.julian09.financeapp.ui.feature.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/** "o continúa con" divider, Google button disabled until Sprint 11 (REQ-AUT-03) and the link to the other screen. */
@Composable
internal fun AuthAlternatives(
    separator: String,
    question: String,
    link: String,
    onLink: () -> Unit,
) {
    val colors = CofferTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(Modifier.weight(1f), color = colors.border)
        Text(separator, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        HorizontalDivider(Modifier.weight(1f), color = colors.border)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AppButton("Continuar con Google", onClick = {}, kind = ButtonKind.Secondary, enabled = false, modifier = Modifier.fillMaxWidth())
        Text("Disponible pronto", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text("$question ", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Text(
            link,
            style = MaterialTheme.typography.labelLarge,
            color = colors.primary,
            modifier = Modifier.clickable(role = Role.Button, onClick = onLink).padding(vertical = 4.dp),
        )
    }
}

@Composable
internal fun FormError(message: String?) {
    if (message != null) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = CofferTheme.colors.expense,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
