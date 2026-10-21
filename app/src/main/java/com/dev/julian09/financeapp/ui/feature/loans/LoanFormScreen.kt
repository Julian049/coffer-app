package com.dev.julian09.financeapp.ui.feature.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.Amortization
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.LoanMode
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.usecase.LoanField
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppChip
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.PesosTransformation
import com.dev.julian09.financeapp.ui.components.SegmentedSelector
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.feature.cards.rateLabel
import com.dev.julian09.financeapp.ui.feature.cards.rateTypeLabel
import com.dev.julian09.financeapp.ui.feature.history.DateField
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.LocalDate
import java.util.UUID

@Composable
fun LoanFormScreen(
    onBack: () -> Unit,
    name: String = "",
    viewModel: LoanFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val destinations by viewModel.destinations.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    // Keeps the name typed in Nueva cuenta before choosing Préstamo, like the card form (E4)
    LaunchedEffect(Unit) { if (state.convertingId == null && state.name.isEmpty()) viewModel.setName(name) }
    LoanFormContent(
        state,
        destinations,
        LoanFormActions(
            onBack = onBack,
            onMode = viewModel::setMode,
            onName = viewModel::setName,
            onAmount = viewModel::setAmount,
            onInProgress = viewModel::setInProgress,
            onDestination = viewModel::setDestination,
            onCount = viewModel::setCount,
            onPaymentDay = viewModel::setPaymentDay,
            onRate = viewModel::setRate,
            onRateType = viewModel::setRateType,
            onAmortization = viewModel::setAmortization,
            onFirstDue = viewModel::setFirstDueDate,
            onMonthly = viewModel::setMonthly,
            onInsurance = viewModel::setInsurance,
            onInsuranceAmount = viewModel::setInsuranceAmount,
            onOtherCosts = viewModel::setOtherCosts,
            onOtherCostsAmount = viewModel::setOtherCostsAmount,
            onSave = viewModel::save,
        ),
    )
}

class LoanFormActions(
    val onBack: () -> Unit = {},
    val onMode: (LoanMode) -> Unit = {},
    val onName: (String) -> Unit = {},
    val onAmount: (String) -> Unit = {},
    val onInProgress: (Boolean) -> Unit = {},
    val onDestination: (UUID) -> Unit = {},
    val onCount: (String) -> Unit = {},
    val onPaymentDay: (String) -> Unit = {},
    val onRate: (String) -> Unit = {},
    val onRateType: (RateType) -> Unit = {},
    val onAmortization: (Amortization) -> Unit = {},
    val onFirstDue: (LocalDate) -> Unit = {},
    val onMonthly: (String) -> Unit = {},
    val onInsurance: (Boolean) -> Unit = {},
    val onInsuranceAmount: (String) -> Unit = {},
    val onOtherCosts: (Boolean) -> Unit = {},
    val onOtherCostsAmount: (String) -> Unit = {},
    val onSave: () -> Unit = {},
)

private fun LedgerError.loanMessage(field: LoanField) =
    if (field == LoanField.COUNT && this == LedgerError.INSTALLMENTS_INVALID) "Elige de 1 a 600 cuotas" else message()

/** Nuevo préstamo · Detallado y Simple (S10-UI01, REQ-PRE-01, 02, 08, 10). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoanFormContent(
    state: LoanFormState,
    destinations: List<Account>,
    actions: LoanFormActions,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    val converting = state.convertingId != null
    val detailed = state.mode == LoanMode.DETAILED || converting
    val inProgress = state.inProgress || converting

    fun error(field: LoanField) = state.errors[field]?.loanMessage(field)
    Column(modifier.fillMaxSize()) {
        AppHeader(title = if (converting) "Pasar a detallado" else "Nuevo préstamo", onBack = actions.onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!converting) {
                val modes = listOf(LoanMode.DETAILED, LoanMode.SIMPLE)
                SegmentedSelector(
                    listOf("Detallado", "Simple"),
                    selectedIndex = modes.indexOf(state.mode),
                    onSelect = { actions.onMode(modes[it]) },
                    modifier = Modifier.fillMaxWidth(),
                )
                // REQ-PRE-10
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.textSecondary)
                        Text(
                            if (detailed) {
                                "¿No conoces el saldo? Regístralo como gasto recurrente."
                            } else {
                                "Usa este modo si solo sabes cuánto pagas al mes. Cada pago reduce el saldo por su valor completo; " +
                                    "podrás pasarlo a detallado cuando conozcas las condiciones."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            AppTextField(
                value = state.name,
                onValueChange = actions.onName,
                label = "Nombre",
                placeholder = "ej. Préstamo libre inversión",
                error = error(LoanField.NAME),
                enabled = !converting,
            )
            if (detailed && !converting) {
                SwitchRow(
                    "Ya lo estoy pagando",
                    "Registra el saldo de hoy y las cuotas que faltan, sin desembolso.",
                    state.inProgress,
                    actions.onInProgress,
                )
            }
            AppTextField(
                value = state.amountDigits,
                onValueChange = actions.onAmount,
                label = if (detailed && !inProgress) "Monto recibido" else "Saldo adeudado actual",
                placeholder = "0",
                keyboardType = KeyboardType.Number,
                visualTransformation = PesosTransformation(),
                error = error(LoanField.AMOUNT),
                enabled = !converting,
            )
            if (detailed) {
                if (!inProgress) {
                    Text("Cuenta de destino", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        destinations.forEach {
                            AppChip(
                                it.name,
                                selected = state.destinationId == it.id,
                                onClick = { actions.onDestination(it.id) },
                            )
                        }
                    }
                    error(LoanField.DESTINATION)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.expense) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(
                        value = state.countText,
                        onValueChange = actions.onCount,
                        label = if (inProgress) "Cuotas que faltan" else "Número de cuotas",
                        placeholder = "24",
                        keyboardType = KeyboardType.Number,
                        error = error(LoanField.COUNT),
                        modifier = Modifier.weight(1f),
                    )
                    AppTextField(
                        value = state.paymentDay,
                        onValueChange = actions.onPaymentDay,
                        label = "Día de pago",
                        placeholder = "5",
                        keyboardType = KeyboardType.Number,
                        error = error(LoanField.PAYMENT_DAY),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tasa de interés", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    listOf(RateType.MV, RateType.EA).forEach { type ->
                        AppChip(rateTypeLabel(type), selected = state.rateType == type, onClick = { actions.onRateType(type) })
                    }
                }
                AppTextField(
                    value = state.rateText,
                    onValueChange = actions.onRate,
                    label = null,
                    placeholder = "1,5",
                    keyboardType = KeyboardType.Decimal,
                    error = error(LoanField.RATE),
                    help = state.draft.rate?.let { rateLabel(it, state.rateType) },
                )
                // REQ-PRE-03 (SRS 1.2): chosen per loan
                Text("Tipo de cuota", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                val kinds = listOf(Amortization.FIXED_PAYMENT, Amortization.FIXED_PRINCIPAL)
                SegmentedSelector(
                    listOf("Cuota fija", "Capital fijo"),
                    selectedIndex = kinds.indexOf(state.amortization),
                    onSelect = { actions.onAmortization(kinds[it]) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (state.amortization == Amortization.FIXED_PAYMENT) {
                        "Pagas lo mismo cada mes; al principio va más a intereses. La usan la mayoría de los bancos."
                    } else {
                        "Pagas el mismo capital cada mes más los intereses del saldo; la cuota baja con el tiempo."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                Text(
                    if (inProgress) "Fecha de la próxima cuota" else "Fecha de la primera cuota",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                DateField("Elegir fecha", state.firstDueDate, Modifier.fillMaxWidth(), actions.onFirstDue)
                error(
                    LoanField.FIRST_DUE,
                )?.let { Text("Elige la fecha", style = MaterialTheme.typography.bodySmall, color = colors.expense) }
                ChargeRow("Seguro de vida", state.insurance, state.insuranceDigits, actions.onInsurance, actions.onInsuranceAmount)
                ChargeRow("Costos adicionales", state.otherCosts, state.otherCostsDigits, actions.onOtherCosts, actions.onOtherCostsAmount)
                state.firstInstallment?.let { first ->
                    AppCard(Modifier.fillMaxWidth()) {
                        Text(
                            if (inProgress) "Próxima cuota estimada" else "Primera cuota estimada",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                        MoneyText(first.total, style = MaterialTheme.typography.headlineSmall)
                        val parts =
                            buildList {
                                add("Capital ${formatMoney(first.principal)}")
                                add("intereses ${formatMoney(first.interest)}")
                                if (first.insurance > Money.ZERO) add("seguro ${formatMoney(first.insurance)}")
                                if (first.otherCosts > Money.ZERO) add("costos ${formatMoney(first.otherCosts)}")
                            }
                        Text(parts.joinToString(" + "), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
                if (!inProgress) {
                    Text(
                        "Recibir el dinero se registra como transferencia del préstamo a la cuenta de destino; no cuenta como ingreso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            } else {
                AppTextField(
                    value = state.monthlyDigits,
                    onValueChange = actions.onMonthly,
                    label = "Cuota mensual",
                    placeholder = "0",
                    keyboardType = KeyboardType.Number,
                    visualTransformation = PesosTransformation(),
                    error = error(LoanField.MONTHLY),
                )
                AppTextField(
                    value = state.paymentDay,
                    onValueChange = actions.onPaymentDay,
                    label = "Día de pago",
                    placeholder = "5",
                    keyboardType = KeyboardType.Number,
                    error = error(LoanField.PAYMENT_DAY),
                )
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            AppButton(
                if (converting) "Guardar condiciones" else "Crear préstamo",
                onClick = actions.onSave,
                icon = Icons.Outlined.Check,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    detail: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = CofferTheme.colors
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.primary, checkedThumbColor = colors.onPrimary),
            )
        }
    }
}

/** REQ-PRE-02: checking it asks for the monthly value and says it is charged in every installment. */
@Composable
private fun ChargeRow(
    title: String,
    checked: Boolean,
    digits: String,
    onCheck: (Boolean) -> Unit,
    onAmount: (String) -> Unit,
) {
    val colors = CofferTheme.colors
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheck,
                colors = CheckboxDefaults.colors(checkedColor = colors.primary, checkmarkColor = colors.onPrimary),
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text("Se cobrará en cada cuota", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        if (checked) {
            AppTextField(
                value = digits,
                onValueChange = onAmount,
                label = "Valor mensual",
                placeholder = "0",
                keyboardType = KeyboardType.Number,
                visualTransformation = PesosTransformation(),
            )
        }
    }
}
