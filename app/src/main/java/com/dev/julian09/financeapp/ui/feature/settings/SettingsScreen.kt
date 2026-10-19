package com.dev.julian09.financeapp.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material3.HorizontalDivider
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.SyncStatus
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.ConfirmDialog
import com.dev.julian09.financeapp.ui.components.SyncBadge
import com.dev.julian09.financeapp.ui.feature.sync.SyncStatusViewModel
import com.dev.julian09.financeapp.ui.theme.CofferTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenDeactivated: () -> Unit,
    onOpenRecurring: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    syncViewModel: SyncStatusViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val syncStatus by syncViewModel.status.collectAsStateWithLifecycle()
    SettingsContent(
        state = state,
        syncStatus = syncStatus,
        onBack = onBack,
        onRetry = viewModel::refreshServer,
        onLogoutRequest = viewModel::requestLogout,
        onLogoutConfirm = viewModel::confirmLogout,
        onLogoutDismiss = viewModel::dismissLogout,
        onOpenCategories = onOpenCategories,
        onOpenDeactivated = onOpenDeactivated,
        onOpenRecurring = onOpenRecurring,
    )
}

/** Settings so far: profile, server status, sign-out and version. Notices and data arrive in later sprints. */
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onLogoutRequest: () -> Unit = {},
    onLogoutConfirm: () -> Unit = {},
    onLogoutDismiss: () -> Unit = {},
    syncStatus: SyncStatus? = null,
    onOpenCategories: () -> Unit = {},
    onOpenDeactivated: () -> Unit = {},
    onOpenRecurring: () -> Unit = {},
) {
    val colors = CofferTheme.colors
    LogoutDialogIfNeeded(state.confirmingLogout, onLogoutConfirm, onLogoutDismiss)
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Ajustes", onBack = onBack, actions = { syncStatus?.let { SyncBadge(it) } })
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.user?.let { user ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(onClick = {}, initial = user.displayName.firstOrNull(), size = 48)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(user.displayName, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text(
                                "@${user.username} · ${user.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
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
            AppCard {
                LinkRow(Icons.AutoMirrored.Outlined.Label, "Categorías", onOpenCategories)
                HorizontalDivider(thickness = 1.dp, color = colors.border)
                LinkRow(Icons.Outlined.EventRepeat, "Gastos recurrentes", onOpenRecurring)
                HorizontalDivider(thickness = 1.dp, color = colors.border)
                LinkRow(Icons.Outlined.AccountBalance, "Cuentas desactivadas", onOpenDeactivated)
            }
            if (state.user != null) {
                AppButton(
                    "Cerrar sesión",
                    onClick = onLogoutRequest,
                    kind = ButtonKind.Danger,
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    modifier = Modifier.fillMaxWidth(),
                )
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

@Composable
private fun LogoutDialogIfNeeded(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (show) {
        ConfirmDialog(
            title = "¿Cerrar sesión?",
            message =
                "Tus datos se quedan en este teléfono, pero dejarán de sincronizarse. " +
                    "Los cambios que hagas después no tendrán respaldo en la nube.",
            confirmText = "Cerrar sesión",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
        )
    }
}

private data class ServerRow(
    val icon: ImageVector,
    val tint: Color,
    val title: String,
    val detail: String,
)

@Composable
private fun LinkRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = CofferTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.iconOnContainer, modifier = Modifier.size(20.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary)
    }
}
