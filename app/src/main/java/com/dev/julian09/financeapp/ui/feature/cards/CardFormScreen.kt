package com.dev.julian09.financeapp.ui.feature.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.usecase.CardField
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.ApproximateNote
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.CARD_APPROXIMATE_NOTE
import com.dev.julian09.financeapp.ui.components.PesosTransformation
import com.dev.julian09.financeapp.ui.components.SegmentedSelector
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.feature.accounts.LastDigitsField
import com.dev.julian09.financeapp.ui.theme.CofferShapes
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
                // TAR-APP-001 (Figma `1:1652`): the kicker, the line under it and a preview of the card as it is filled in
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = colors.primary, modifier = Modifier.size(15.dp))
                        Text("Instrumento financiero", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                    }
                    Text(
                        "Registra tu tarjeta para monitorear cupo, cuotas y pagos oportunos con precisión.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
                CardPreview(state)
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
                label = "Nombre",
                placeholder = "ej. Visa Platinum",
                leadingIcon = Icons.Outlined.Badge,
                error = errors[CardField.NAME]?.message(),
            )
            LastDigitsField(state.lastDigits, actions.onLastDigits, leadingIcon = Icons.Outlined.Pin)
            AppTextField(
                value = state.limitDigits,
                visualTransformation = PesosTransformation(),
                onValueChange = actions.onLimit,
                label = if (editing) "Cupo total" else "Cupo total asignado",
                placeholder = "0",
                keyboardType = KeyboardType.Number,
                error = errors[CardField.LIMIT]?.message(),
            )
            if (!editing) {
                AppTextField(
                    value = state.debtDigits,
                    visualTransformation = PesosTransformation(),
                    onValueChange = actions.onDebt,
                    label = "Deuda actual",
                    placeholder = "0",
                    keyboardType = KeyboardType.Number,
                    error = errors[CardField.DEBT]?.message(),
                    // the available balance now shows in the preview above
                    help = "Cuenta como una compra a 1 cuota del periodo en que creas la tarjeta.",
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Tasa de interés pactada",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                    )
                    SegmentedSelector(
                        listOf(RateType.MV, RateType.EA).map(::rateTypeLabel),
                        selectedIndex = if (state.rateType == RateType.MV) 0 else 1,
                        onSelect = { actions.onRateType(if (it == 0) RateType.MV else RateType.EA) },
                        modifier = Modifier.width(110.dp),
                    )
                }
                AppTextField(
                    value = state.rateText,
                    onValueChange = actions.onRate,
                    label = null,
                    placeholder = "2,0",
                    leadingIcon = Icons.Outlined.Percent,
                    keyboardType = KeyboardType.Decimal,
                    error = errors[CardField.RATE]?.message(),
                    help =
                        "% ${if (state.rateType == RateType.MV) "Mes vencido" else "Efectivo anual"}" +
                            (state.rate?.let { " · " + rateLabel(it, state.rateType) } ?: ""),
                )
            }
            // Figma `1:1652`: the two days are picked from a list, «Día 20» and «Día 5»
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayField("Día de corte", state.cutOffDay, actions.onCutOff, errors[CardField.CUT_OFF]?.message(), Modifier.weight(1f))
                DayField(
                    if (editing) "Día de pago" else "Límite de pago",
                    state.paymentDay,
                    actions.onPayment,
                    errors[CardField.PAYMENT]?.message(),
                    Modifier.weight(1f),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(CofferShapes.card)
                    .background(colors.background)
                    .border(1.dp, colors.border, CofferShapes.card)
                    .toggleable(state.firstInstallmentInterestFree, role = Role.Checkbox, onValueChange = actions.onFirstFree)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Checkbox(
                    checked = state.firstInstallmentInterestFree,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(checkedColor = colors.primary, checkmarkColor = colors.onPrimary),
                )
                Column(Modifier.weight(1f)) {
                    Text("Primera cuota sin intereses", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    Text(
                        "Las compras diferidas no pagan intereses en su primera cuota.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }
        }
        ApproximateNote(CARD_APPROXIMATE_NOTE, Modifier.padding(horizontal = 16.dp))
        // M4 (#88): the save button stays visible while the form scrolls
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(
                if (editing) "Guardar cambios" else "Guardar tarjeta de crédito",
                onClick = actions.onSave,
                icon = if (editing) Icons.Outlined.Check else Icons.Outlined.Save,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton("Cancelar", onClick = actions.onBack, kind = ButtonKind.Ghost, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Figma `1:1652`: the card as it is filled in: chip, name, available limit with its bar, last digits and the days. */
@Composable
private fun CardPreview(state: CardFormState) {
    val colors = CofferTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.background, CofferShapes.card)
            .border(1.dp, colors.border, CofferShapes.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(width = 28.dp, height = 20.dp)
                    .background(colors.borderHighlight, CofferShapes.pill)
                    .border(1.dp, colors.warning, CofferShapes.pill),
            )
            Icon(Icons.Outlined.Contactless, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.weight(1f))
            Text(state.name.ifBlank { "Tu tarjeta" }, style = MaterialTheme.typography.titleSmall, color = colors.primary, maxLines = 1)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Cupo disponible", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            Text(formatMoney(state.available), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            val share = if (state.limit > Money.ZERO) state.available.scaled.toFloat() / state.limit.scaled.toFloat() else 0f
            LinearProgressIndicator(
                progress = { share.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = colors.primary,
                trackColor = colors.border,
                drawStopIndicator = {},
                gapSize = 0.dp,
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (state.lastDigits.length == 4) "•••• ${state.lastDigits}" else "•••• ••••",
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary,
            )
            PreviewDay("Corte", state.cutOffDay)
            Spacer(Modifier.width(12.dp))
            PreviewDay("Límite", state.paymentDay)
        }
    }
}

@Composable
private fun PreviewDay(
    label: String,
    day: String,
) {
    Column(horizontalAlignment = Alignment.End) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = CofferTheme.colors.textSecondary)
        Text(if (day.isBlank()) "—" else "Día $day", style = MaterialTheme.typography.labelMedium, color = CofferTheme.colors.textPrimary)
    }
}

/** A day of the month (1 to 31) from a list; the value stays the text the view model already validates. */
@Composable
private fun DayField(
    label: String,
    day: String,
    onDay: (String) -> Unit,
    error: String?,
    modifier: Modifier,
) {
    val colors = CofferTheme.colors
    var open by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CofferShapes.card)
                    .background(colors.surface)
                    .border(1.dp, if (error != null) colors.expense else colors.border, CofferShapes.card)
                    .clickable(role = Role.DropdownList, onClickLabel = label) { open = true }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (day.isBlank()) "Elegir" else "Día $day",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (day.isBlank()) colors.textSecondary else colors.textPrimary,
                )
                Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = colors.textSecondary)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 280.dp)) {
                (1..31).forEach { n ->
                    DropdownMenuItem(text = { Text("Día $n") }, onClick = {
                        onDay("$n")
                        open = false
                    })
                }
            }
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.expense) }
    }
}
