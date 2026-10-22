package com.dev.julian09.financeapp.ui.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.ui.components.ACCOUNT_COLORS
import com.dev.julian09.financeapp.ui.components.ACCOUNT_ICONS
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.IconRow
import com.dev.julian09.financeapp.ui.components.PesosTransformation
import com.dev.julian09.financeapp.ui.components.accountIcon
import com.dev.julian09.financeapp.ui.components.accountTypeLabel
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.feature.cards.CardFormScreen
import com.dev.julian09.financeapp.ui.feature.loans.LoanEditScreen
import com.dev.julian09.financeapp.ui.feature.loans.LoanFormScreen
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme

@Composable
fun AccountFormScreen(
    onBack: () -> Unit,
    viewModel: AccountFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    // A card has its own form, for a new one and for the edit (S7-UI01)
    if (state.type == AccountType.CREDIT_CARD) {
        CardFormScreen(onBack = onBack, name = state.name)
        return
    }
    // A loan is created from Gestión de cuentas → Préstamos (S12-UI05) and has its own edit (S12-DOM01)
    if (state.type == AccountType.LOAN) {
        if (state.editingId == null) LoanFormScreen(onBack = onBack, name = state.name) else LoanEditScreen(onBack = onBack)
        return
    }
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

/** Types offered by Nueva cuenta (Figma `1:2293`). */
private val NEW_ACCOUNT_TYPES = listOf(AccountType.CASH, AccountType.SAVINGS, AccountType.CREDIT_CARD)

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
    // S12-UI09: the bottom button rides on the keyboard
    Column(modifier.fillMaxSize().imePadding()) {
        AppHeader(title = if (editing) "Editar cuenta" else "Nueva cuenta", onBack = onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
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
                    value = state.balanceDigits,
                    visualTransformation = PesosTransformation(symbol = false),
                    onValueChange = onBalance,
                    label = "Saldo inicial",
                    placeholder = "0",
                    keyboardType = KeyboardType.Number,
                    help = null,
                )
            }
            if (editing) {
                // Editar cuenta (Figma `20:745`): the type and the opening balance are fixed
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(48.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(accountIcon(state.icon, state.type), contentDescription = null, tint = colors.iconOnContainer)
                        }
                        Column {
                            Text(state.name.ifBlank { "Cuenta" }, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text(accountTypeLabel(state.type), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                    Text(
                        "El tipo y el saldo inicial no se pueden cambiar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                nameField()
            } else {
                balanceField()
                nameField()
                Label("Tipo de cuenta")
                // S12-UI05: a loan is not an account to create here; it has its own section in Gestión de cuentas
                // Figma `1:2293`: three equal boxes, icon above and the name in two lines at most
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NEW_ACCOUNT_TYPES.forEach { type ->
                        val selected = state.type == type
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(CofferShapes.card)
                                .background(if (selected) colors.primaryContainer else colors.surface)
                                .border(1.dp, if (selected) colors.onPrimaryContainer else colors.border, CofferShapes.card)
                                .selectable(selected, role = Role.RadioButton, onClick = { onType(type) })
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                accountIcon(null, type),
                                contentDescription = null,
                                tint = if (selected) colors.onPrimaryContainer else colors.iconOnContainer,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                accountTypeLabel(type),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) colors.onPrimaryContainer else colors.textPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
            run {
                Label(if (editing) "Ícono" else "Ícono representativo")
                // Figma, S13: five icons and «Más», the same states as the category pickers
                IconRow(state.icon, ACCOUNT_ICONS.map { it.first }, onPick = onIcon)
                Label(if (editing) "Color" else "Distintivo de color")
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
        }
        // M4 (#88): the save button stays visible while the form scrolls
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
