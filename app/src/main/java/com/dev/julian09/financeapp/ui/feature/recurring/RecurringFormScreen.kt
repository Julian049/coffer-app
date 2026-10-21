package com.dev.julian09.financeapp.ui.feature.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.Frequency
import com.dev.julian09.financeapp.domain.model.IntervalUnit
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.usecase.RecurringField
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppChip
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.PesosTransformation
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.feature.transactions.categoryLabel
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val ES_CO: Locale = Locale.forLanguageTag("es-CO")
private val SHORT_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", ES_CO)

/** "17 oct 2026" */
fun shortDate(date: LocalDate): String = SHORT_DATE.format(date).replace(".", "")

fun frequencyLabel(frequency: Frequency): String =
    when (frequency) {
        Frequency.WEEKLY -> "Semanal"
        Frequency.BIWEEKLY -> "Quincenal"
        Frequency.MONTHLY -> "Mensual"
        Frequency.QUARTERLY -> "Trimestral"
        Frequency.YEARLY -> "Anual"
        Frequency.CUSTOM -> "Personalizada"
    }

private fun unitLabel(unit: IntervalUnit) =
    when (unit) {
        IntervalUnit.DAY -> "días"
        IntervalUnit.WEEK -> "semanas"
        IntervalUnit.MONTH -> "meses"
    }

@Composable
fun RecurringFormScreen(
    onBack: () -> Unit,
    viewModel: RecurringFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val choices by viewModel.choices.collectAsStateWithLifecycle()
    // A new one asks for the notification permission if it is missing (S6-DAT02), then leaves
    val requestPermission = rememberNotificationPermissionRequest(onDone = onBack)
    LaunchedEffect(state.saved) {
        if (state.saved) {
            if (state.editingId == null) {
                requestPermission()
            } else {
                onBack()
            }
        }
    }
    RecurringFormContent(
        state = state,
        choices = choices,
        actions =
            RecurringFormActions(
                onBack = onBack,
                onName = viewModel::setName,
                onAmount = viewModel::setAmount,
                onAccount = viewModel::setAccount,
                onCategory = viewModel::setCategory,
                onStartDate = viewModel::setStartDate,
                onFrequency = viewModel::setFrequency,
                onInterval = viewModel::setInterval,
                onIntervalUnit = viewModel::setIntervalUnit,
                onSave = viewModel::save,
            ),
    )
}

class RecurringFormActions(
    val onBack: () -> Unit = {},
    val onName: (String) -> Unit = {},
    val onAmount: (String) -> Unit = {},
    val onAccount: (UUID) -> Unit = {},
    val onCategory: (UUID) -> Unit = {},
    val onStartDate: (LocalDate) -> Unit = {},
    val onFrequency: (Frequency) -> Unit = {},
    val onInterval: (String) -> Unit = {},
    val onIntervalUnit: (IntervalUnit) -> Unit = {},
    val onSave: () -> Unit = {},
)

/** Nuevo gasto recurrente (S6-UI01): Figma "6 · Pagos recurrentes". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurringFormContent(
    state: RecurringFormState,
    choices: RecurringChoices,
    actions: RecurringFormActions,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = if (state.editingId == null) "Nuevo gasto recurrente" else "Editar gasto recurrente", onBack = actions.onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppTextField(
                value = state.name,
                onValueChange = actions.onName,
                label = "Nombre",
                placeholder = "Plan celular",
                error = state.errors[RecurringField.NAME]?.message(),
            )
            AppTextField(
                value = state.amountDigits,
                visualTransformation = PesosTransformation(),
                onValueChange = actions.onAmount,
                label = "Monto",
                placeholder = "$ 0",
                keyboardType = KeyboardType.Number,
                error = state.errors[RecurringField.AMOUNT]?.message(),
            )
            Section("Cuenta", state.errors[RecurringField.ACCOUNT]) {
                if (choices.accounts.isEmpty()) {
                    Text("Crea una cuenta en Cuentas primero.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    choices.accounts.forEach { item ->
                        AppChip(
                            item.account.name,
                            selected = item.account.id == state.accountId,
                            onClick = { actions.onAccount(item.account.id) },
                        )
                    }
                }
            }
            CategoryRow(state.categoryId, choices.categories, state.errors[RecurringField.CATEGORY], actions.onCategory)
            StartDateRow(state.startDate, actions.onStartDate)
            Section("Frecuencia", null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Frequency.entries.forEach { f ->
                        AppChip(frequencyLabel(f), selected = f == state.frequency, onClick = { actions.onFrequency(f) })
                    }
                }
            }
            if (state.frequency == Frequency.CUSTOM) {
                AppCard {
                    Text("Personalizada", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cada", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        AppTextField(
                            value = state.intervalDigits,
                            onValueChange = actions.onInterval,
                            label = null,
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.width(64.dp),
                        )
                        IntervalUnit.entries.forEach { unit ->
                            AppChip(unitLabel(unit), selected = unit == state.intervalUnit, onClick = { actions.onIntervalUnit(unit) })
                        }
                    }
                    state.errors[RecurringField.INTERVAL]?.let {
                        Text(it.message(), style = MaterialTheme.typography.bodySmall, color = colors.expense)
                    }
                }
            }
            AppCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    Text(
                        "Aparecerá como pendiente ${days(choices.leadDays)} antes de cada fecha. " +
                            "Puedes cambiar la anticipación en Ajustes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }
        }
        // M4 (#88): the save button stays visible while the form scrolls
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(
                if (state.editingId == null) "Guardar recurrente" else "Guardar cambios",
                onClick = actions.onSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

fun days(n: Int) = if (n == 1) "1 día" else "$n días"

@Composable
private fun Section(
    title: String,
    error: LedgerError?,
    content: @Composable () -> Unit,
) {
    val colors = CofferTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        content()
        error?.let { Text(it.message(), style = MaterialTheme.typography.bodySmall, color = colors.expense) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryRow(
    selectedId: UUID?,
    categories: List<Category>,
    error: LedgerError?,
    onCategory: (UUID) -> Unit,
) {
    val colors = CofferTheme.colors
    var picking by rememberSaveable { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == selectedId }
    Section("Categoría", error) {
        AppCard(Modifier.fillMaxWidth(), onClick = { picking = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    selected?.let { categoryLabel(it, categories) } ?: "Elige una categoría",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected == null) colors.textSecondary else colors.textPrimary,
                )
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary)
            }
        }
    }
    if (picking) {
        AppBottomSheet(onDismiss = { picking = false }, title = "Categoría") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                categories.filter { it.parentId == null }.forEach { main ->
                    Text(main.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (listOf(main) + categories.filter { it.parentId == main.id }).forEach { c ->
                            AppChip(
                                if (c.id == main.id) "Solo ${c.name}" else c.name,
                                selected = c.id == selectedId,
                                onClick = {
                                    onCategory(c.id)
                                    picking = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDateRow(
    date: LocalDate,
    onChange: (LocalDate) -> Unit,
) {
    val colors = CofferTheme.colors
    var picking by rememberSaveable { mutableStateOf(false) }
    Section("Fecha de inicio", null) {
        AppCard(Modifier.fillMaxWidth(), onClick = { picking = true }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                Text(shortDate(date), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            }
        }
    }
    if (picking) {
        // The date picker works in UTC midnights
        val picker = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    picking = false
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancelar") } },
        ) { DatePicker(state = picker) }
    }
}
