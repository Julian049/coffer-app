package com.dev.julian09.financeapp.ui.feature.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.R
import com.dev.julian09.financeapp.domain.finance.FoxMood
import com.dev.julian09.financeapp.domain.finance.FoxState
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.CardDue
import com.dev.julian09.financeapp.domain.model.DuePayment
import com.dev.julian09.financeapp.domain.model.LoanDue
import com.dev.julian09.financeapp.domain.model.SyncStatus
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.model.Urgency
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.FabClearance
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.SyncBadge
import com.dev.julian09.financeapp.ui.components.accountIcon
import com.dev.julian09.financeapp.ui.components.accountTypeShortLabel
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.feature.cards.PayCardSheet
import com.dev.julian09.financeapp.ui.feature.history.DayGroup
import com.dev.julian09.financeapp.ui.feature.loans.LoanPaymentSheet
import com.dev.julian09.financeapp.ui.feature.loans.LoanSheet
import com.dev.julian09.financeapp.ui.feature.recurring.PendingSheets
import com.dev.julian09.financeapp.ui.feature.recurring.dayMonth
import com.dev.julian09.financeapp.ui.feature.recurring.dueSentence
import com.dev.julian09.financeapp.ui.feature.recurring.urgencyTint
import com.dev.julian09.financeapp.ui.feature.tags.TagRow
import com.dev.julian09.financeapp.ui.navigation.PendingLink
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

private val ES_CO: Locale = Locale.forLanguageTag("es-CO")

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAccount: (UUID) -> Unit,
    onNewTransaction: (TransactionType) -> Unit,
    onOpenTransaction: (UUID) -> Unit,
    onOpenRecurring: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTags: () -> Unit = {},
    onOpenTag: (UUID) -> Unit = {},
    userName: String? = null,
    syncStatus: SyncStatus? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var paying by rememberSaveable { mutableStateOf<String?>(null) }
    val link by PendingLink.recurringId.collectAsStateWithLifecycle()
    val cardLink by PendingLink.cardId.collectAsStateWithLifecycle()
    val loanLink by PendingLink.loanId.collectAsStateWithLifecycle()
    var loanPaying by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(link) {
        link?.let {
            pending = it.toString()
            PendingLink.recurringId.value = null
        }
    }
    LaunchedEffect(cardLink) {
        cardLink?.let {
            paying = it.toString()
            PendingLink.cardId.value = null
        }
    }
    PendingSheets(pending?.let(UUID::fromString), onDismiss = { pending = null })
    PayCardSheet(paying?.let(UUID::fromString), onDismiss = { paying = null })
    LaunchedEffect(loanLink) {
        loanLink?.let {
            loanPaying = it.toString()
            PendingLink.loanId.value = null
        }
    }
    LoanPaymentSheet(loanPaying?.let(UUID::fromString), LoanSheet.PAY, onDismiss = { loanPaying = null })
    HomeContent(
        state,
        onOpenSettings,
        onOpenHistory,
        onOpenAccount,
        modifier,
        userName,
        syncStatus,
        onNewTransaction = onNewTransaction,
        onOpenTransaction = onOpenTransaction,
        onOpenRecurring = onOpenRecurring,
        onOpenTags = onOpenTags,
        onOpenTag = onOpenTag,
        onOpenPending = { p ->
            when (p) {
                is CardDue -> paying = p.id.toString()
                is LoanDue -> loanPaying = p.id.toString()
                else -> pending = p.id.toString()
            }
        },
    )
}

/** Inicio (S4-UI04, S6-UI02, S9-UI02): customization arrives in S11. */
@Composable
fun HomeContent(
    state: HomeUiState?,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAccount: (UUID) -> Unit,
    modifier: Modifier = Modifier,
    userName: String? = null,
    syncStatus: SyncStatus? = null,
    today: LocalDate = LocalDate.now(),
    onNewTransaction: (TransactionType) -> Unit = {},
    onOpenTransaction: (UUID) -> Unit = {},
    onOpenRecurring: () -> Unit = {},
    onOpenTags: () -> Unit = {},
    onOpenTag: (UUID) -> Unit = {},
    onOpenPending: (DuePayment) -> Unit = {},
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(
            title = "Inicio",
            actions = {
                syncStatus?.let { SyncBadge(it) }
                Avatar(onClick = onOpenSettings, initial = userName?.firstOrNull())
            },
        )
        if (state == null) return@Column
        val dashboard = state.dashboard
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = FabClearance),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Bienvenido de nuevo", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    Text(
                        "Hola, ${userName?.substringBefore(' ') ?: ""}".trimEnd(',', ' '),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary,
                    )
                }
                StatusChip(dashboard.fox)
            }
            AppCard(Modifier.fillMaxWidth(), highlight = true) {
                Text("Saldo total disponible", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                MoneyText(dashboard.available, style = MaterialTheme.typography.displaySmall)
                dashboard.monthlyChange?.let { MonthlyIndicator(it, dashboard.previousMonth) }
            }
            FoxCard(dashboard.fox, dashboard.previousMonth)
            QuickActions(onNewTransaction)
            val active = dashboard.accounts.filter { it.account.active }
            SectionTitle("Tus cuentas", if (active.size == 1) "1 activa" else "${active.size} activas")
            LazyRow(contentPadding = PaddingValues(end = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(active, key = { it.account.id }) { AccountChipCard(it, onClick = { onOpenAccount(it.account.id) }) }
            }
            if (dashboard.pending.isNotEmpty()) UpcomingPayments(dashboard.pending, onOpenRecurring, onOpenPending)
            if (state.pinned.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Etiquetas fijadas", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    TextButton(onClick = onOpenTags) { Text("Ver todas", color = colors.textSecondary) }
                }
                AppCard {
                    state.pinned.forEach { line -> TagRow(line, onOpen = { onOpenTag(line.tag.id) }) }
                }
            }
            if (state.days.isNotEmpty()) {
                SectionTitle("Últimos 5 días", null)
                state.days.forEach { DayGroup(it, today, onOpen = onOpenTransaction) }
            }
            AppButton(
                "Ver todas las transacciones",
                onClick = onOpenHistory,
                kind = ButtonKind.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    detail: String?,
) {
    val colors = CofferTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
    }
}

@Composable
private fun StatusChip(mood: FoxMood) {
    val colors = CofferTheme.colors
    val (label, tint) =
        when (mood.state) {
            FoxState.RESTING -> "Nuevo" to colors.onPrimaryContainer
            FoxState.DEBT -> "En rojo" to colors.expense
            FoxState.ATTENTIVE -> paymentChip(mood.payment!!.daysLeft) to colors.warning
            FoxState.WORRIED -> "Revisa" to colors.warning
            else -> "Al día" to colors.onPrimaryContainer
        }
    Surface(shape = CofferShapes.pill, color = colors.primaryContainer) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

private fun paymentChip(daysLeft: Long) =
    when {
        daysLeft < 0 -> "Pago vencido"
        daysLeft == 0L -> "Pago hoy"
        daysLeft == 1L -> "Pago mañana"
        else -> "Pago en $daysLeft días"
    }

/** Próximos pagos (S6-UI02): only the ones inside the reminder window (PO, 2026-10-18); they never touch the balance. */
@Composable
private fun UpcomingPayments(
    pending: List<DuePayment>,
    onOpenRecurring: () -> Unit,
    onOpen: (DuePayment) -> Unit,
) {
    val colors = CofferTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Próximos pagos", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            TextButton(onClick = onOpenRecurring) { Text("Gestionar recurrentes →", color = colors.primary) }
        }
        Text(
            "Avisos automáticos · No afectan tu saldo disponible",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        pending.forEach { p ->
            AppCard(Modifier.fillMaxWidth(), onClick = { onOpen(p) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(8.dp).background(urgencyTint(p), CircleShape))
                    Column(Modifier.weight(1f)) {
                        Text(p.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, maxLines = 1)
                        Text(
                            "${dueSentence(p.daysLeft)} · ${dayMonth(p.dueDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = urgencyTint(p),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            formatMoney(p.amount),
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                        )
                        Text(
                            when (p.urgency) {
                                Urgency.OVERDUE -> "Vencido"
                                Urgency.SOON -> "Pendiente"
                                Urgency.UPCOMING -> "Próximo"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

private fun percent(value: BigDecimal): String = DecimalFormat("#,##0.#", DecimalFormatSymbols(ES_CO)).format(value.abs())

fun monthName(month: YearMonth): String = month.month.getDisplayName(TextStyle.FULL, ES_CO)

@Composable
private fun MonthlyIndicator(
    change: BigDecimal,
    previousMonth: YearMonth,
) {
    val colors = CofferTheme.colors
    val month = monthName(previousMonth)
    val (icon, text, tint) =
        when (change.signum()) {
            1 -> Triple(Icons.AutoMirrored.Outlined.TrendingUp, "${percent(change)} % más que en $month", colors.income)
            -1 -> Triple(Icons.AutoMirrored.Outlined.TrendingDown, "${percent(change)} % menos que en $month", colors.expense)
            else -> Triple(null, "Igual que en $month", colors.textSecondary)
        }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        icon?.let { Icon(it, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

/** The mascot and its message (rules: FoxRules; texts: Figma "Estado del zorro"). */
@Composable
fun FoxCard(
    mood: FoxMood,
    previousMonth: YearMonth,
) {
    val colors = CofferTheme.colors
    val month = monthName(previousMonth)
    val (image, title, message) =
        when (mood.state) {
            FoxState.RESTING ->
                Triple(
                    R.drawable.mascot_resting,
                    "Aún no hay movimientos",
                    "Registra un gasto o un ingreso con el botón +. El zorro te irá contando cómo vas.",
                )
            FoxState.DEBT ->
                Triple(
                    R.drawable.mascot_debt,
                    "Tu saldo está en rojo",
                    "Tu saldo disponible es ${formatMoney(mood.amount!!)}. Revisa tus gastos.",
                )
            FoxState.ATTENTIVE ->
                mood.payment!!.let { p ->
                    Triple(
                        R.drawable.mascot_attentive,
                        "Tienes un pago cerca",
                        "${p.title}: ${dueSentence(p.daysLeft).replaceFirstChar { it.lowercase() }} · " +
                            formatMoney(p.amount) + ".",
                    )
                }
            FoxState.WORRIED ->
                Triple(
                    R.drawable.mascot_worried,
                    "Gastas más de lo que entra",
                    "Gastos ${formatMoney(mood.month!!.expense)} · ingresos ${formatMoney(mood.month.income)} este mes.",
                )
            FoxState.GOAL ->
                Triple(
                    R.drawable.mascot_goal,
                    "¡Mes cerrado en positivo!",
                    "Ahorraste ${formatMoney(mood.amount!!)} en $month.",
                )
            FoxState.GOOD ->
                Triple(R.drawable.mascot_good, "¡Vas muy bien!", "Llevas ${percent(mood.change!!)} % más que en $month. Sigue así.")
            FoxState.NORMAL -> Triple(R.drawable.mascot_normal, "Todo en orden", "Tus gastos van como de costumbre este mes.")
        }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Mascot(image)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun Mascot(
    @DrawableRes image: Int,
) {
    Image(painterResource(image), contentDescription = null, modifier = Modifier.size(72.dp))
}

/** One tap to each form; the transfer is one tap from Home (REQ-TRX-04). */
@Composable
private fun QuickActions(onNew: (TransactionType) -> Unit) {
    val colors = CofferTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickAction("Gasto", Icons.Outlined.ArrowDownward, colors.expense, Modifier.weight(1f)) { onNew(TransactionType.EXPENSE) }
        QuickAction("Ingreso", Icons.Outlined.ArrowUpward, colors.income, Modifier.weight(1f)) { onNew(TransactionType.INCOME) }
        QuickAction("Transferencia", Icons.Outlined.SwapHoriz, colors.textSecondary, Modifier.weight(1f)) {
            onNew(TransactionType.TRANSFER)
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = CofferTheme.colors
    AppCard(modifier, onClick = onClick) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(32.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.textPrimary, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun AccountChipCard(
    item: AccountBalance,
    onClick: () -> Unit,
) {
    val colors = CofferTheme.colors
    AppCard(Modifier.width(150.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                accountTypeShortLabel(item.account.type),
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
                maxLines = 1,
            )
            Box(Modifier.size(28.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    accountIcon(item.account.icon, item.account.type),
                    contentDescription = null,
                    tint = colors.iconOnContainer,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(item.account.name, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1)
        MoneyText(item.balance, style = MaterialTheme.typography.titleMedium)
    }
}
