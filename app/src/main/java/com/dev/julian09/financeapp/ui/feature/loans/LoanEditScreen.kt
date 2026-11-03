package com.dev.julian09.financeapp.ui.feature.loans

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.usecase.LoanField
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.SegmentedSelector
import com.dev.julian09.financeapp.ui.feature.cards.parsePercent
import com.dev.julian09.financeapp.ui.feature.cards.rateLabel
import com.dev.julian09.financeapp.ui.feature.cards.rateTypeLabel
import com.dev.julian09.financeapp.ui.theme.CofferTheme

@Composable
fun LoanEditScreen(
    onBack: () -> Unit,
    viewModel: LoanEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    LoanEditContent(state, onBack, viewModel)
}

@Composable
private fun LoanEditContent(
    state: LoanEditState,
    onBack: () -> Unit,
    viewModel: LoanEditViewModel,
) {
    val colors = CofferTheme.colors

    fun error(field: LoanField) = state.errors[field]?.loanMessage(field)
    // S12-UI09: the bottom button rides on the keyboard
    Column(Modifier.fillMaxSize().imePadding()) {
        AppHeader(title = "Editar préstamo", onBack = onBack)
        if (!state.loaded) return@Column
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // PRE-APP-001 (Figma `100:326`): the loan's icon, name and terms on the background, the note under them
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Toll, contentDescription = null, tint = colors.iconOnContainer)
                    }
                    Column {
                        Text(state.name, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                        if (state.summary.isNotEmpty()) {
                            Text(state.summary, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
                Text(
                    "El monto, el número de cuotas y la fecha de inicio no se pueden cambiar. Para reducir la deuda, usa Abonar a capital.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            AppTextField(value = state.name, onValueChange = viewModel::setName, label = "Nombre", error = error(LoanField.NAME))
            if (state.detailed) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tasa de interés", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    SegmentedSelector(
                        listOf(RateType.MV, RateType.EA).map(::rateTypeLabel),
                        selectedIndex = if (state.rateType == RateType.MV) 0 else 1,
                        onSelect = { viewModel.setRateType(if (it == 0) RateType.MV else RateType.EA) },
                        modifier = Modifier.width(110.dp),
                    )
                }
                AppTextField(
                    value = state.rateText,
                    onValueChange = viewModel::setRate,
                    label = null,
                    placeholder = "1,5",
                    leadingIcon = Icons.Outlined.Percent,
                    keyboardType = KeyboardType.Decimal,
                    error = error(LoanField.RATE),
                    help = parsePercent(state.rateText)?.let { rateLabel(it, state.rateType) },
                )
            }
            AppTextField(
                value = state.paymentDay,
                onValueChange = viewModel::setPaymentDay,
                label = "Día de pago",
                placeholder = "5",
                leadingIcon = Icons.Outlined.CalendarToday,
                keyboardType = KeyboardType.Number,
                error = error(LoanField.PAYMENT_DAY),
            )
            if (state.detailed) {
                AppCard(Modifier.fillMaxWidth()) {
                    ChargeRow(
                        "Seguro de vida",
                        state.insurance,
                        state.insuranceDigits,
                        viewModel::setInsurance,
                        viewModel::setInsuranceAmount,
                        inCard = false,
                    )
                    HorizontalDivider(thickness = 1.dp, color = colors.border)
                    ChargeRow(
                        "Costos adicionales",
                        state.otherCosts,
                        state.otherCostsDigits,
                        viewModel::setOtherCosts,
                        viewModel::setOtherCostsAmount,
                        inCard = false,
                    )
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            AppButton("Guardar cambios", onClick = viewModel::save, modifier = Modifier.fillMaxWidth())
        }
    }
}
