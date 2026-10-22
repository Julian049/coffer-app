package com.dev.julian09.financeapp.ui.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.finance.BalanceCalculator
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountImpact
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.CardOverview
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.LoanOverview
import com.dev.julian09.financeapp.domain.model.Money
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
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.accountIcon
import com.dev.julian09.financeapp.ui.components.accountTypeLabel
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.feature.cards.PayCardSheet
import com.dev.julian09.financeapp.ui.feature.loans.LoanPaymentSheet
import com.dev.julian09.financeapp.ui.feature.loans.LoanSheet
import com.dev.julian09.financeapp.ui.feature.recurring.dayMonth
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
    onNewLoan: () -> Unit = {},
    avatarInitial: Char? = null,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    var payingCard by rememberSaveable { mutableStateOf<String?>(null) }
    var payingLoan by rememberSaveable { mutableStateOf<String?>(null) }
    PayCardSheet(payingCard?.let(UUID::fromString), onDismiss = { payingCard = null })
    LoanPaymentSheet(payingLoan?.let(UUID::fromString), LoanSheet.PAY, onDismiss = { payingLoan = null })
    AccountsContent(
        accounts = accounts,
        onOpenSettings = onOpenSettings,
        onNewAccount = onNewAccount,
        onOpenAccount = onOpenAccount,
        onOpenOptions = viewModel::openOptions,
        onOpenDeactivated = onOpenDeactivated,
        modifier = modifier,
        avatarInitial = avatarInitial,
        cards = cards,
        loans = loans,
        onNewLoan = onNewLoan,
        onPay = { a ->
            if (a.account.type == AccountType.LOAN) payingLoan = a.account.id.toString() else payingCard = a.account.id.toString()
        },
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

/** Gestión de cuentas (Figma `1:1990`, S12-UI04): what you have, and what you owe on cards and loans. */
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
    cards: Map<UUID, CardOverview> = emptyMap(),
    loans: Map<UUID, LoanOverview> = emptyMap(),
    onNewLoan: () -> Unit = {},
    onPay: (AccountBalance) -> Unit = {},
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Gestión de cuentas", actions = { Avatar(onClick = onOpenSettings, initial = avatarInitial) })
        if (accounts == null) return@Column
        val active = accounts.filter { it.account.active }
        val inactive = accounts.size - active.size
        val assets = active.filter { !it.account.type.isDebt }
        val creditCards = active.filter { it.account.type == AccountType.CREDIT_CARD }
        val loanAccounts = active.filter { it.account.type == AccountType.LOAN }
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppCard(highlight = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Patrimonio neto disponible",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
                MoneyText(BalanceCalculator.available(active), style = MaterialTheme.typography.displaySmall)
                Text(
                    if (active.size == 1) "1 cuenta activa" else "${active.size} cuentas activas",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                AppButton("Nueva cuenta", onClick = onNewAccount, icon = Icons.Outlined.Add, modifier = Modifier.fillMaxWidth())
            }
            if (active.isEmpty()) {
                EmptyState(title = "Aún no tienes cuentas", message = "Crea tu primera cuenta de efectivo o de ahorros.")
            }
            if (assets.isNotEmpty()) {
                GroupTitle("Lo que tienes", assets.fold(Money.ZERO) { sum, it -> sum + it.balance }, MoneyKind.Neutral)
                SubTitle("Cuentas bancarias y efectivo", assets.size)
                assets.forEach { AccountCard(it, onOpen = { onOpenAccount(it.account.id) }, onMore = { onOpenOptions(it) }) }
            }
            if (creditCards.isNotEmpty() || loanAccounts.isNotEmpty()) {
                GroupTitle("Lo que debes", (creditCards + loanAccounts).fold(Money.ZERO) { sum, it -> sum + it.balance }, MoneyKind.Expense)
            }
            if (creditCards.isNotEmpty()) {
                SubTitle("Tarjetas de crédito", creditCards.size)
                creditCards.forEach { card ->
                    CardAccountCard(
                        card,
                        cards[card.account.id],
                        onOpen = { onOpenAccount(card.account.id) },
                        onMore = { onOpenOptions(card) },
                        onPay = { onPay(card) },
                    )
                }
            }
            SubTitle("Préstamos", loanAccounts.size) {
                TextButton(onClick = onNewLoan) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                    Text(" Agregar", color = colors.textPrimary, style = MaterialTheme.typography.labelLarge)
                }
            }
            loanAccounts.forEach { loan ->
                LoanAccountCard(
                    loan,
                    loans[loan.account.id],
                    onOpen = { onOpenAccount(loan.account.id) },
                    onMore = { onOpenOptions(loan) },
                    onPay = { onPay(loan) },
                )
            }
            if (creditCards.isNotEmpty()) {
                Surface(shape = CofferShapes.card, color = colors.primaryContainer) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Mantener las tarjetas por debajo del 30% del cupo total fortalece tu perfil crediticio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onPrimaryContainer,
                        )
                    }
                }
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
private fun GroupTitle(
    title: String,
    total: Money,
    kind: MoneyKind,
) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = CofferTheme.colors.textPrimary)
        MoneyText(total, kind = kind, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SubTitle(
    title: String,
    count: Int,
    action: @Composable () -> Unit = {},
) {
    val colors = CofferTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Text("$count", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary.copy(alpha = 0.6f))
        Spacer(Modifier.weight(1f))
        action()
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

/** The head of every account card: icon, name with two lines at most (S12-UI07), detail and options. */
@Composable
private fun CardHead(
    item: AccountBalance,
    detail: String,
    onMore: () -> Unit,
) {
    val colors = CofferTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AccountIcon(item)
        Column(Modifier.weight(1f)) {
            Text(
                item.account.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item.account.color?.let { ColorDot(it) }
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onMore) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "Opciones de ${item.account.name}", tint = colors.textSecondary)
        }
    }
}

/** Pagar · Ver movimientos, or Ver movimientos alone, under a divider. */
@Composable
private fun CardActions(
    onOpen: () -> Unit,
    onPay: (() -> Unit)? = null,
) {
    val colors = CofferTheme.colors
    HorizontalDivider(thickness = 1.dp, color = colors.border)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        onPay?.let {
            TextButton(onClick = it, Modifier.weight(1f)) {
                Icon(Icons.Outlined.Payments, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                Text("  Pagar", color = colors.textPrimary, style = MaterialTheme.typography.labelLarge)
            }
            Box(Modifier.size(width = 1.dp, height = 20.dp).background(colors.border))
        }
        TextButton(onClick = onOpen, Modifier.weight(1f)) {
            Icon(
                Icons.AutoMirrored.Outlined.ReceiptLong,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp),
            )
            Text("  Ver movimientos", color = colors.textPrimary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun UsageBar(
    fraction: Float,
    color: androidx.compose.ui.graphics.Color,
) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        Modifier.fillMaxWidth().height(6.dp),
        color = color,
        trackColor = CofferTheme.colors.border,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
private fun AccountCard(
    item: AccountBalance,
    onOpen: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = CofferTheme.colors
    AppCard(onClick = onOpen) {
        CardHead(item, listOfNotNull(accountTypeLabel(item.account.type), item.account.institution).joinToString(" · "), onMore)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (item.account.includeInTotal) "Saldo disponible" else "Saldo · fuera del total",
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            MoneyText(item.balance, style = MaterialTheme.typography.titleLarge)
        }
        CardActions(onOpen)
    }
}

@Composable
private fun CardAccountCard(
    item: AccountBalance,
    overview: CardOverview?,
    onOpen: () -> Unit,
    onMore: () -> Unit,
    onPay: () -> Unit,
) {
    val colors = CofferTheme.colors
    AppCard(onClick = onOpen) {
        CardHead(item, listOfNotNull("Crédito rotativo", item.account.institution).joinToString(" · "), onMore)
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Deuda actual al corte", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                MoneyText(item.balance, kind = MoneyKind.Expense, style = MaterialTheme.typography.titleLarge)
            }
            overview?.let {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Próximo pago · ${dayMonth(it.statement.period.dueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    MoneyText(it.statement.minimum, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        overview?.takeIf { it.card.creditLimit > Money.ZERO }?.let {
            UsageBar(
                (
                    it.debt.toBigDecimal().toFloat() /
                        it.card.creditLimit
                            .toBigDecimal()
                            .toFloat()
                ),
                colors.expense,
            )
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "Disponible ${formatMoney(it.available)}",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                Text("Cupo ${formatMoney(it.card.creditLimit)}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        CardActions(onOpen, onPay)
    }
}

@Composable
private fun LoanAccountCard(
    item: AccountBalance,
    overview: LoanOverview?,
    onOpen: () -> Unit,
    onMore: () -> Unit,
    onPay: () -> Unit,
) {
    val colors = CofferTheme.colors
    val total = overview?.installments?.count { it.status != InstallmentStatus.CANCELLED } ?: 0
    val next = overview?.next
    val detail =
        listOfNotNull(
            item.account.institution,
            next?.let { "cuota ${it.number} de $total" },
        ).joinToString(" · ").ifEmpty { accountTypeLabel(item.account.type) }
    AppCard(onClick = onOpen) {
        CardHead(item, detail, onMore)
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Deuda pendiente", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                MoneyText(item.balance, kind = MoneyKind.Expense, style = MaterialTheme.typography.titleLarge)
            }
            next?.let {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Próxima cuota · ${dayMonth(it.dueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    MoneyText(it.total, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        if (overview != null && total > 0) {
            UsageBar(overview.paid.toFloat() / total, colors.primary)
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "${overview.paid} de $total cuotas pagadas",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                overview.installments.lastOrNull { it.status != InstallmentStatus.CANCELLED }?.let {
                    Text("Termina ${monthYear(it.dueDate)}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
        }
        CardActions(onOpen, onPay)
    }
}

private fun monthYear(date: java.time.LocalDate) = MONTH_YEAR.format(date).replace(".", "")

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
        is AccountSheet.Options -> OptionsSheet(sheet.account.account.type, onDismiss, onEdit, onStartRemove)
        is AccountSheet.Remove -> RemoveSheet(sheet, accounts, onDismiss, onSelectMode, onSelectTarget, onApply)
        is AccountSheet.Confirm -> ConfirmSheet(sheet, onDismiss, onType, onConfirmDelete)
    }
}

@Composable
private fun OptionsSheet(
    type: AccountType,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    // Hoja · Opciones de la cuenta and de la tarjeta (Figma `14:310`, `103:90`)
    val (what, detail) =
        when (type) {
            AccountType.CREDIT_CARD -> "tarjeta" to "Nombre, cupo, tasa, fechas y color"
            AccountType.LOAN -> "préstamo" to "Nombre, tasa, día de pago, seguro y costos"
            else -> "cuenta" to "Nombre, ícono, color e inclusión en el saldo total"
        }
    val title =
        when (type) {
            AccountType.CREDIT_CARD -> "Opciones de la tarjeta"
            AccountType.LOAN -> "Opciones del préstamo"
            else -> "Opciones de la cuenta"
        }
    AppBottomSheet(onDismiss = onDismiss, title = title) {
        OptionRow(
            Icons.Outlined.Edit,
            "Editar $what",
            detail,
            danger = false,
            onClick = onEdit,
        )
        OptionRow(
            Icons.Outlined.DeleteOutline,
            "Eliminar $what",
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
