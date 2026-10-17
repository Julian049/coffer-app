package com.dev.julian09.financeapp.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.theme.CofferTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsContent(state = state, onBack = onBack, onRetry = viewModel::refreshServer)
}

/** Minimal Settings of Sprint 1: server status and app version. Profile, notices and data arrive in later sprints. */
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Ajustes", onBack = onBack)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val (icon, tint, title, detail) =
                        when (state.server) {
                            ServerState.Checking ->
                                ServerRow(
                                    Icons.Outlined.CloudSync,
                                    colors.textSecondary,
                                    "Servidor: comprobando…",
                                    "Puede tardar si el servidor estaba en reposo.",
                                )
                            ServerState.Available ->
                                ServerRow(
                                    Icons.Outlined.CloudDone,
                                    colors.income,
                                    "Servidor: disponible",
                                    "La copia en la nube está lista.",
                                )
                            ServerState.Unavailable ->
                                ServerRow(
                                    Icons.Outlined.CloudOff,
                                    colors.expense,
                                    "Servidor: no disponible",
                                    "La app sigue funcionando sin conexión.",
                                )
                        }
                    Box(Modifier.size(40.dp).background(colors.background, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
                if (state.server == ServerState.Unavailable) {
                    AppButton("Reintentar", onRetry, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
                }
            }
            Text(
                "Versión ${state.appVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private data class ServerRow(
    val icon: ImageVector,
    val tint: Color,
    val title: String,
    val detail: String,
)
