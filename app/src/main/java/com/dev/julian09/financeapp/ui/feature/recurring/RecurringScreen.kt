package com.dev.julian09.financeapp.ui.feature.recurring

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.DuePayment
import com.dev.julian09.financeapp.domain.model.LoanDue
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
    // S15-UI09: a tap on the row opens Hoja · Pendiente (Pagar, Omitir, Finalizar), as in Figma; one sheet at a time
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
            viewModel::skip,
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
    val onSkip: (UUID) -> Unit = {},
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
            AppCard(Modifier.fillMaxWidth(), highlight = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Compromisos de ${monthName(state.month)}",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    StatusPill(
                        if (summary.overdueCount == 0) "Al día" else "${summary.overdueCount} vencidos",
                        if (summary.overdueCount == 0) colors.onPrimaryContainer else colors.expense,
                    )
                }
                Text(formatMoney(summary.committed), style = MaterialTheme.typography.displaySmall, color = colors.textPrimary)
                Text("Presupuesto programado para este mes", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SummaryFigure(
                        if (summary.pendingCount == 1) "1 pendiente" else "${summary.pendingCount} pendientes",
                        "${formatMoney(summary.remaining)} restante",
                        Modifier.weight(1f),
                    )
                    SummaryFigure(
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
                    Text("Pagos próximos", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text("Orden por vencimiento", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                shownCards.forEach { DueRow(it, if (it is LoanDue) actions.onPayLoan else actions.onPayCard) }
                shown.forEach { UpcomingRow(it, state.categories, actions) }
            }
            if (overview.paid.isNotEmpty() && (state.filter == RecurringFilter.ALL || state.filter == RecurringFilter.PAID)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pagados este mes", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text(
                        if (overview.paid.size == 1) "1 registrado" else "${overview.paid.size} registrados",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.income,
                    )
                }
                overview.paid.forEach { PaidRow(it, state.categories, zone, actions.onOpenTransaction) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
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

@Composable
private fun StatusPill(
    label: String,
    tint: Color,
) {
    Surface(shape = CofferShapes.pill, color = CofferTheme.colors.primaryContainer) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
private fun SummaryFigure(
    title: String,
    detail: String,
    modifier: Modifier,
) {
    val colors = CofferTheme.colors
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    }
}

private fun iconOf(
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
    AppCard(Modifier.fillMaxWidth(), onClick = { actions.onOpen(r.id) }) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(urgencyTint(pending), CofferShapes.pill))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircleIcon(categoryIcon(iconOf(r.categoryId, categories)))
                    Column(Modifier.weight(1f)) {
                        Text(r.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1)
                        Text(
                            "Vence ${LONG_DATE.format(r.nextDueDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatMoney(r.suggestedAmount), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text(dueLabel(pending.daysLeft), style = MaterialTheme.typography.labelMedium, color = urgencyTint(pending))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { actions.onSkip(r.id) }) { Text("Omitir", color = colors.textSecondary) }
                    TextButton(onClick = { actions.onEdit(r.id) }) { Text("Editar", color = colors.textSecondary) }
                    AppButton("Registrar pago", onClick = { actions.onPay(r.id) }, icon = Icons.Outlined.Check)
                }
            }
        }
    }
}

/** The payment of a card statement (S7-UI03): paid from Hoja · Pagar tarjeta, never skipped. */
@Composable
private fun DueRow(
    due: DuePayment,
    onPay: (UUID) -> Unit,
) {
    val colors = CofferTheme.colors
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(urgencyTint(due), CofferShapes.pill))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircleIcon(if (due is LoanDue) Icons.Outlined.AccountBalance else Icons.Outlined.CreditCard)
                    Column(Modifier.weight(1f)) {
                        Text(due.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1)
                        Text(
                            // Figma `1:2674`: «Pago sugerido · Vence 12 de octubre»
                            (if (due is LoanDue) "Cuota" else "Pago sugerido") + " · Vence ${LONG_DATE.format(due.dueDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatMoney(due.amount), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text(dueLabel(due.daysLeft), style = MaterialTheme.typography.labelMedium, color = urgencyTint(due))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    AppButton("Registrar pago", onClick = { onPay(due.id) }, icon = Icons.Outlined.Check)
                }
            }
        }
    }
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
            CircleIcon(categoryIcon(iconOf(t.categoryId, categories)))
            Column(Modifier.weight(1f)) {
                Text(
                    paid.recurringName,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textSecondary,
                    textDecoration = TextDecoration.LineThrough,
                    maxLines = 1,
                )
                Text(
                    "Pagado el ${LONG_DATE.format(t.occurredAt.atZone(zone))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatMoney(t.amount), style = MaterialTheme.typography.titleSmall, color = colors.textSecondary)
                Text("Ver recibo", style = MaterialTheme.typography.labelMedium, color = colors.primary)
            }
        }
    }
}
