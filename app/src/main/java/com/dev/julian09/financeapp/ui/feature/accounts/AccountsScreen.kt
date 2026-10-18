package com.dev.julian09.financeapp.ui.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.finance.BalanceCalculator
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountImpact
import com.dev.julian09.financeapp.domain.model.RemoveMode
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppChip
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.EmptyState
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.accountIcon
import com.dev.julian09.financeapp.ui.components.accountTypeLabel
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Composable
fun AccountsScreen(
    onOpenSettings: () -> Unit,
    onNewAccount: () -> Unit,
    onOpenAccount: (UUID) -> Unit,
    onEditAccount: (UUID) -> Unit,
    onOpenDeactivated: () -> Unit,
    modifier: Modifier = Modifier,
    avatarInitial: Char? = null,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    AccountsContent(
        accounts = accounts,
        onOpenSettings = onOpenSettings,
        onNewAccount = onNewAccount,
        onOpenAccount = onOpenAccount,
        onOpenOptions = viewModel::openOptions,
        onOpenDeactivated = onOpenDeactivated,
        modifier = modifier,
        avatarInitial = avatarInitial,
    )
    sheet?.let {
        AccountSheetHost(
            sheet = it,
            accounts = accounts.orEmpty(),
            onDismiss = viewModel::dismiss,
            onEdit = {
                viewModel.dismiss()
                onEditAccount(it.account.account.id)
            },
            onStartRemove = viewModel::startRemove,
            onSelectMode = viewModel::selectMode,
            onSelectTarget = viewModel::selectTarget,
            onApply = viewModel::applyRemove,
            onType = viewModel::typeConfirmation,
            onConfirmDelete = viewModel::confirmDelete,
        )
    }
}

/** Gestión de cuentas (S4-UI01). Cards and loans get their own section in S7 and S10. */
@Composable
fun AccountsContent(
    accounts: List<AccountBalance>?,
    onOpenSettings: () -> Unit,
    onNewAccount: () -> Unit,
    onOpenAccount: (UUID) -> Unit,
    onOpenOptions: (AccountBalance) -> Unit,
    onOpenDeactivated: () -> Unit,
    modifier: Modifier = Modifier,
    avatarInitial: Char? = null,
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Cuentas", actions = { Avatar(onClick = onOpenSettings, initial = avatarInitial) })
        if (accounts == null) return@Column
        val active = accounts.filter { it.account.active }
        val inactive = accounts.size - active.size
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppCard(highlight = true) {
                Text("Saldo total disponible", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                MoneyText(BalanceCalculator.available(active), style = MaterialTheme.typography.displaySmall)
                Text(
                    if (active.size == 1) "1 cuenta activa" else "${active.size} cuentas activas",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                AppButton("Nueva cuenta", onClick = onNewAccount, icon = Icons.Outlined.Add, modifier = Modifier.fillMaxWidth())
            }
            if (active.isEmpty()) {
                EmptyState(title = "Aún no tienes cuentas", message = "Crea tu primera cuenta de efectivo, débito o ahorros.")
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Cuentas bancarias y efectivo",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Text(
                        if (active.size ==
                            1
                        ) {
                            "1 activa"
                        } else {
                            "${active.size} activas"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                active.forEach { AccountCard(it, onClick = { onOpenAccount(it.account.id) }, onMore = { onOpenOptions(it) }) }
            }
            if (inactive > 0) {
                AppCard(onClick = onOpenDeactivated) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (inactive == 1) "1 cuenta desactivada" else "$inactive cuentas desactivadas",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textPrimary,
                        )
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountIcon(
    item: AccountBalance,
    size: Int = 44,
) {
    val colors = CofferTheme.colors
    Box(Modifier.size(size.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
        Icon(
            accountIcon(item.account.icon, item.account.type),
            contentDescription = null,
            tint = colors.iconOnContainer,
            modifier =
                Modifier.size(
                    (
                        size /
                            2
                    ).dp,
                ),
        )
    }
}

@Composable
private fun AccountCard(
    item: AccountBalance,
    onClick: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = CofferTheme.colors
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccountIcon(item)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.account.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    item.account.color?.let { ColorDot(it) }
                }
                Text(
                    listOfNotNull(accountTypeLabel(item.account.type), item.account.institution).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Opciones de ${item.account.name}", tint = colors.textSecondary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (item.account.includeInTotal) "Saldo disponible" else "Saldo · fuera del total",
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            MoneyText(item.balance, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun ColorDot(hex: String) {
    Box(Modifier.size(8.dp).background(Color(android.graphics.Color.parseColor(hex)), CircleShape))
}

private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("es-CO"))

/** "jun – oct 2026", or the single month. */
fun impactRange(impact: AccountImpact): String? {
    val zone = ZoneId.systemDefault()
    val first = impact.firstAt?.atZone(zone) ?: return null
    val last = impact.lastAt?.atZone(zone) ?: return null
    val to = MONTH_YEAR.format(last).replace(".", "")
    if (first.year == last.year && first.month == last.month) return to
    val from =
        (
            if (first.year ==
                last.year
            ) {
                DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("es-CO"))
            } else {
                MONTH_YEAR
            }
        ).format(first)
    return "${from.replace(".", "")} – $to"
}

@Composable
private fun AccountSheetHost(
    sheet: AccountSheet,
    accounts: List<AccountBalance>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onStartRemove: () -> Unit,
    onSelectMode: (RemoveMode) -> Unit,
    onSelectTarget: (UUID) -> Unit,
    onApply: () -> Unit,
    onType: (String) -> Unit,
    onConfirmDelete: () -> Unit,
) {
    when (sheet) {
        is AccountSheet.Options -> OptionsSheet(onDismiss, onEdit, onStartRemove)
        is AccountSheet.Remove -> RemoveSheet(sheet, accounts, onDismiss, onSelectMode, onSelectTarget, onApply)
        is AccountSheet.Confirm -> ConfirmSheet(sheet, onDismiss, onType, onConfirmDelete)
    }
}

@Composable
private fun OptionsSheet(
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss, title = "Opciones de la cuenta") {
        OptionRow(
            Icons.Outlined.Edit,
            "Editar cuenta",
            "Nombre, saldo inicial e inclusión en el saldo total",
            danger = false,
            onClick = onEdit,
        )
        OptionRow(
            Icons.Outlined.DeleteOutline,
            "Eliminar cuenta",
            "Desactivarla, mover sus movimientos o borrarla",
            danger = true,
            onClick = onRemove,
        )
        AppButton("Cancelar", onClick = onDismiss, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun OptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String?,
    danger: Boolean,
    onClick: () -> Unit,
) {
    val colors = CofferTheme.colors
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(36.dp).background(colors.background, CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (danger) colors.expense else colors.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = if (danger) colors.expense else colors.textPrimary)
                detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
            }
        }
    }
}

private fun summary(
    item: AccountBalance,
    impact: AccountImpact,
) = "${item.account.name} · Saldo ${formatMoney(impact.balance)} · " +
    if (impact.transactions == 1) "1 movimiento" else "${impact.transactions} movimientos"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RemoveSheet(
    sheet: AccountSheet.Remove,
    accounts: List<AccountBalance>,
    onDismiss: () -> Unit,
    onSelectMode: (RemoveMode) -> Unit,
    onSelectTarget: (UUID) -> Unit,
    onApply: () -> Unit,
) {
    val colors = CofferTheme.colors
    val targets = accounts.filter { it.account.active && it.account.id != sheet.account.account.id }
    AppBottomSheet(onDismiss = onDismiss, title = "Eliminar cuenta", subtitle = summary(sheet.account, sheet.impact)) {
        if (sheet.mode == RemoveMode.DELETE_ALL) {
            Warning(
                "Atención con esta cuenta",
                "Se borrarán la cuenta y todas sus transacciones de tus reportes. Esta acción no se puede deshacer.",
            )
        }
        RemoveOption(
            selected = sheet.mode == RemoveMode.DEACTIVATE,
            title = "Desactivar cuenta",
            badge = "Recomendada",
            detail =
                "Oculta la cuenta del día a día pero preserva intactos tus historiales, estadísticas y saldos pasados. " +
                    "Podrás reactivarla cuando quieras.",
            danger = false,
            onClick = { onSelectMode(RemoveMode.DEACTIVATE) },
        )
        RemoveOption(
            selected = sheet.mode == RemoveMode.MOVE,
            title = "Mover transacciones a otra cuenta",
            badge = null,
            detail = "Reasigna los movimientos de esta cuenta a otra antes de retirarla para no descuadrar tu histórico.",
            danger = false,
            enabled = targets.isNotEmpty(),
            onClick = { onSelectMode(RemoveMode.MOVE) },
        )
        RemoveOption(
            selected = sheet.mode == RemoveMode.DELETE_ALL,
            title = "Eliminar definitivamente",
            badge = null,
            detail =
                "Acción de alto riesgo. No recomendada. Esta acción borrará todas las transacciones históricas asociadas " +
                    "y alterará los saldos calculados de meses pasados. No se puede deshacer.",
            danger = true,
            onClick = { onSelectMode(RemoveMode.DELETE_ALL) },
        )
        if (sheet.mode == RemoveMode.MOVE) {
            Text(
                "Mover los ${sheet.impact.transactions} movimientos a",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                targets.forEach {
                    AppChip(
                        it.account.name,
                        selected = sheet.targetId == it.account.id,
                        onClick = { onSelectTarget(it.account.id) },
                    )
                }
            }
        }
        val (label, kind) =
            when (sheet.mode) {
                RemoveMode.DEACTIVATE -> "Desactivar cuenta" to ButtonKind.Primary
                RemoveMode.MOVE -> "Mover transacciones" to ButtonKind.Primary
                RemoveMode.DELETE_ALL -> "Eliminar definitivamente" to ButtonKind.Danger
            }
        AppButton(
            label,
            onClick = onApply,
            kind = kind,
            enabled = sheet.mode != RemoveMode.MOVE || sheet.targetId != null,
            modifier = Modifier.fillMaxWidth(),
        )
        AppButton("Cancelar", onClick = onDismiss, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Warning(
    title: String,
    message: String,
) {
    val colors = CofferTheme.colors
    Surface(shape = CofferShapes.card, color = colors.surface, border = androidx.compose.foundation.BorderStroke(1.dp, colors.expense)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = colors.expense)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.expense)
                Text(message, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun RemoveOption(
    selected: Boolean,
    title: String,
    badge: String?,
    detail: String,
    danger: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = CofferTheme.colors
    val border = if (selected) (if (danger) colors.expense else colors.onPrimaryContainer) else colors.border
    Surface(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        shape = CofferShapes.card,
        color = if (selected && !danger) colors.primaryContainer else colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(selectedColor = if (danger) colors.expense else colors.textPrimary),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = if (danger) colors.expense else colors.textPrimary)
                    badge?.let {
                        Surface(shape = CofferShapes.pill, color = colors.primary) {
                            Text(
                                it,
                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onPrimary,
                            )
                        }
                    }
                    if (danger) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = colors.expense,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected && !danger) colors.onPrimaryContainer else colors.textSecondary,
                )
            }
        }
    }
}

/** REQ-CTA-05: the impact first, then type the name to enable the button. */
@Composable
internal fun ConfirmSheet(
    sheet: AccountSheet.Confirm,
    onDismiss: () -> Unit,
    onType: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = CofferTheme.colors
    val name = sheet.account.account.name
    AppBottomSheet(onDismiss = onDismiss, title = "Eliminar cuenta y transacciones", subtitle = "Esta acción no se puede deshacer.") {
        Surface(
            shape = CofferShapes.card,
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.expense),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Impacto", style = MaterialTheme.typography.titleSmall, color = colors.expense)
                ImpactRow("Transacciones que se borran", "${sheet.impact.transactions}", colors.textPrimary)
                if (sheet.account.account.includeInTotal) {
                    ImpactRow("Tu saldo total baja", formatMoney(-sheet.impact.balance), colors.expense)
                }
                impactRange(sheet.impact)?.let { ImpactRow("Reportes afectados", it, colors.textPrimary) }
            }
        }
        AppTextField(
            value = sheet.typed,
            onValueChange = onType,
            label = "Escribe «$name» para confirmar",
            error = sheet.error?.message(),
        )
        AppButton(
            "Eliminar cuenta y transacciones",
            onClick = onConfirm,
            kind = ButtonKind.Danger,
            enabled = sheet.canDelete,
            modifier = Modifier.fillMaxWidth(),
        )
        AppButton("Cancelar", onClick = onDismiss, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ImpactRow(
    label: String,
    value: String,
    tint: Color,
) {
    val colors = CofferTheme.colors
    Row {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = tint)
    }
}
