package com.dev.julian09.financeapp.ui.feature.recurring

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.DuePayment
import com.dev.julian09.financeapp.domain.model.LoanDue
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.PendingPayment
import com.dev.julian09.financeapp.domain.model.RecurringPayment
import com.dev.julian09.financeapp.domain.model.Urgency
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppChip
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.EmptyState
import com.dev.julian09.financeapp.ui.components.categoryIcon
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.feature.cards.PayCardSheet
import com.dev.julian09.financeapp.ui.feature.home.monthName
import com.dev.julian09.financeapp.ui.feature.loans.LoanPaymentSheet
import com.dev.julian09.financeapp.ui.feature.loans.LoanSheet
import com.dev.julian09.financeapp.ui.feature.transactions.CircleIcon
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val ES_CO: Locale = Locale.forLanguageTag("es-CO")
private val LONG_DATE = DateTimeFormatter.ofPattern("d 'de' MMMM", ES_CO)

/** "En 2 días", "Mañana", "Hoy", "Venció hace 3 días". */
fun dueLabel(daysLeft: Long): String =
    when {
        daysLeft < -1 -> "Venció hace ${-daysLeft} días"
        daysLeft == -1L -> "Venció ayer"
        daysLeft == 0L -> "Hoy"
        daysLeft == 1L -> "Mañana"
        else -> "En $daysLeft días"
    }

/** design-system: secondary when far, warning inside the window, danger when due in ≤ 2 days or overdue. */
@Composable
fun urgencyTint(pending: DuePayment): Color {
    val colors = CofferTheme.colors
    return when {
        pending.urgency != Urgency.UPCOMING -> colors.expense
        pending.inWindow -> colors.warning
        else -> colors.textSecondary
    }
}

@Composable
fun RecurringScreen(
    onBack: () -> Unit,
    onNew: () -> Unit,
    onEdit: (UUID) -> Unit,
    onOpenTransaction: (UUID) -> Unit,
    viewModel: RecurringViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var paying by rememberSaveable { mutableStateOf<String?>(null) }
    var opening by rememberSaveable { mutableStateOf<String?>(null) }
    var payingCard by rememberSaveable { mutableStateOf<String?>(null) }
    var payingLoan by rememberSaveable { mutableStateOf<String?>(null) }
    // S15-UI09: a tap on the row opens Hoja · Pendiente (Pagar, Finalizar; Omitir is gone since 2.0), as in Figma; one sheet at a time
    PendingSheets(
        (paying ?: opening)?.let(UUID::fromString),
        onDismiss = {
            paying = null
            opening = null
        },
        payNow = paying != null,
    )
    PayCardSheet(payingCard?.let(UUID::fromString), onDismiss = { payingCard = null })
    LoanPaymentSheet(payingLoan?.let(UUID::fromString), LoanSheet.PAY, onDismiss = { payingLoan = null })
    RecurringContent(
        state,
        RecurringActions(
            onBack,
            onNew,
            onEdit,
            onOpenTransaction,
            viewModel::setFilter,
            onOpen = { opening = it.toString() },
            onPay = { paying = it.toString() },
            onPayCard = { payingCard = it.toString() },
            onPayLoan = { payingLoan = it.toString() },
        ),
    )
}

class RecurringActions(
    val onBack: () -> Unit = {},
    val onNew: () -> Unit = {},
    val onEdit: (UUID) -> Unit = {},
    val onOpenTransaction: (UUID) -> Unit = {},
    val onFilter: (RecurringFilter) -> Unit = {},
    val onOpen: (UUID) -> Unit = {},
    val onPay: (UUID) -> Unit = {},
    val onPayCard: (UUID) -> Unit = {},
    val onPayLoan: (UUID) -> Unit = {},
)

/** Gastos recurrentes (S6-UI01): the month's commitments, the next payments and what was paid this month. */
@Composable
fun RecurringContent(
    state: RecurringUiState?,
    actions: RecurringActions,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Gastos recurrentes", onBack = actions.onBack)
        if (state == null) return@Column
        val overview = state.overview
        val summary = state.summary
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // REC-APP-001 (Figma `1:2674`): a white card, the pill with its dot and the two figures in their own boxes
            AppCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Compromisos de ${monthName(state.month)}",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                    )
                    StatusPill(
                        if (summary.overdueCount == 0) "Al día" else "${summary.overdueCount} vencidos",
                        if (summary.overdueCount == 0) colors.onPrimaryContainer else colors.expense,
                    )
                }
                Column {
                    Text(formatMoney(summary.committed), style = MaterialTheme.typography.displaySmall, color = colors.textPrimary)
                    Text("Presupuesto programado para este mes", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryFigure(
                        Icons.Outlined.PendingActions,
                        colors.background,
                        if (summary.pendingCount == 1) "1 pendiente" else "${summary.pendingCount} pendientes",
                        "${formatMoney(summary.remaining)} restante",
                        Modifier.weight(1f),
                    )
                    SummaryFigure(
                        Icons.Outlined.TaskAlt,
                        colors.primaryContainer,
                        if (summary.paidCount == 1) "1 cancelado" else "${summary.paidCount} cancelados",
                        "${formatMoney(summary.covered)} cubiertos",
                        Modifier.weight(1f),
                    )
                }
            }
            AppButton(
                "Programar nuevo gasto recurrente",
                onClick = actions.onNew,
                icon = Icons.Outlined.AddCircleOutline,
                modifier = Modifier.fillMaxWidth(),
            )
            val overdue = overview.upcoming.filter { it.urgency == Urgency.OVERDUE }
            val upcoming = overview.upcoming - overdue.toSet()
            val overdueCards = state.dues.filter { it.urgency == Urgency.OVERDUE }
            val upcomingCards = state.dues - overdueCards.toSet()
            val chips =
                listOf(
                    RecurringFilter.ALL to "Todos (${overview.upcoming.size + state.dues.size + overview.paid.size})",
                    RecurringFilter.UPCOMING to "Próximos (${upcoming.size + upcomingCards.size})",
                    RecurringFilter.OVERDUE to "Vencidos (${overdue.size + overdueCards.size})",
                    RecurringFilter.PAID to "Pagados (${overview.paid.size})",
                )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 8.dp)) {
                items(
                    chips,
                    key = { it.first },
                ) { (f, label) -> AppChip(label, selected = state.filter == f, onClick = { actions.onFilter(f) }) }
            }
            val shown =
                when (state.filter) {
                    RecurringFilter.ALL -> overview.upcoming
                    RecurringFilter.UPCOMING -> upcoming
                    RecurringFilter.OVERDUE -> overdue
                    RecurringFilter.PAID -> emptyList()
                }
            val shownCards =
                when (state.filter) {
                    RecurringFilter.ALL -> state.dues
                    RecurringFilter.UPCOMING -> upcomingCards
                    RecurringFilter.OVERDUE -> overdueCards
                    RecurringFilter.PAID -> emptyList()
                }
            if (overview.upcoming.isEmpty() && overview.paid.isEmpty()) {
                EmptyState(
                    title = "Sin gastos recurrentes",
                    message = "Programa el arriendo, los servicios o tus suscripciones y te avisaremos antes de cada pago.",
                )
            }
            if (shown.isNotEmpty() || shownCards.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pagos próximos", Modifier.weight(1f), style = SECTION_TITLE, color = colors.textPrimary)
                    Text("Orden por vencimiento", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                shownCards.forEach { DueRow(it, if (it is LoanDue) actions.onPayLoan else actions.onPayCard) }
                shown.forEach { UpcomingRow(it, state.categories, actions) }
            }
            if (overview.paid.isNotEmpty() && (state.filter == RecurringFilter.ALL || state.filter == RecurringFilter.PAID)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pagados este mes", Modifier.weight(1f), style = SECTION_TITLE, color = colors.textPrimary)
                    Icon(Icons.Outlined.DoneAll, contentDescription = null, tint = colors.income, modifier = Modifier.size(15.dp))
                    Text(
                        " " + if (overview.paid.size == 1) "1 registrado" else "${overview.paid.size} registrados",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.income,
                    )
                }
                overview.paid.forEach { PaidRow(it, state.categories, zone, actions.onOpenTransaction) }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        colors.background,
                        CofferShapes.card,
                    ).border(1.dp, colors.border, CofferShapes.card)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.primary, modifier = Modifier.size(17.dp))
                Text(
                    "Los gastos recurrentes son recordatorios proyectados y no descuentan dinero de tus cuentas " +
                        "hasta que los marques como pagados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

private val SECTION_TITLE = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp)

@Composable
private fun StatusPill(
    label: String,
    tint: Color,
) {
    Row(
        Modifier.background(CofferTheme.colors.primaryContainer, CofferShapes.pill).padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(6.dp).background(tint, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
private fun SummaryFigure(
    icon: ImageVector,
    iconBackground: Color,
    title: String,
    detail: String,
    modifier: Modifier,
) {
    val colors = CofferTheme.colors
    Row(
        modifier.background(colors.background, CofferShapes.card).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(32.dp).background(iconBackground, CircleShape).border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, maxLines = 1)
            Text(
                detail,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

/** REC-APP-002 (Figma `1:2674`): the card with its urgency as a 6 dp edge, the due label in a pill and small actions. */
@Composable
private fun DueCard(
    tint: Color,
    icon: ImageVector,
    title: String,
    subtitle: String,
    amount: Money,
    due: String,
    urgent: Boolean,
    onClick: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onPay: () -> Unit,
) {
    val colors = CofferTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CofferShapes.card,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(tint))
            Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircleIcon(icon)
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1)
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                            color = colors.textSecondary,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(formatMoney(amount), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Row(
                            Modifier.background(colors.background, CofferShapes.pill).padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            if (urgent) Icon(Icons.Outlined.Alarm, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
                            Text(due, style = MaterialTheme.typography.labelSmall, color = tint)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)) {
                    onEdit?.let {
                        Text(
                            "Editar",
                            Modifier
                                .clip(
                                    RoundedCornerShape(8.dp),
                                ).clickable(role = Role.Button, onClick = it)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                        )
                    }
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primary)
                            .clickable(role = Role.Button, onClick = onPay)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(14.dp))
                        Text("Registrar pago", style = MaterialTheme.typography.labelMedium, color = colors.onPrimary)
                    }
                }
            }
        }
    }
}

internal fun iconOf(
    categoryId: UUID?,
    categories: List<Category>,
) = categories.firstOrNull { it.id == categoryId }?.let { c -> c.icon ?: categories.firstOrNull { it.id == c.parentId }?.icon }

@Composable
private fun UpcomingRow(
    pending: PendingPayment,
    categories: List<Category>,
    actions: RecurringActions,
) {
    val colors = CofferTheme.colors
    val r = pending.recurring
    DueCard(
        urgencyTint(pending),
        categoryIcon(iconOf(r.categoryId, categories)),
        r.name,
        "Vence ${LONG_DATE.format(r.nextDueDate)}",
        r.suggestedAmount,
        dueLabel(pending.daysLeft),
        urgent = pending.urgency != Urgency.UPCOMING || pending.inWindow,
        onClick = { actions.onOpen(r.id) },
        onEdit = { actions.onEdit(r.id) },
        onPay = { actions.onPay(r.id) },
    )
}

/** The payment of a card statement (S7-UI03): paid from Hoja · Pagar tarjeta, never skipped. */
@Composable
private fun DueRow(
    due: DuePayment,
    onPay: (UUID) -> Unit,
) {
    // REC-APP-002: a card or loan payment is never edited from here
    DueCard(
        urgencyTint(due),
        if (due is LoanDue) Icons.Outlined.AccountBalance else Icons.Outlined.CreditCard,
        due.title,
        // Figma `1:2674`: «Pago sugerido · Vence 12 de octubre»
        (if (due is LoanDue) "Cuota" else "Pago sugerido") + " · Vence ${LONG_DATE.format(due.dueDate)}",
        due.amount,
        dueLabel(due.daysLeft),
        urgent = due.urgency != Urgency.UPCOMING || due.inWindow,
        onClick = null,
        onEdit = null,
        onPay = { onPay(due.id) },
    )
}

@Composable
private fun PaidRow(
    paid: RecurringPayment,
    categories: List<Category>,
    zone: ZoneId,
    onOpen: (UUID) -> Unit,
) {
    val colors = CofferTheme.colors
    val t = paid.transaction
    AppCard(Modifier.fillMaxWidth(), onClick = { onOpen(t.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // REC-APP-003 (Figma `1:2674`): the icon on the selection color, a green check after the name, the amount struck
            Box(Modifier.size(40.dp).background(colors.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    categoryIcon(iconOf(t.categoryId, categories)),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        paid.recurringName,
                        Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textSecondary,
                        textDecoration = TextDecoration.LineThrough,
                        maxLines = 1,
                    )
                    Icon(Icons.Outlined.CheckCircle, contentDescription = "Pagado", tint = colors.income, modifier = Modifier.size(14.dp))
                }
                Text(
                    "Pagado el ${LONG_DATE.format(t.occurredAt.atZone(zone))}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    color = colors.textSecondary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatMoney(t.amount),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textSecondary,
                    textDecoration = TextDecoration.LineThrough,
                )
                Text("Ver recibo", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal), color = colors.primary)
            }
        }
    }
}
