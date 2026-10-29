package com.dev.julian09.financeapp.ui.feature.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.InitialSyncProgress
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.BrewingFox
import com.dev.julian09.financeapp.ui.theme.CofferTheme

@Composable
fun InitialDownloadScreen(viewModel: InitialDownloadViewModel = hiltViewModel()) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    InitialDownloadContent(progress, onRetry = viewModel::retry)
}

/** Labels per entity of the sync protocol, in the order they are listed. */
private val ENTITY_LABELS =
    listOf(
        listOf("account") to "Cuentas",
        listOf("category") to "Categorías",
        listOf("transaction") to "Transacciones",
        listOf("recurringExpense", "tag") to "Recurrentes y etiquetas",
    )

/** Descargando tus datos (S3-UI01, Figma section 1 · Acceso). */
@Composable
fun InitialDownloadContent(
    progress: InitialSyncProgress,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    Column(
        modifier.fillMaxSize().background(colors.background).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val fraction = progress.total?.takeIf { it > 0 }?.let { (progress.received.toFloat() / it).coerceIn(0f, 1f) }
        // 2.2: the fox waits for its coffee; the glass fills with the same value as the bar
        BrewingFox(fraction)
        Text("Descargando tus datos", fontSize = 22.sp, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Text(
            when {
                progress.failed -> "No pudimos terminar la descarga. Revisa que hayas iniciado sesión con tu cuenta e inténtalo de nuevo."
                progress.waiting -> "Esperando conexión. La descarga sigue sola cuando haya internet."
                else -> "Puede tardar un momento."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        AppCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth()) {
                Text("Progreso", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                if (fraction != null) {
                    Text("${(fraction * 100).toInt()} %", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                }
            }
            if (fraction != null) {
                LinearProgressIndicator(
                    progress = { fraction },
                    Modifier.fillMaxWidth(),
                    color = colors.primary,
                    trackColor = colors.border,
                )
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.primary, trackColor = colors.border)
            }
            ENTITY_LABELS.forEach { (entities, label) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    Text(
                        "${entities.sumOf { progress.byEntity[it] ?: 0L }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                    )
                }
            }
        }
        if (progress.failed) AppButton("Reintentar", onClick = onRetry, modifier = Modifier.fillMaxWidth())
    }
}
