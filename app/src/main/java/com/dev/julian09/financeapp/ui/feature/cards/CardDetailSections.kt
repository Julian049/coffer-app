package com.dev.julian09.financeapp.ui.feature.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.domain.model.CardOverview
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.feature.recurring.dayMonth
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/** Detalle de tarjeta (S7-UI01): the debt with the credit used, what is due, the terms and the deferred purchases. */
@Composable
fun CardDetailSections(
    overview: CardOverview,
    onPay: () -> Unit,
) {
    val colors = CofferTheme.colors
    val card = overview.card
    val statement = overview.statement
    AppCard(Modifier.fillMaxWidth(), highlight = true) {
        Text("Deuda total", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        MoneyText(
            overview.debt,
            kind =
                if (overview.debt ==
                    Money.ZERO
                ) {
                    MoneyKind.Neutral
                } else {
                    MoneyKind.Debt
                },
            style = MaterialTheme.typography.displaySmall,
        )
        val used = if (card.creditLimit > Money.ZERO) (overview.debt.scaled.toFloat() / card.creditLimit.scaled).coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = { used },
            modifier = Modifier.fillMaxWidth().clip(CofferShapes.pill),
            color = colors.expense,
            trackColor = colors.border,
            drawStopIndicator = {},
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Cupo disponible", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            Text(
                "${formatMoney(overview.available)} de ${formatMoney(card.creditLimit)}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textPrimary,
            )
        }
    }
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Vence este periodo", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            Text(dayMonth(statement.period.dueDate), style = MaterialTheme.typography.labelLarge, color = colors.warning)
        }
        Text(formatMoney(statement.minimum), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Text(
            "Compras a 1 cuota ${formatMoney(statement.onePayment)} · cuotas ${formatMoney(statement.installmentsPrincipal)} · " +
                "intereses ${formatMoney(statement.interest)}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        AppButton("Pagar tarjeta", onClick = onPay, modifier = Modifier.fillMaxWidth())
    }
    AppCard(Modifier.fillMaxWidth()) {
        Text("Condiciones", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Term("Día de corte", card.cutOffDay.toString())
        Term("Día de pago", card.paymentDay.toString())
        Term("Tasa", rateLabel(card.rate, card.rateType))
        Term("Primera cuota sin intereses", if (card.firstInstallmentInterestFree) "Sí" else "No")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Compras diferidas", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Text(
            if (overview.deferred.size == 1) "1 activa" else "${overview.deferred.size} activas",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
    if (overview.deferred.isEmpty()) {
        Text("Sin compras a cuotas pendientes.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
    }
    overview.deferred.forEach { d ->
        val plan = overview.installments.filter { it.deferredPurchaseId == d.id && it.status != InstallmentStatus.CANCELLED }
        val next = plan.firstOrNull { it.status == InstallmentStatus.SCHEDULED }
        AppCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(formatMoney(d.amount), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    Text(
                        "${d.installmentsCount} cuotas · ${rateLabel(d.rate, d.rateType).substringBefore(" ·")}" +
                            (next?.let { " · cuota ${it.number} de ${d.installmentsCount}" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                next?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatMoney(it.total), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text(
                            if (it.interest == Money.ZERO) "sin intereses" else "+ ${formatMoney(it.interest)} int.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Term(
    label: String,
    value: String,
) {
    val colors = CofferTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
    }
}
