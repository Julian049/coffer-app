package com.dev.julian09.financeapp.ui.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.usecase.AVAILABLE_ACCOUNT_TYPES
import com.dev.julian09.financeapp.ui.components.ACCOUNT_COLORS
import com.dev.julian09.financeapp.ui.components.ACCOUNT_ICONS
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppChip
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.accountIcon
import com.dev.julian09.financeapp.ui.components.accountTypeLabel
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.theme.CofferTheme

@Composable
fun AccountFormScreen(
    onBack: () -> Unit,
    viewModel: AccountFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    AccountFormContent(
        state = state,
        onBack = onBack,
        onName = viewModel::setName,
        onBalance = viewModel::setBalance,
        onType = viewModel::setType,
        onIcon = viewModel::setIcon,
        onColor = viewModel::setColor,
        onInclude = viewModel::setIncludeInTotal,
        onSave = viewModel::save,
    )
}

private fun balanceText(digits: String) = if (digits.isEmpty()) "" else formatMoney(Money.ofPesos(digits.toLong())).removePrefix("$")

/** Nueva cuenta and Editar cuenta: the type is chosen once and never changes (S4-SRV01). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountFormContent(
    state: AccountFormState,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onBalance: (String) -> Unit,
    onType: (AccountType) -> Unit,
    onIcon: (String) -> Unit,
    onColor: (String) -> Unit,
    onInclude: (Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    val editing = state.editingId != null
    Column(modifier.fillMaxSize()) {
        AppHeader(title = if (editing) "Editar cuenta" else "Nueva cuenta", onBack = onBack)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!editing) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(56.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(accountIcon(state.icon, state.type), contentDescription = null, tint = colors.iconOnContainer)
                    }
                    Column {
                        Text("Previsualización", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        Text(state.name.ifBlank { "Nueva cuenta" }, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    }
                }
            }
            val nameField = @Composable {
                AppTextField(
                    value = state.name,
                    onValueChange = onName,
                    label = if (editing) "Nombre" else "Nombre de la cuenta",
                    placeholder = "ej. Bancolombia Nómina, Billetera",
                    error = state.error?.takeIf { it == LedgerError.NAME_REQUIRED || it == LedgerError.NAME_TOO_LONG }?.message(),
                )
            }
            val balanceField = @Composable {
                AppTextField(
                    value = balanceText(state.balanceDigits),
                    onValueChange = onBalance,
                    label = "Saldo inicial",
                    placeholder = "0",
                    keyboardType = KeyboardType.Number,
                    help = if (editing) "Cambiarlo recalcula el saldo: saldo inicial + ingresos − gastos ± transferencias." else null,
                )
            }
            if (editing) {
                nameField()
                balanceField()
                AppTextField(
                    value = accountTypeLabel(state.type),
                    onValueChange = {},
                    label = "Tipo de cuenta",
                    help = "El tipo no se puede cambiar después de crear la cuenta.",
                    enabled = false,
                )
            } else {
                balanceField()
                nameField()
                Label("Tipo de cuenta")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountType.entries.forEach { type ->
                        val available = type in AVAILABLE_ACCOUNT_TYPES
                        AppChip(
                            if (available) accountTypeLabel(type) else "${accountTypeLabel(type)} · Disponible pronto",
                            selected = state.type == type,
                            onClick = { if (available) onType(type) },
                            modifier = if (available) Modifier else Modifier.alpha(0.5f),
                        )
                    }
                }
                Label("Ícono representativo")
                AppCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ACCOUNT_ICONS.forEach { (key, icon) ->
                            val selected = state.icon == key
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .background(if (selected) colors.primaryContainer else Color.Transparent, CircleShape)
                                    .selectable(selected, role = Role.RadioButton, onClick = { onIcon(key) }),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = key,
                                    tint = if (selected) colors.onPrimaryContainer else colors.textSecondary,
                                )
                            }
                        }
                    }
                }
                Label("Distintivo de color")
                AppCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ACCOUNT_COLORS.forEach { hex ->
                            val selected = state.color == hex
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                    .then(if (selected) Modifier.border(2.dp, colors.background, CircleShape) else Modifier)
                                    .selectable(selected, role = Role.RadioButton, onClick = { onColor(hex) }),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = "Color elegido",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Incluir en el saldo total", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text(
                            "Si está activo, el dinero de esta cuenta se suma al saldo total visible en Inicio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Switch(
                        checked = state.includeInTotal,
                        onCheckedChange = onInclude,
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.primary, checkedThumbColor = colors.onPrimary),
                    )
                }
            }
            state.error?.takeIf { it == LedgerError.TYPE_NOT_AVAILABLE }?.let {
                Text(it.message(), style = MaterialTheme.typography.bodySmall, color = colors.expense)
            }
            AppButton(
                if (editing) "Guardar cambios" else "Crear cuenta",
                onClick = onSave,
                icon = if (editing) null else Icons.Outlined.AddCircleOutline,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = CofferTheme.colors.textSecondary)
}
