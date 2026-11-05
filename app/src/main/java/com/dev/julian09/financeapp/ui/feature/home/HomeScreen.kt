package com.dev.julian09.financeapp.ui.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.R
import com.dev.julian09.financeapp.domain.finance.FoxMood
import com.dev.julian09.financeapp.domain.finance.FoxState
import com.dev.julian09.financeapp.domain.finance.RecurrenceCalculator
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.CardDue
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.DuePayment
import com.dev.julian09.financeapp.domain.model.HomeSection
import com.dev.julian09.financeapp.domain.model.LoanDue
import com.dev.julian09.financeapp.domain.model.NetBalanceMode
import com.dev.julian09.financeapp.domain.model.PendingPayment
import com.dev.julian09.financeapp.domain.model.SyncStatus
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.model.Urgency
import com.dev.julian09.financeapp.domain.model.homeLayout
import com.dev.julian09.financeapp.ui.components.AnimatedMoneyText
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.FabClearance
import com.dev.julian09.financeapp.ui.components.FoxMascot
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.SyncBadge
import com.dev.julian09.financeapp.ui.components.accountIcon
import com.dev.julian09.financeapp.ui.components.categoryIcon
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.feature.cards.PayCardSheet
import com.dev.julian09.financeapp.ui.feature.history.DayGroup
import com.dev.julian09.financeapp.ui.feature.history.HistoryDay
import com.dev.julian09.financeapp.ui.feature.loans.LoanPaymentSheet
import com.dev.julian09.financeapp.ui.feature.loans.LoanSheet
import com.dev.julian09.financeapp.ui.feature.recurring.PendingSheets
import com.dev.julian09.financeapp.ui.feature.recurring.dayMonth
import com.dev.julian09.financeapp.ui.feature.recurring.dueSentence
import com.dev.julian09.financeapp.ui.feature.recurring.iconOf
import com.dev.julian09.financeapp.ui.feature.recurring.urgencyTint
import com.dev.julian09.financeapp.ui.feature.tags.TagRow
import com.dev.julian09.financeapp.ui.navigation.PendingLink
import com.dev.julian09.financeapp.ui.root.FirstContent
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
    onCustomize: () -> Unit = {},
    userName: String? = null,
    syncStatus: SyncStatus? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state != null) SideEffect { FirstContent.shown = true }
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
        onBalanceView = viewModel::setBalanceView,
        onRegisteredSeen = viewModel::markRegisteredSeen,
        onNetMode = viewModel::setNetMode,
        onIncludeInNet = viewModel::setIncludeInNet,
        onCustomize = onCustomize,
        onOpenPending = { p ->
            when (p) {
                is CardDue -> paying = p.id.toString()
                is LoanDue -> loanPaying = p.id.toString()
                else -> pending = p.id.toString()
            }
        },
    )
}

/** Inicio (S4-UI04, S6-UI02, S9-UI02, S11-UI04): the cards follow Personalizar inicio. */
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
    onBalanceView: (BalanceView) -> Unit = {},
    onRegisteredSeen: () -> Unit = {},
    onNetMode: (NetBalanceMode) -> Unit = {},
    onIncludeInNet: (UUID, Boolean) -> Unit = { _, _ -> },
    onCustomize: () -> Unit = {},
) {
    val colors = CofferTheme.colors
    var balanceSheet by rememberSaveable { mutableStateOf(false) }
    // The sheet keeps what it opened with: opening it already marks the notice as seen
    var registeredSheet by remember { mutableStateOf<List<HistoryDay>?>(null) }
    val openRegistered = {
        registeredSheet = state?.registered
        onRegisteredSeen()
    }
    var balanceHidden by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        AppHeader(
            title = "Inicio",
            logo = true,
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
                    Text(
                        "Hola, ${userName?.substringBefore(' ') ?: ""}".trimEnd(',', ' '),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary,
                    )
                }
                StatusChip(dashboard.fox)
            }
            FoxCard(dashboard.fox, dashboard.previousMonth, dashboard.monthlyChange)
            // REQ-INI-04: the cards in the order and with the visibility of Personalizar inicio
            val layout = homeLayout(state.settings.homeCards)
            val trend = layout.any { it.key == HomeSection.MONTHLY_TREND.name && it.visible }
            val visible = layout.filter { it.visible }
            // «Por revisar» goes under the balance whatever cards are shown (changes-ui-2.0 §1)
            val balanceShown = visible.any { it.key == HomeSection.BALANCE.name }
            if (!balanceShown) DraftsNotice(state.draftCount, state.registeredCount, onOpenHistory, openRegistered)
            visible.forEach { card ->
                when (runCatching { HomeSection.valueOf(card.key) }.getOrNull()) {
                    HomeSection.BALANCE -> {
                        // REQ-CTA-09: the chosen view; a tap shows both
                        AppCard(Modifier.fillMaxWidth(), highlight = true, onClick = { balanceSheet = true }) {
                            val net = state.balanceView == BalanceView.NET
                            // NAV-APP-001 (Figma `1:997`): 24 dp inside, label 12, amount 10 dp below, the indicator 8 dp below
                            // it; the eye's 44 dp target overlaps the corner instead of making the label's row taller
                            Box(Modifier.fillMaxWidth().padding(4.dp)) {
                                Column {
                                    Text(
                                        if (net) "Saldo neto" else "Saldo total disponible",
                                        Modifier.padding(end = 32.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.textSecondary,
                                    )
                                    Box(Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                                        if (balanceHidden) {
                                            Text("$ ••••••", style = MaterialTheme.typography.displaySmall, color = colors.textPrimary)
                                        } else {
                                            AnimatedMoneyText(
                                                if (net) state.net else dashboard.available,
                                                style = MaterialTheme.typography.displaySmall,
                                            )
                                        }
                                    }
                                    if (!balanceHidden && trend) {
                                        dashboard.monthlyChange?.let {
                                            Box(Modifier.padding(top = 4.dp)) { MonthlyIndicator(it, dashboard.previousMonth) }
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { balanceHidden = !balanceHidden },
                                    Modifier.align(Alignment.TopEnd).size(44.dp).offset(x = 12.dp, y = (-14).dp),
                                ) {
                                    Icon(
                                        if (balanceHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (balanceHidden) "Mostrar el saldo" else "Ocultar el saldo",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                        DraftsNotice(state.draftCount, state.registeredCount, onOpenHistory, openRegistered)
                    }
                    HomeSection.QUICK_ACTIONS -> {
                        QuickActions(onNewTransaction)
                    }
                    HomeSection.ACCOUNTS -> {
                        val active = dashboard.accounts.filter { it.account.active && it.account.type.isPickable }
                        SectionTitle("Tus cuentas", if (active.size == 1) "1 activa" else "${active.size} activas")
                        LazyRow(contentPadding = PaddingValues(end = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(active, key = { it.account.id }) { AccountChipCard(it, onClick = { onOpenAccount(it.account.id) }) }
                        }
                    }
                    HomeSection.PENDING ->
                        if (dashboard.pending.isNotEmpty()) {
                            UpcomingPayments(
                                dashboard.pending,
                                state.categories,
                                onOpenRecurring,
                                onOpenPending,
                            )
                        }
                    HomeSection.PINNED_TAGS ->
                        if (state.pinned.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Etiquetas fijadas",
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                )
                                TextButton(onClick = onOpenTags) { Text("Ver todas", color = colors.textSecondary) }
                            }
                            AppCard {
                                state.pinned.forEach { line -> TagRow(line, onOpen = { onOpenTag(line.tag.id) }) }
                            }
                        }
                    HomeSection.RECENT ->
                        if (state.days.isNotEmpty()) {
                            SectionTitle("Últimos 5 días", null)
                            state.days.forEach { DayGroup(it, today, onOpen = onOpenTransaction) }
                        }
                    HomeSection.MONTHLY_TREND, null -> Unit
                }
            }
            registeredSheet?.let { days ->
                RegisteredSheet(days, today, onOpenTransaction, onDismiss = { registeredSheet = null })
            }
            if (balanceSheet) {
                BalanceSheet(
                    dashboard,
                    state.balanceView,
                    state.settings.netBalanceMode,
                    onBalanceView,
                    onNetMode,
                    onIncludeInNet,
                    onDismiss = { balanceSheet = false },
                )
            }
            AppButton(
                "Ver todas las transacciones",
                onClick = onOpenHistory,
                kind = ButtonKind.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onCustomize, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Outlined.Edit, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                Text(" Personalizar inicio", color = colors.textSecondary)
            }
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
    categories: List<Category>,
    onOpenRecurring: () -> Unit,
    onOpen: (DuePayment) -> Unit,
) {
    val colors = CofferTheme.colors
    val icon = { p: DuePayment ->
        when (p) {
            is CardDue -> Icons.Outlined.CreditCard
            is LoanDue -> Icons.Outlined.AccountBalance
            is PendingPayment -> categoryIcon(iconOf(p.recurring.categoryId, categories))
        }
    }
    // 2.2: one summary card; a tap unfolds the list (the state is remembered between visits)
    var expanded by rememberSaveable { mutableStateOf(false) }
    val closest = pending.minBy { it.daysLeft }
    val soon = closest.daysLeft <= RecurrenceCalculator.SOON_DAYS
    val shape = RoundedCornerShape(12.dp)
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, tween(300), label = "chevron")
    val accent = if (soon) colors.expense else colors.iconOnContainer
    // #224: Figma's pinks are light-only; in dark the warning tints the dark surface so the title stays readable
    val dark = colors.background.luminance() < 0.5f
    val soonFill = if (dark) lerp(colors.surface, colors.expense, 0.14f) else Color(0xFFFBEDEB)
    val soonBorder = if (dark) lerp(colors.surface, colors.expense, 0.35f) else Color(0xFFEDC4BE)
    val soonBubble = if (dark) lerp(colors.surface, colors.expense, 0.25f) else Color(0xFFF5D7D2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .clip(shape)
                .background(if (soon) soonFill else colors.surface, shape)
                .border(1.dp, if (soon) soonBorder else colors.border, shape)
                .clickable(onClickLabel = if (expanded) "Plegar los próximos pagos" else "Ver los próximos pagos") { expanded = !expanded }
                .padding(start = 12.dp, top = 12.dp, end = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(36.dp).background(if (soon) soonBubble else colors.iconContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Próximos pagos · ${pending.size}", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                val dueText = dueSentence(closest.daysLeft).replaceFirstChar { it.lowercase() }
                Text(
                    if (soon) "${closest.title} $dueText · ${formatMoney(closest.amount)}" else "El próximo: ${closest.title} $dueText",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (soon) accent else colors.textSecondary,
                    maxLines = 1,
                )
            }
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.rotate(chevron),
            )
        }
        AnimatedVisibility(
            expanded,
            enter = expandVertically(tween(300)) + fadeIn(tween(300)),
            exit = shrinkVertically(tween(300)) + fadeOut(tween(300)),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // NAV-APP-016 (Figma `182:1330`): one card with a row per payment, the notice and the link under it
                AppCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                    pending.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 12.dp), thickness = 1.dp, color = colors.border)
                        UpcomingRow(p, icon(p), onClick = { onOpen(p) })
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            colors.background,
                            RoundedCornerShape(8.dp),
                        ).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(13.dp))
                    Text(
                        "Avisos automáticos",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                        color = colors.textSecondary,
                    )
                }
                TextButton(onClick = onOpenRecurring, modifier = Modifier.align(Alignment.End)) {
                    Text("Gestionar recurrentes →", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun UpcomingRow(
    p: DuePayment,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val colors = CofferTheme.colors
    val tint = urgencyTint(p)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp)) {
            Box(Modifier.fillMaxSize().background(colors.iconContainer, CofferShapes.card), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            // the urgency dot on the corner, only while the payment needs attention
            if (tint != colors.textSecondary) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-3).dp)
                        .size(10.dp)
                        .background(colors.surface, CircleShape)
                        .padding(2.dp)
                        .background(tint, CircleShape),
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(p.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    dueSentence(p.daysLeft),
                    Modifier.background(colors.background, CofferShapes.pill).padding(horizontal = 6.dp),
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                    color = tint,
                )
                Text(
                    "· ${dayMonth(p.dueDate)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal, lineHeight = 14.sp),
                    color = colors.textSecondary,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formatMoney(p.amount), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(
                when {
                    p.urgency == Urgency.OVERDUE -> "Vencido"
                    p.urgency == Urgency.SOON -> "Pendiente"
                    p.inWindow -> "Próximo"
                    else -> "Programado"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal, lineHeight = 14.sp),
                color = tint,
            )
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
    monthlyChange: BigDecimal? = null,
) {
    val colors = CofferTheme.colors
    val month = monthName(previousMonth)
    val (_, title, message) =
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
                    // CMP-APP-001 (Figma «Estado del zorro»): «Ojo con el arriendo · Vence mañana y son $900.000.»
                    Triple(
                        R.drawable.mascot_attentive,
                        "Ojo con ${p.title}",
                        "${dueSentence(p.daysLeft)} y son ${formatMoney(p.amount)}.",
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
                Triple(
                    R.drawable.mascot_good,
                    "¡Vas muy bien!",
                    monthlyChange?.takeIf { it.signum() > 0 }?.let { "Llevas ${percent(it)} % más que en $month. Sigue así." }
                        ?: "Este mes llevas ${formatMoney(mood.month!!.income - mood.month.expense)} más de ingresos que de gastos.",
                )
            FoxState.NORMAL -> Triple(R.drawable.mascot_normal, "Todo en orden", "Tus gastos van como de costumbre este mes.")
        }
    // M13 (#96): no card; the fox stands on the background and over the top edge of the card below it
    Row(Modifier.fillMaxWidth().zIndex(1f), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
        // FEATURE-09: the animated fox of the month's mood
        FoxMascot(mood.state.name.lowercase(), Modifier.offset(y = FOX_OVERLAP), size = 92.dp)
    }
}

// Figma «Estado del zorro»: 92 dp, 12 dp over the top edge of the balance card: the 16 dp gap between blocks plus those 12 dp
private val FOX_OVERLAP = 28.dp

/** One tap to each form; the transfer is one tap from Home (REQ-TRX-04). M12 (#95): a thin bar of three segments. */
@Composable
private fun QuickActions(onNew: (TransactionType) -> Unit) {
    val colors = CofferTheme.colors
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(colors.iconContainer, CofferShapes.card),
        verticalAlignment = Alignment.CenterVertically,
        // S12-UI11: each segment as wide as its label plus the same padding; «Transferencia» no longer touches the edge
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        QuickAction(
            "Gasto",
            Icons.Outlined.ArrowDownward,
            colors.expense,
            Modifier,
        ) { onNew(TransactionType.EXPENSE) }
        VerticalDivider(Modifier.padding(vertical = 10.dp), thickness = 1.dp, color = colors.border)
        QuickAction(
            "Ingreso",
            Icons.Outlined.ArrowUpward,
            colors.income,
            Modifier,
        ) { onNew(TransactionType.INCOME) }
        VerticalDivider(Modifier.padding(vertical = 10.dp), thickness = 1.dp, color = colors.border)
        QuickAction("Transferencia", Icons.Outlined.SwapHoriz, colors.textSecondary, Modifier) {
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
    Row(
        modifier.clip(CofferShapes.card).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = CofferTheme.colors.textPrimary,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun AccountChipCard(
    item: AccountBalance,
    onClick: () -> Unit,
) {
    val colors = CofferTheme.colors
    // 2.2: a low horizontal tile (56 dp): icon, name and balance; the type label is gone
    AppCard(Modifier.heightIn(min = 56.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(28.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    accountIcon(item.account.icon, item.account.type),
                    contentDescription = null,
                    tint = colors.iconOnContainer,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column {
                Text(item.account.name, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary, maxLines = 1)
                MoneyText(item.balance, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/**
 * «Aviso - Por revisar» and «Aviso - Registrados» (2.4 §8): under the balance, only with something to tell. Both at once are
 * one card of one colour: to review above, saved on their own below, each with its own 44 dp touch area.
 */
@Composable
private fun DraftsNotice(
    count: Int,
    registered: Int,
    onOpenReview: () -> Unit,
    onOpenRegistered: () -> Unit,
) {
    val colors = CofferTheme.colors
    // motion.md §7: it grows in and out, and its count cross-fades
    AnimatedVisibility(
        count > 0 || registered > 0,
        enter = expandVertically(tween(400)) + fadeIn(tween(400)),
        exit = shrinkVertically(tween(400)) + fadeOut(tween(400)),
    ) {
        val both = count > 0 && registered > 0
        Column(
            Modifier
                .fillMaxWidth()
                .clip(CofferShapes.card)
                .background(if (registered > 0 && count == 0) colors.iconContainer else colors.primaryContainer),
        ) {
            if (count > 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable(role = Role.Button, onClick = onOpenReview)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Email, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
                    Crossfade(count, Modifier.weight(1f), animationSpec = tween(150), label = "drafts") { n ->
                        Text(
                            if (n == 1) "1 movimiento por revisar" else "$n movimientos por revisar",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onPrimaryContainer,
                        )
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.onPrimaryContainer)
                }
            }
            if (both) HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = Color(0xFFE6D3C2))
            if (registered > 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable(role = Role.Button, onClick = onOpenRegistered)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = Color(0xFF7A4520), modifier = Modifier.size(16.dp))
                    Text(
                        if (registered == 1) "1 movimiento registrado" else "$registered movimientos registrados",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onPrimaryContainer,
                    )
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF7A4520),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/** Hoja · Movimientos registrados (2.4 §3): what came in on its own, to look at without approving anything. */
@Composable
private fun RegisteredSheet(
    days: List<HistoryDay>,
    today: LocalDate,
    onOpen: (UUID) -> Unit,
    onDismiss: () -> Unit,
) {
    val count = days.sumOf { it.rows.size }
    AppBottomSheet(
        onDismiss = onDismiss,
        title = if (count == 1) "1 movimiento registrado" else "$count movimientos registrados",
        subtitle = "Llegaron por correo y ya están en tu historial.",
        actions = { AppButton("Entendido", onClick = onDismiss, modifier = Modifier.fillMaxWidth()) },
    ) {
        days.forEach {
            DayGroup(it, today, showTime = true, onOpen = { id ->
                onDismiss()
                onOpen(id)
            })
        }
    }
}
