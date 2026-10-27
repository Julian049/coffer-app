package com.dev.julian09.financeapp.ui.feature.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.usecase.CardField
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppChip
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.PesosTransformation
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.feature.accounts.LastDigitsField
import com.dev.julian09.financeapp.ui.theme.CofferTheme

@Composable
fun CardFormScreen(
    onBack: () -> Unit,
    name: String = "",
    viewModel: CardFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    // Keeps the name typed in Nueva cuenta before choosing Tarjeta de crédito (E4, #78)
    LaunchedEffect(Unit) { if (state.editingId == null && state.name.isEmpty()) viewModel.setName(name) }
    CardFormContent(
        state,
        CardFormActions(
            onBack = onBack,
            onName = viewModel::setName,
            onLimit = viewModel::setLimit,
            onDebt = viewModel::setDebt,
            onCutOff = viewModel::setCutOffDay,
            onPayment = viewModel::setPaymentDay,
            onRate = viewModel::setRate,
            onRateType = viewModel::setRateType,
            onFirstFree = viewModel::setFirstInterestFree,
            onSave = viewModel::save,
            onLastDigits = viewModel::setLastDigits,
        ),
    )
}

class CardFormActions(
    val onBack: () -> Unit = {},
    val onName: (String) -> Unit = {},
    val onLimit: (String) -> Unit = {},
    val onDebt: (String) -> Unit = {},
    val onCutOff: (String) -> Unit = {},
    val onPayment: (String) -> Unit = {},
    val onRate: (String) -> Unit = {},
    val onRateType: (RateType) -> Unit = {},
    val onFirstFree: (Boolean) -> Unit = {},
    val onSave: () -> Unit = {},
    val onLastDigits: (String) -> Unit = {},
)

/** Nueva tarjeta de crédito (S7-UI01, REQ-TC-01 to 03, 13); editing keeps the debt it was created with. */
@Composable
fun CardFormContent(
    state: CardFormState,
    actions: CardFormActions,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    val editing = state.editingId != null
    val errors = state.errors
    // S12-UI09: the bottom button rides on the keyboard
    Column(modifier.fillMaxSize().imePadding()) {
        AppHeader(title = if (editing) "Editar tarjeta" else "Nueva tarjeta de crédito", onBack = actions.onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!editing) {
                Text(
                    "Registra tu tarjeta para monitorear cupo, cuotas y pagos oportunos con precisión.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            } else {
                // Editar tarjeta (Figma `100:229`): what is calculated shows above, only the editable fields below
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(48.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = colors.iconOnContainer)
                        }
                        Column {
                            Text(state.name.ifBlank { "Tarjeta" }, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text(
                                "Tarjeta de crédito · deuda ${formatMoney(state.debt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                    }
                    Text(
                        "La deuda se calcula con tus compras y pagos. Para cambiarla, registra un pago o un movimiento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }
            AppTextField(
                value = state.name,
                onValueChange = actions.onName,
                label = if (editing) "Nombre" else "Nombre / Entidad financiera",
                placeholder = "ej. Visa Platinum",
                error = errors[CardField.NAME]?.message(),
            )
            if (editing) LastDigitsField(state.lastDigits, actions.onLastDigits)
            AppTextField(
                value = state.limitDigits,
                visualTransformation = PesosTransformation(symbol = false),
                onValueChange = actions.onLimit,
                label = if (editing) "Cupo total" else "Cupo total asignado",
                placeholder = "0",
                keyboardType = KeyboardType.Number,
                error = errors[CardField.LIMIT]?.message(),
            )
            if (!editing) {
                AppTextField(
                    value = state.debtDigits,
                    visualTransformation = PesosTransformation(symbol = false),
                    onValueChange = actions.onDebt,
                    label = "Deuda actual",
                    placeholder = "0",
                    keyboardType = KeyboardType.Number,
                    error = errors[CardField.DEBT]?.message(),
                )
            }
            if (!editing) {
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (editing) "Deuda inicial" else "Cupo disponible calculado",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                        if (editing) {
                            MoneyText(state.debt, kind = if (state.debt == Money.ZERO) MoneyKind.Neutral else MoneyKind.Debt)
                        } else {
                            MoneyText(state.available)
                        }
                    }
                    Text(
                        "Cuenta como una compra a 1 cuota del periodo en que creas la tarjeta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Tasa de interés pactada",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    listOf(RateType.MV, RateType.EA).forEach { type ->
                        AppChip(rateTypeLabel(type), selected = state.rateType == type, onClick = { actions.onRateType(type) })
                    }
                }
                AppTextField(
                    value = state.rateText,
                    onValueChange = actions.onRate,
                    label = null,
                    placeholder = "2,0",
                    keyboardType = KeyboardType.Decimal,
                    error = errors[CardField.RATE]?.message(),
                    help =
                        state.rate?.let { rateLabel(it, state.rateType) }
                            ?: "% ${if (state.rateType == RateType.MV) "mes vencido" else "efectivo anual"}",
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(
                    value = state.cutOffDay,
                    onValueChange = actions.onCutOff,
                    label = "Día de corte",
                    placeholder = "20",
                    keyboardType = KeyboardType.Number,
                    error = errors[CardField.CUT_OFF]?.message(),
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = state.paymentDay,
                    onValueChange = actions.onPayment,
                    label = if (editing) "Día de pago" else "Límite de pago",
                    placeholder = "5",
                    keyboardType = KeyboardType.Number,
                    error = errors[CardField.PAYMENT]?.message(),
                    modifier = Modifier.weight(1f),
                )
            }
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Primera cuota sin intereses", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text(
                            "Las compras diferidas no pagan intereses en su primera cuota.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Switch(
                        checked = state.firstInstallmentInterestFree,
                        onCheckedChange = actions.onFirstFree,
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.primary, checkedThumbColor = colors.onPrimary),
                    )
                }
            }
        }
        // M4 (#88): the save button stays visible while the form scrolls
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(
                if (editing) "Guardar cambios" else "Guardar tarjeta de crédito",
                onClick = actions.onSave,
                icon = Icons.Outlined.Check,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton("Cancelar", onClick = actions.onBack, kind = ButtonKind.Ghost, modifier = Modifier.fillMaxWidth())
        }
    }
}
