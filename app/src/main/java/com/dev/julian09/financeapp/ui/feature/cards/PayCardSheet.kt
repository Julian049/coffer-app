package com.dev.julian09.financeapp.ui.feature.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.CardPaymentKind
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.usecase.TransactionField
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.PesosTransformation
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.feature.recurring.dayMonth
import com.dev.julian09.financeapp.ui.feature.transactions.AccountPicker
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.util.UUID

/** Hoja · Pagar tarjeta, shown while [cardId] is set. */
@Composable
fun PayCardSheet(
    cardId: UUID?,
    onDismiss: () -> Unit,
    viewModel: PayCardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    LaunchedEffect(cardId) { if (cardId == null) viewModel.reset() else viewModel.open(cardId) }
    LaunchedEffect(state.done) {
        if (state.done) {
            viewModel.reset()
            onDismiss()
        }
    }
    if (cardId == null || state.overview?.card?.accountId != cardId) return
    AppBottomSheet(onDismiss = onDismiss) {
        PayCardContent(
            state,
            sources,
            PayCardActions(viewModel::setKind, viewModel::setAmount, viewModel::setSource, viewModel::confirm, onDismiss),
        )
    }
}

class PayCardActions(
    val onKind: (CardPaymentKind) -> Unit = {},
    val onAmount: (String) -> Unit = {},
    val onSource: (UUID) -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onCancel: () -> Unit = {},
)

/** SEQ-07: mínimo or total, the source account, an editable amount and how it splits (REQ-TC-10, 11). */
@Composable
fun PayCardContent(
    state: PayCardState,
    sources: List<AccountBalance>,
    actions: PayCardActions,
) {
    val colors = CofferTheme.colors
    val overview = state.overview ?: return
    val statement = overview.statement
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("Pagar ${overview.account.account.name}", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text(
                "Corte ${dayMonth(statement.period.end)} · vence ${dayMonth(statement.period.dueDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        Option(
            "Pago mínimo",
            "Lo que vence en el periodo",
            statement.minimum,
            state.kind == CardPaymentKind.MINIMUM,
        ) { actions.onKind(CardPaymentKind.MINIMUM) }
        Option(
            "Pago total",
            "Liquida las cuotas restantes sin intereses futuros",
            statement.total,
            state.kind == CardPaymentKind.TOTAL,
        ) { actions.onKind(CardPaymentKind.TOTAL) }
        AccountPicker("Cuenta de origen", sources, state.sourceId, state.errors[TransactionField.ACCOUNT], actions.onSource)
        AppTextField(
            value = state.amountDigits,
            visualTransformation = PesosTransformation(),
            onValueChange = actions.onAmount,
            label = "Monto a pagar",
            keyboardType = KeyboardType.Number,
            error = state.errors[TransactionField.AMOUNT]?.message(),
        )
        state.breakdown?.let { split ->
            AppCard(Modifier.fillMaxWidth()) {
                Text("Desglose", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Line("Capital", split.principal)
                Line("Intereses", split.interest)
                Line("Otros cargos", split.fees)
            }
        }
        AppButton("Registrar pago", onClick = actions.onConfirm, modifier = Modifier.fillMaxWidth())
        AppButton("Cancelar", onClick = actions.onCancel, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Option(
    title: String,
    detail: String,
    amount: Money,
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
            Text(formatMoney(amount), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
        }
    }
}

@Composable
internal fun Line(
    label: String,
    amount: Money,
) {
    val colors = CofferTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Text(formatMoney(amount), style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
    }
}
