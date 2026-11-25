package com.dev.julian09.financeapp.ui.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.CardOverview
import com.dev.julian09.financeapp.domain.model.DuePayment
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.LoanDue
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.NetBalanceMode
import com.dev.julian09.financeapp.domain.usecase.HomeDashboard
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.SegmentedSelector
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.feature.recurring.dayMonth
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.util.UUID

/**
 * Hoja · Detalle del saldo (Figma `20:168`, `156:149`, `156:256`): both views as cards; a tap picks the one Home shows and
 * saves at once. Inside the net, «Cuotas del periodo» or «Elegir deudas» (S15-UI11).
 */
@Composable
fun BalanceSheet(
    dashboard: HomeDashboard,
    view: BalanceView,
    mode: NetBalanceMode,
    onView: (BalanceView) -> Unit,
    onMode: (NetBalanceMode) -> Unit,
    onIncludeInNet: (UUID, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onPeriodOnly: (UUID, Boolean) -> Unit = { _, _ -> },
    onPurchaseIncluded: (UUID, Boolean) -> Unit = { _, _ -> },
) {
    AppBottomSheet(onDismiss = onDismiss, title = "Detalle del saldo", subtitle = "Toca la tarjeta que quieres ver en inicio.") {
        BalanceViews(dashboard, view, mode, onView, onMode, onIncludeInNet, onPeriodOnly, onPurchaseIncluded)
    }
}

@Composable
internal fun BalanceViews(
    dashboard: HomeDashboard,
    view: BalanceView,
    mode: NetBalanceMode,
    onView: (BalanceView) -> Unit,
    onMode: (NetBalanceMode) -> Unit,
    onIncludeInNet: (UUID, Boolean) -> Unit,
    onPeriodOnly: (UUID, Boolean) -> Unit,
    onPurchaseIncluded: (UUID, Boolean) -> Unit,
) {
    val colors = CofferTheme.colors
    AppCard(
        Modifier.fillMaxWidth().animateContentSize(tween(250)),
        selected = view == BalanceView.AVAILABLE,
        onClick = { onView(BalanceView.AVAILABLE) },
    ) {
        ViewTitle("Saldo disponible", view == BalanceView.AVAILABLE)
        MoneyText(dashboard.available, style = MaterialTheme.typography.headlineMedium)
        Text(
            "Efectivo, débito y ahorros incluidos en el saldo total. Sin tarjetas ni préstamos.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
    AppCard(
        Modifier.fillMaxWidth().animateContentSize(tween(250)),
        selected = view == BalanceView.NET,
        onClick = { onView(BalanceView.NET) },
    ) {
        ViewTitle("Saldo neto", view == BalanceView.NET)
        SegmentedSelector(
            listOf("Cuotas del periodo", "Personalizado"),
            selectedIndex = mode.ordinal,
            onSelect = { onMode(NetBalanceMode.entries[it]) },
        )
        MoneyText(dashboard.net(mode), style = MaterialTheme.typography.headlineMedium)
        Line("Cuentas incluidas") { MoneyText(dashboard.available, style = MaterialTheme.typography.bodyMedium) }
        when (mode) {
            NetBalanceMode.PERIOD ->
                dashboard.dues.forEach { due ->
                    Line(dueLabel(due)) { MoneyText(due.amount, kind = MoneyKind.Expense, style = MaterialTheme.typography.bodyMedium) }
                }
            NetBalanceMode.CUSTOM ->
                dashboard.debts.forEach { debt ->
                    val marked = debt.account.includeInNet
                    val card = dashboard.cards[debt.account.id]
                    val periodOnly = debt.account.id in dashboard.choices.periodCards
                    val label =
                        when {
                            debt.account.type == AccountType.LOAN -> "deuda pendiente"
                            card != null && periodOnly -> "pago del periodo"
                            else -> "deuda total"
                        }
                    val amount = if (marked) dashboard.customDebt(debt) else -debt.balance
                    DebtLine("${debt.account.name} · $label", marked, { onIncludeInNet(debt.account.id, !marked) }) {
                        if (marked) {
                            MoneyText(amount, kind = MoneyKind.Expense, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(formatMoney(amount), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        }
                    }
                    if (marked && card != null) {
                        CardChoices(card, periodOnly, dashboard.choices.excludedPurchases, onPeriodOnly, onPurchaseIncluded)
                    }
                }
        }
        Text(
            when (mode) {
                NetBalanceMode.PERIOD -> "Resta lo que pagas este periodo: el pago de la tarjeta y la cuota del préstamo."
                NetBalanceMode.CUSTOM ->
                    "Marca las deudas que quieres restar del disponible. En cada tarjeta eliges si resta toda la deuda o solo el " +
                        "pago del periodo, y qué compras a cuotas cuentan."
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

/**
 * Under a marked card (2.5, PO 2026-11-24): «Deuda total» or «Solo el periodo», and its installment purchases folded
 * under «Compras a cuotas», each with its own mark and what it adds: what is left to pay, or this period's installment.
 */
@Composable
private fun CardChoices(
    card: CardOverview,
    periodOnly: Boolean,
    excluded: Set<UUID>,
    onPeriodOnly: (UUID, Boolean) -> Unit,
    onPurchaseIncluded: (UUID, Boolean) -> Unit,
) {
    val colors = CofferTheme.colors
    val id = card.card.accountId
    var open by rememberSaveable(id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(start = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentedSelector(
            listOf("Deuda total", "Solo el periodo"),
            selectedIndex = if (periodOnly) 1 else 0,
            onSelect = { onPeriodOnly(id, it == 1) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (card.deferred.isEmpty()) return@Column
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp)
                .clickable(onClickLabel = if (open) "Ocultar" else "Ver") { open = !open },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val inside = card.deferred.count { it.id !in excluded }
            Text(
                "Compras a cuotas · $inside de ${card.deferred.size}",
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            Icon(
                if (open) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        if (open) {
            val dueDate = card.statement.period.dueDate
            card.deferred.forEach { d ->
                val scheduled = card.installments.filter { it.deferredPurchaseId == d.id && it.status == InstallmentStatus.SCHEDULED }
                val next = scheduled.minByOrNull { it.number }
                val adds =
                    if (periodOnly) {
                        scheduled.filter { it.dueDate == dueDate }.fold(Money.ZERO) { sum, i -> sum + i.total }
                    } else {
                        scheduled.fold(Money.ZERO) { sum, i -> sum + i.principal }
                    }
                val included = d.id !in excluded
                val name = card.purchaseNotes[d.id]?.ifBlank { null } ?: "Compra de ${formatMoney(d.amount)}"
                val progress = next?.let { " · cuota ${it.number} de ${d.installmentsCount}" }.orEmpty()
                DebtLine(name + progress, included, { onPurchaseIncluded(d.id, !included) }) {
                    if (included) {
                        MoneyText(adds, kind = MoneyKind.Expense, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(formatMoney(adds), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

/** A debt of «Personalizado»: a round mark (espresso with a check when subtracted, a ring when not) and its amount. */
@Composable
private fun DebtLine(
    label: String,
    marked: Boolean,
    onToggle: () -> Unit,
    amount: @Composable () -> Unit,
) {
    val colors = CofferTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            // NAV-APP-003 (Figma `214:432`): rows as close as the other lines; the whole row is the target
            .heightIn(min = 24.dp)
            .toggleable(marked, role = Role.Checkbox, onValueChange = { onToggle() }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(18.dp)
                .then(
                    if (marked) {
                        Modifier.background(colors.primary, CircleShape)
                    } else {
                        Modifier.border(1.5.dp, colors.borderHighlight, CircleShape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (marked) Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(12.dp))
        }
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        amount()
    }
}

private fun dueLabel(due: DuePayment) =
    when (due) {
        is LoanDue -> "${due.overview.account.account.name} · cuota del ${dayMonth(due.dueDate)}"
        else -> "${due.title.removePrefix("Pago ")} · vence ${dayMonth(due.dueDate)}"
    }

@Composable
private fun ViewTitle(
    title: String,
    onHome: Boolean,
) {
    val colors = CofferTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        if (onHome) {
            Surface(shape = CofferShapes.pill, color = colors.primaryContainer) {
                Text(
                    "En inicio",
                    Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun Line(
    label: String,
    amount: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = CofferTheme.colors.textSecondary)
        amount()
    }
}
