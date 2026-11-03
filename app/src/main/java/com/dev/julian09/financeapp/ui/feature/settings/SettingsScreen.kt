package com.dev.julian09.financeapp.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.Settings
import com.dev.julian09.financeapp.domain.model.SyncStatus
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.ConfirmDialog
import com.dev.julian09.financeapp.ui.components.SegmentedSelector
import com.dev.julian09.financeapp.ui.feature.auth.rememberGoogleIdToken
import com.dev.julian09.financeapp.ui.feature.mail.MailProvidersViewModel
import com.dev.julian09.financeapp.ui.feature.recurring.days
import com.dev.julian09.financeapp.ui.feature.recurring.rememberNotificationPermissionRequest
import com.dev.julian09.financeapp.ui.feature.sync.SyncStatusViewModel
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenDeactivated: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenImportExport: () -> Unit = {},
    onOpenTags: () -> Unit = {},
    onOpenChangeHistory: () -> Unit = {},
    onOpenCustomizeHome: () -> Unit = {},
    onOpenConnectEmail: () -> Unit = {},
    onOpenMailProviders: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    syncViewModel: SyncStatusViewModel = hiltViewModel(),
    reminderViewModel: ReminderSettingsViewModel = hiltViewModel(),
    mailViewModel: MailProvidersViewModel = hiltViewModel(),
) {
    val mail by mailViewModel.overview.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val syncStatus by syncViewModel.status.collectAsStateWithLifecycle()
    val reminders by reminderViewModel.settings.collectAsStateWithLifecycle()
    // Turning notices on asks for the permission when it is missing
    val requestPermission = rememberNotificationPermissionRequest()
    // The Google confirmation of "Eliminar mi cuenta" only exists for accounts linked to Google
    val googlePicker =
        rememberGoogleIdToken(
            onToken = { viewModel.confirmDeleteAccount(idToken = it) },
            onFailed = viewModel::deleteAccountFailed,
        )
    val googleConfirm = googlePicker.takeIf { state.user?.googleLinked == true }
    SettingsContent(
        state = state,
        syncStatus = syncStatus,
        onBack = onBack,
        onRetry = viewModel::refreshServer,
        onLogoutRequest = viewModel::requestLogout,
        onLogoutConfirm = viewModel::confirmLogout,
        onLogoutDismiss = viewModel::dismissLogout,
        onDeleteAccountRequest = viewModel::requestDeleteAccount,
        onDeleteAccountConfirm = { viewModel.confirmDeleteAccount(password = it) },
        onDeleteAccountGoogle = googleConfirm,
        onDeleteAccountDismiss = viewModel::dismissDeleteAccount,
        onOpenCategories = onOpenCategories,
        onOpenDeactivated = onOpenDeactivated,
        onOpenRecurring = onOpenRecurring,
        onOpenImportExport = onOpenImportExport,
        onOpenTags = onOpenTags,
        onOpenChangeHistory = onOpenChangeHistory,
        reminders = reminders,
        onNotifications = { enabled ->
            reminderViewModel.setNotifications(enabled)
            if (enabled) requestPermission()
        },
        onLeadDays = reminderViewModel::setLeadDays,
        onBalanceView = reminderViewModel::setBalanceView,
        onDecimals = reminderViewModel::setDecimals,
        onOpenCustomizeHome = onOpenCustomizeHome,
        onOpenConnectEmail = onOpenConnectEmail,
        mailSummary = mail?.summary,
        onOpenMailProviders = onOpenMailProviders,
    )
}

/** Settings so far: profile, server status, notices (S6-UI03), data, sign-out and version. */
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onLogoutRequest: () -> Unit = {},
    onLogoutConfirm: () -> Unit = {},
    onLogoutDismiss: () -> Unit = {},
    onDeleteAccountRequest: () -> Unit = {},
    onDeleteAccountConfirm: (String) -> Unit = {},
    onDeleteAccountGoogle: (() -> Unit)? = null,
    onDeleteAccountDismiss: () -> Unit = {},
    syncStatus: SyncStatus? = null,
    onOpenCategories: () -> Unit = {},
    onOpenDeactivated: () -> Unit = {},
    onOpenRecurring: () -> Unit = {},
    onOpenImportExport: () -> Unit = {},
    onOpenTags: () -> Unit = {},
    onOpenChangeHistory: () -> Unit = {},
    reminders: Settings? = null,
    onNotifications: (Boolean) -> Unit = {},
    onLeadDays: (Int) -> Unit = {},
    onBalanceView: (String) -> Unit = {},
    onDecimals: (Int) -> Unit = {},
    onOpenCustomizeHome: () -> Unit = {},
    onOpenConnectEmail: () -> Unit = {},
    /** Ajustes → Correos (changes-ui-2.3 §3.1); null hides the group until the providers load. */
    mailSummary: String? = null,
    onOpenMailProviders: () -> Unit = {},
) {
    val colors = CofferTheme.colors
    var showAbout by rememberSaveable { mutableStateOf(false) }
    LogoutDialogIfNeeded(state.confirmingLogout, onLogoutConfirm, onLogoutDismiss)
    if (showAbout) AboutSheet(state.appVersion, onDismiss = { showAbout = false })
    state.deleteAccount?.let { DeleteAccountDialog(it, onDeleteAccountConfirm, onDeleteAccountGoogle, onDeleteAccountDismiss) }
    Column(modifier.fillMaxSize()) {
        // NAV-APP-011: the sync state lives in the card under the profile, not in the header
        AppHeader(title = "Ajustes", onBack = onBack)
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
                        syncStatus?.let {
                            Text(syncLine(it), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
                if (state.server == ServerState.Unavailable) {
                    AppButton("Reintentar", onRetry, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
                }
            }
            reminders?.let { HomeCard(it, onBalanceView, onOpenCustomizeHome) }
            reminders?.let { RemindersCard(it, onNotifications, onLeadDays) }
            reminders?.let { AmountsCard(it, onDecimals) }
            // COR-APP-001: Proveedores de correo and Conectar correo together under Correos
            MailCard(mailSummary, onOpenMailProviders, onOpenConnectEmail)
            // NAV-APP-015 (Figma `20:1541`): the section has its title and Figma's order
            Text("Datos", style = MaterialTheme.typography.titleSmall, color = colors.textSecondary)
            AppCard {
                LinkRow(Icons.AutoMirrored.Outlined.Label, "Categorías", onOpenCategories)
                HorizontalDivider(thickness = 1.dp, color = colors.border)
                LinkRow(Icons.Outlined.Sell, "Etiquetas", onOpenTags)
                HorizontalDivider(thickness = 1.dp, color = colors.border)
                LinkRow(Icons.Outlined.EventRepeat, "Gastos recurrentes", onOpenRecurring)
                HorizontalDivider(thickness = 1.dp, color = colors.border)
                LinkRow(Icons.Outlined.ImportExport, "Importar y exportar", onOpenImportExport)
                HorizontalDivider(thickness = 1.dp, color = colors.border)
                LinkRow(Icons.Outlined.History, "Historial de cambios", onOpenChangeHistory)
                HorizontalDivider(thickness = 1.dp, color = colors.border)
                LinkRow(Icons.Outlined.AccountBalance, "Cuentas desactivadas", onOpenDeactivated)
                HorizontalDivider(thickness = 1.dp, color = colors.border)
                LinkRow(Icons.Outlined.Info, "Acerca de Coffer", { showAbout = true })
            }
            if (state.user != null) {
                AppButton(
                    "Cerrar sesión",
                    onClick = onLogoutRequest,
                    kind = ButtonKind.Danger,
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    modifier = Modifier.fillMaxWidth(),
                )
                AppButton(
                    "Eliminar mi cuenta",
                    onClick = onDeleteAccountRequest,
                    kind = ButtonKind.Ghost,
                    icon = Icons.Outlined.DeleteForever,
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

/** Ajustes → Inicio: which balance Home shows (REQ-CTA-09). */
@Composable
private fun HomeCard(
    settings: Settings,
    onBalanceView: (String) -> Unit,
    onOpenCustomizeHome: () -> Unit,
) {
    val colors = CofferTheme.colors
    Text("Inicio", style = MaterialTheme.typography.titleSmall, color = colors.textSecondary)
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Vista del saldo", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                Text("La otra se ve al tocar el saldo", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            SegmentedSelector(
                listOf("Disponible", "Neto"),
                selectedIndex = if (settings.homeBalanceView == "NET") 1 else 0,
                onSelect = { onBalanceView(if (it == 1) "NET" else "AVAILABLE") },
                // NAV-APP-012: narrow enough for the explanation to read in two lines
                modifier = Modifier.width(156.dp),
            )
        }
        HorizontalDivider(thickness = 1.dp, color = colors.border)
        LinkRow(Icons.Outlined.Dashboard, "Personalizar inicio", onOpenCustomizeHome)
    }
}

/** Ajustes → Correos, between Montos and Datos (changes-ui-2.3 §3.1). */
@Composable
private fun MailCard(
    summary: String?,
    onOpenProviders: () -> Unit,
    onOpenConnect: () -> Unit,
) {
    val colors = CofferTheme.colors
    Text("Correos", style = MaterialTheme.typography.titleSmall, color = colors.textSecondary)
    AppCard {
        MailRow("Proveedores de correo", summary, onOpenProviders)
        HorizontalDivider(thickness = 1.dp, color = colors.border)
        MailRow("Conectar correo", "Reenvía tus comprobantes a Coffer", onOpenConnect)
    }
}

@Composable
private fun MailRow(
    title: String,
    detail: String?,
    onClick: () -> Unit,
) {
    val colors = CofferTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBox(Icons.Outlined.Email)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary)
    }
}

/** Figma `20:1541`: the row icons sit in a 32 dp rounded square. */
@Composable
private fun IconBox(icon: ImageVector) {
    val colors = CofferTheme.colors
    Box(Modifier.size(32.dp).background(colors.iconContainer, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = colors.iconOnContainer, modifier = Modifier.size(18.dp))
    }
}

private fun syncLine(status: SyncStatus) =
    when (status) {
        is SyncStatus.Synced -> "Sincronizado · 0 cambios pendientes"
        is SyncStatus.Pending -> if (status.count == 1) "1 cambio pendiente" else "${status.count} cambios pendientes"
        SyncStatus.Syncing -> "Sincronizando…"
        SyncStatus.Offline -> "Sin conexión · los cambios se guardan en el teléfono"
    }

/** Ajustes → Montos (RNF-13): 0 or 2 decimals in every amount; the currency is fixed. */
@Composable
private fun AmountsCard(
    settings: Settings,
    onDecimals: (Int) -> Unit,
) {
    val colors = CofferTheme.colors
    Text("Montos", style = MaterialTheme.typography.titleSmall, color = colors.textSecondary)
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Decimales", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                Text("Cómo se muestran los montos", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            SegmentedSelector(
                listOf("0", "2"),
                selectedIndex = if (settings.amountDecimals == 2) 1 else 0,
                onSelect = { onDecimals(if (it == 1) 2 else 0) },
                modifier = Modifier.width(120.dp),
            )
        }
        HorizontalDivider(thickness = 1.dp, color = colors.border)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Moneda", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            Text("COP · peso colombiano", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}

/** REQ-REC-03, 10: only pending payments ever notify, and the user can turn them off. */
@Composable
private fun RemindersCard(
    settings: Settings,
    onNotifications: (Boolean) -> Unit,
    onLeadDays: (Int) -> Unit,
) {
    val colors = CofferTheme.colors
    Text("Avisos", style = MaterialTheme.typography.titleSmall, color = colors.textSecondary)
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Notificaciones de pagos pendientes", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                Text(
                    "Solo al abrir la ventana de aviso y el día de vencimiento",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            // NAV-APP-013: a little smaller, as in Figma
            Switch(
                modifier = Modifier.scale(0.8f),
                checked = settings.notificationsEnabled,
                onCheckedChange = onNotifications,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.primary, checkedThumbColor = colors.onPrimary),
            )
        }
        HorizontalDivider(thickness = 1.dp, color = colors.border)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Anticipación de avisos", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                Text("Días antes del vencimiento", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            // NAV-APP-014 (Figma `20:1541`): − n días + instead of a dialog; 0 to 30 days as before
            Stepper(settings.reminderLeadDays, onLeadDays)
        }
    }
}

@Composable
private fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
) {
    val colors = CofferTheme.colors
    Row(
        Modifier.border(1.dp, colors.border, CofferShapes.pill),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onChange(value - 1) }, enabled = value > 0, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Remove, contentDescription = "Un día menos", tint = colors.textPrimary, modifier = Modifier.size(18.dp))
        }
        Text(days(value), style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
        IconButton(onClick = { onChange(value + 1) }, enabled = value < 30, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = "Un día más", tint = colors.textPrimary, modifier = Modifier.size(18.dp))
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
        IconBox(icon)
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary)
    }
}
