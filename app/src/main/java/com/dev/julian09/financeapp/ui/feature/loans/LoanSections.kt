package com.dev.julian09.financeapp.ui.feature.loans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.Amortization
import com.dev.julian09.financeapp.domain.model.ExtraPaymentMode
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.LoanMode
import com.dev.julian09.financeapp.domain.model.LoanOverview
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.usecase.TransactionField
import com.dev.julian09.financeapp.domain.usecase.balance
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.ApproximateNote
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.LOAN_APPROXIMATE_NOTE
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.PesosTransformation
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.feature.cards.rateLabel
import com.dev.julian09.financeapp.ui.feature.recurring.dayMonth
import com.dev.julian09.financeapp.ui.feature.transactions.AccountPicker
import com.dev.julian09.financeapp.ui.feature.transactions.DateTimeCard
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("es-CO"))

private fun monthYear(month: YearMonth) = MONTH_YEAR.format(month.atDay(1)).replace(".", "")

/** Detalle de préstamo (S10-UI01): the debt, pay and extra payment, the terms and the plan (REQ-PRE-03). */
@Composable
fun LoanDetailSections(
    overview: LoanOverview,
    onPay: () -> Unit,
    onExtra: () -> Unit,
    onConvert: () -> Unit,
) {
    val colors = CofferTheme.colors
    val loan = overview.loan
    val detailed = loan.mode == LoanMode.DETAILED
    AppCard(Modifier.fillMaxWidth(), highlight = true) {
        Text("Deuda pendiente", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
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
        if (detailed) {
            val total = overview.installments.count { it.status != InstallmentStatus.CANCELLED }
            val last = overview.scheduled.lastOrNull()?.dueDate
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else overview.paid.toFloat() / total },
                modifier = Modifier.fillMaxWidth().clip(CofferShapes.pill),
                color = colors.expense,
                trackColor = colors.border,
                drawStopIndicator = {},
            )
            Text(
                "${overview.paid} de $total cuotas pagadas" + (last?.let { " · termina en ${monthYear(YearMonth.from(it))}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppButton("Pagar cuota", onClick = onPay, enabled = overview.debt > Money.ZERO, modifier = Modifier.weight(1f))
        if (detailed) {
            AppButton(
                "Abonar a capital",
                onClick = onExtra,
                kind = ButtonKind.Secondary,
                enabled = overview.scheduled.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
        } else {
            AppButton("Pasar a detallado", onClick = onConvert, kind = ButtonKind.Secondary, modifier = Modifier.weight(1f))
        }
    }
    AppCard(Modifier.fillMaxWidth()) {
        Text("Condiciones", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        if (detailed) {
            loan.principal?.let {
                Term(
                    if (loan.disbursementAccountId ==
                        null
                    ) {
                        "Saldo al registrarlo"
                    } else {
                        "Monto inicial"
                    },
                    formatMoney(it),
                )
            }
            loan.rate?.let { rate -> loan.rateType?.let { Term("Tasa", rateLabel(rate, it)) } }
            Term("Tipo de cuota", if (loan.amortization == Amortization.FIXED_PAYMENT) "Cuota fija" else "Capital fijo")
            Term("Seguro de vida", if (loan.lifeInsurance > Money.ZERO) "${formatMoney(loan.lifeInsurance)} / mes" else "No")
            Term("Costos adicionales", if (loan.otherCosts > Money.ZERO) "${formatMoney(loan.otherCosts)} / mes" else "No")
        } else {
            loan.monthlyPayment?.let { Term("Cuota mensual", formatMoney(it)) }
        }
        Term("Día de pago", loan.paymentDay.toString())
    }
    if (detailed) {
        val plan = overview.installments.filter { it.status != InstallmentStatus.CANCELLED }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Plan de pagos", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text("${plan.size} cuotas", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        // The next ones first, then the paid ones (newest first), like the Figma
        val ordered = overview.scheduled + plan.filter { it.status == InstallmentStatus.PAID }.sortedByDescending { it.number }
        AppCard(Modifier.fillMaxWidth()) {
            ordered.forEachIndexed { i, it ->
                val status =
                    when {
                        it.status == InstallmentStatus.PAID -> "Pagada"
                        i == 0 -> "Pendiente"
                        else -> "Programada"
                    }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Cuota ${it.number} · ${dayMonth(it.dueDate)} ${it.dueDate.year}",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                        )
                        val parts =
                            buildList {
                                add("Capital ${formatMoney(it.principal)}")
                                add("intereses ${formatMoney(it.interest)}")
                                if (it.charges > Money.ZERO) add("seguro y costos ${formatMoney(it.charges)}")
                            }
                        Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatMoney(it.total), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text(
                            status,
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                when (status) {
                                    "Pagada" -> colors.income
                                    "Pendiente" -> colors.warning
                                    else -> colors.textSecondary
                                },
                        )
                    }
                }
            }
        }
    }
    Text(
        "El saldo neto descuenta la cuota del periodo o toda la deuda pendiente, según elijas en Detalle del saldo.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary,
    )
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

/** Hoja · Pagar cuota or Hoja · Abono a capital, shown while [loanId] is set. */
@Composable
fun LoanPaymentSheet(
    loanId: UUID?,
    sheet: LoanSheet,
    onDismiss: () -> Unit,
    viewModel: LoanPaymentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    LaunchedEffect(loanId, sheet) { if (loanId == null) viewModel.reset() else viewModel.open(loanId, sheet) }
    LaunchedEffect(state.done) {
        if (state.done) {
            viewModel.reset()
            onDismiss()
        }
    }
    if (loanId == null || state.overview?.loan?.accountId != loanId) return
    val actions =
        LoanPaymentActions(
            viewModel::setAmount,
            viewModel::setMode,
            viewModel::setSource,
            viewModel::confirm,
            onDismiss,
            viewModel::setOccurredAt,
        )
    AppBottomSheet(onDismiss = onDismiss, actions = { LoanPaymentButtons(state.sheet, actions) }) {
        LoanPaymentContent(state, sources, actions, withButtons = false)
    }
}

class LoanPaymentActions(
    val onAmount: (String) -> Unit = {},
    val onMode: (ExtraPaymentMode) -> Unit = {},
    val onSource: (UUID) -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onOccurredAt: (Instant) -> Unit = {},
)

/** SEQ-11: the installment with its split (REQ-PRE-05), or the extra payment with both options simulated (REQ-PRE-06). */
@Composable
fun LoanPaymentContent(
    state: LoanPaymentState,
    sources: List<AccountBalance>,
    actions: LoanPaymentActions,
    /** False inside the sheet, which shows them fixed under the content (S12-UI09). */
    withButtons: Boolean = true,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val colors = CofferTheme.colors
    val overview = state.overview ?: return
    val name = overview.account.account.name
    val next = overview.next.takeIf { overview.loan.mode == LoanMode.DETAILED }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text(
                if (state.sheet == LoanSheet.EXTRA) "Abonar a capital" else "Pagar cuota",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
            )
            Text("$name · deuda ${formatMoney(overview.debt)}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        if (state.sheet == LoanSheet.PAY && next != null) {
            AppCard(Modifier.fillMaxWidth()) {
                Text(
                    "Cuota ${next.number} · vence ${dayMonth(next.dueDate)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                MoneyText(next.total, style = MaterialTheme.typography.headlineSmall)
                Split("Capital", next.principal)
                Split("Intereses", next.interest)
                if (next.charges > Money.ZERO) Split("Seguro y costos", next.charges)
                Text(
                    "El capital baja la deuda; intereses, seguro y costos van a \"Intereses y cargos financieros\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        } else {
            AppTextField(
                value = state.amountDigits,
                visualTransformation = PesosTransformation(),
                onValueChange = actions.onAmount,
                label = if (state.sheet == LoanSheet.EXTRA) "Monto del abono" else "Monto a pagar",
                keyboardType = KeyboardType.Number,
                error = state.errors[TransactionField.AMOUNT]?.message(),
            )
        }
        if (state.sheet == LoanSheet.EXTRA) {
            Text("¿Qué quieres reducir?", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            val remaining = overview.scheduled.size
            val shorter = state.simulation(ExtraPaymentMode.REDUCE_TERM)
            val lower = state.simulation(ExtraPaymentMode.REDUCE_PAYMENT)
            ModeOption(
                "El plazo",
                if (shorter.isEmpty()) "Terminas antes" else "Terminas antes: quedan ${shorter.size} cuotas en lugar de $remaining",
                state.mode == ExtraPaymentMode.REDUCE_TERM,
            ) { actions.onMode(ExtraPaymentMode.REDUCE_TERM) }
            ModeOption(
                "El valor de la cuota",
                lower.firstOrNull()?.let { "Sigues con $remaining cuotas y la cuota baja a ≈ ${formatMoney(it.total)}" }
                    ?: "Sigues con $remaining cuotas",
                state.mode == ExtraPaymentMode.REDUCE_PAYMENT,
            ) { actions.onMode(ExtraPaymentMode.REDUCE_PAYMENT) }
            if (state.amount >= overview.balance && overview.balance > Money.ZERO) {
                Text("Este abono paga todo el préstamo.", style = MaterialTheme.typography.bodySmall, color = colors.income)
            }
        }
        AccountPicker("Cuenta de origen", sources, state.sourceId, state.errors[TransactionField.ACCOUNT], actions.onSource)
        // S15-UI10: the date and time of the payment
        DateTimeCard(state.occurredAt, actions.onOccurredAt, today, zone)
        state.errors[TransactionField.DESTINATION]?.let {
            Text(
                it.message(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.expense,
            )
        }
        if (state.sheet == LoanSheet.PAY) ApproximateNote(LOAN_APPROXIMATE_NOTE)
        if (withButtons) LoanPaymentButtons(state.sheet, actions)
    }
}

@Composable
private fun LoanPaymentButtons(
    sheet: LoanSheet,
    actions: LoanPaymentActions,
) {
    AppButton(
        if (sheet == LoanSheet.EXTRA) "Registrar abono" else "Registrar pago",
        onClick = actions.onConfirm,
        modifier = Modifier.fillMaxWidth(),
    )
    AppButton("Cancelar", onClick = actions.onCancel, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun Split(
    label: String,
    amount: Money,
) {
    val colors = CofferTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Text(formatMoney(amount), style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
    }
}

@Composable
private fun ModeOption(
    title: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = CofferTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onClick),
        shape = CofferShapes.card,
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.onPrimaryContainer else colors.border),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = colors.primary))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }
}
