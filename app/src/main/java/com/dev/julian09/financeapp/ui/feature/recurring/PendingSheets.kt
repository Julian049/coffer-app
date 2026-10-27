package com.dev.julian09.financeapp.ui.feature.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.usecase.TransactionField
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.ConfirmDialog
import com.dev.julian09.financeapp.ui.components.PesosTransformation
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.feature.transactions.AccountPicker
import com.dev.julian09.financeapp.ui.feature.transactions.DateTimeCard
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-CO"))

/** "9 oct" */
fun dayMonth(date: LocalDate): String = DAY_MONTH.format(date).replace(".", "")

/** "Vence en 2 días", "Vence mañana", "Vence hoy", "Venció hace 3 días". */
fun dueSentence(daysLeft: Long): String =
    when {
        daysLeft < 0 -> dueLabel(daysLeft)
        daysLeft == 0L -> "Vence hoy"
        daysLeft == 1L -> "Vence mañana"
        else -> "Vence en $daysLeft días"
    }

/**
 * The sheets of a pending payment (SEQ-10 §10.2): the actions, the payment and the confirmation to finalize. Shown while
 * [recurringId] is set; [payNow] opens on the payment.
 */
@Composable
fun PendingSheets(
    recurringId: UUID?,
    onDismiss: () -> Unit,
    payNow: Boolean = false,
    viewModel: PendingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    LaunchedEffect(recurringId, payNow) { if (recurringId == null) viewModel.reset() else viewModel.open(recurringId, payNow) }
    LaunchedEffect(state.done) {
        if (state.done) {
            viewModel.reset()
            onDismiss()
        }
    }
    val pending = state.pending ?: return
    if (recurringId == null || pending.recurring.id != recurringId) return
    val r = pending.recurring
    val colors = CofferTheme.colors
    when (state.step) {
        PendingStep.ACTIONS ->
            AppBottomSheet(
                onDismiss = onDismiss,
                title = r.name,
                subtitle = "${dueSentence(pending.daysLeft)} · ${dayMonth(r.nextDueDate)} · ${frequencyLabel(r.frequency).lowercase()}",
            ) {
                AppCard(Modifier.fillMaxWidth()) {
                    Text("Monto del último pago", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    Text(formatMoney(r.suggestedAmount), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    Text(
                        "Este pendiente no afecta ningún saldo hasta que lo pagues.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                ActionRow(
                    Icons.Outlined.Check,
                    "Pagar",
                    "Registra el gasto real y programa el siguiente",
                    colors.textPrimary,
                    viewModel::startPay,
                )
                ActionRow(
                    Icons.Outlined.Close,
                    "Finalizar recurrencia",
                    "No se generarán más pendientes",
                    colors.expense,
                    viewModel::askEnd,
                )
                AppButton("Cancelar", onClick = onDismiss, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
            }
        PendingStep.PAY ->
            AppBottomSheet(
                onDismiss = onDismiss,
                title = "Pagar ${r.name}",
                subtitle = "Se registrará como gasto real en ${state.categoryLabel}.",
                actions = {
                    AppButton("Confirmar pago", onClick = viewModel::confirmPay, modifier = Modifier.fillMaxWidth())
                    AppButton("Cancelar", onClick = onDismiss, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppTextField(
                        value = state.amountDigits,
                        visualTransformation = PesosTransformation(),
                        onValueChange = viewModel::setAmount,
                        label = "Monto",
                        keyboardType = KeyboardType.Number,
                        help = "Prellenado con el último pago; puedes cambiarlo.",
                        error = state.errors[TransactionField.AMOUNT]?.message(),
                    )
                    DateTimeCard(state.occurredAt, viewModel::setOccurredAt, LocalDate.now(), ZoneId.systemDefault())
                    AccountPicker("Cuenta", accounts, state.accountId, state.errors[TransactionField.ACCOUNT], viewModel::setAccount)
                    state.errors[TransactionField.CATEGORY]?.let {
                        Text(
                            "La categoría de este recurrente ya no existe: edítalo y elige otra.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.expense,
                        )
                    }
                }
            }
        PendingStep.CONFIRM_END ->
            ConfirmDialog(
                title = "¿Finalizar ${r.name}?",
                message = "No se generarán más pendientes y desaparecerá de tus gastos recurrentes. Los pagos ya registrados se quedan.",
                confirmText = "Finalizar",
                onConfirm = viewModel::confirmEnd,
                onDismiss = viewModel::back,
                destructive = true,
            )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    detail: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val colors = CofferTheme.colors
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(32.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = tint)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }
}
