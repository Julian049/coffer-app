package com.dev.julian09.financeapp.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.domain.model.DuePayment
import com.dev.julian09.financeapp.domain.model.LoanDue
import com.dev.julian09.financeapp.domain.usecase.HomeDashboard
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.SegmentedSelector
import com.dev.julian09.financeapp.ui.feature.recurring.dayMonth
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/** Hoja · Detalle del saldo (S11-UI02, REQ-CTA-08, 09): both views, and which one Home shows. */
@Composable
fun BalanceSheet(
    dashboard: HomeDashboard,
    view: BalanceView,
    onView: (BalanceView) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss, title = "Detalle del saldo", subtitle = "Elige qué vista ves en inicio; la otra queda aquí.") {
        BalanceViews(dashboard, view)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mostrar en inicio", style = MaterialTheme.typography.labelMedium, color = CofferTheme.colors.textSecondary)
            SegmentedSelector(
                listOf("Disponible", "Neto"),
                selectedIndex = view.ordinal,
                onSelect = { onView(BalanceView.entries[it]) },
            )
        }
        AppButton("Listo", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun BalanceViews(
    dashboard: HomeDashboard,
    view: BalanceView,
) {
    val colors = CofferTheme.colors
    AppCard(Modifier.fillMaxWidth(), highlight = view == BalanceView.AVAILABLE) {
        ViewTitle("Saldo disponible", view == BalanceView.AVAILABLE)
        MoneyText(dashboard.available, style = MaterialTheme.typography.headlineMedium)
        Text(
            "Efectivo, débito y ahorros incluidos en el saldo total. Sin tarjetas ni préstamos.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
    AppCard(Modifier.fillMaxWidth(), highlight = view == BalanceView.NET) {
        ViewTitle("Saldo neto", view == BalanceView.NET)
        MoneyText(dashboard.net, style = MaterialTheme.typography.headlineMedium)
        Line("Cuentas incluidas") { MoneyText(dashboard.available, style = MaterialTheme.typography.bodyMedium) }
        dashboard.dues.forEach { due ->
            Line(dueLabel(due)) { MoneyText(due.amount, kind = MoneyKind.Expense, style = MaterialTheme.typography.bodyMedium) }
        }
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
