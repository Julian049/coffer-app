package com.dev.julian09.financeapp.ui.feature.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.TransactionField
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppChip
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.SegmentedSelector
import com.dev.julian09.financeapp.ui.components.accountIcon
import com.dev.julian09.financeapp.ui.components.categoryIcon
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Composable
fun TransactionFormScreen(
    onBack: () -> Unit,
    viewModel: TransactionFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val choices by viewModel.choices.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    TransactionFormContent(
        state = state,
        choices = choices,
        actions =
            TransactionFormActions(
                onBack = onBack,
                onType = viewModel::setType,
                onAmount = viewModel::setAmount,
                onCategory = viewModel::setCategory,
                onAccount = viewModel::setAccount,
                onDestination = viewModel::setDestination,
                onSwap = viewModel::swapAccounts,
                onOccurredAt = viewModel::setOccurredAt,
                onTag = viewModel::toggleTag,
                onNewTag = viewModel::newTag,
                onClearTagError = viewModel::clearTagError,
                onNote = viewModel::setNote,
                onSave = viewModel::save,
            ),
    )
}

class TransactionFormActions(
    val onBack: () -> Unit = {},
    val onType: (TransactionType) -> Unit = {},
    val onAmount: (String) -> Unit = {},
    val onCategory: (UUID) -> Unit = {},
    val onAccount: (UUID) -> Unit = {},
    val onDestination: (UUID) -> Unit = {},
    val onSwap: () -> Unit = {},
    val onOccurredAt: (Instant) -> Unit = {},
    val onTag: (UUID) -> Unit = {},
    val onNewTag: (String) -> Unit = {},
    val onClearTagError: () -> Unit = {},
    val onNote: (String) -> Unit = {},
    val onSave: () -> Unit = {},
)

private val TYPES = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
private val ES_CO: Locale = Locale.forLanguageTag("es-CO")
private val DATE = DateTimeFormatter.ofPattern("d 'de' MMM 'de' yyyy", ES_CO)
private val TIME = DateTimeFormatter.ofPattern("h:mm a", ES_CO)

/** "Hoy, 7 de oct de 2026 · 1:10 p. m." */
fun dateTimeLabel(
    instant: Instant,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val at = instant.atZone(zone)
    val prefix =
        when (at.toLocalDate()) {
            today -> "Hoy, "
            today.minusDays(1) -> "Ayer, "
            else -> ""
        }
    return prefix + DATE.format(at).replace(".", "") + " · " + TIME.format(at)
}

/** Nueva transacción (S5-UI01): ≤ 4 interactions for an expense, the transfer tab one tap away (RNF-14, REQ-TRX-04). */
@Composable
fun TransactionFormContent(
    state: TransactionFormState,
    choices: TransactionChoices,
    actions: TransactionFormActions,
    modifier: Modifier = Modifier,
) {
    val transfer = state.type == TransactionType.TRANSFER
    Column(modifier.fillMaxSize()) {
        AppHeader(title = if (state.editingId == null) "Nueva transacción" else "Editar transacción", onBack = actions.onBack)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SegmentedSelector(
                listOf("Gasto", "Ingreso", "Transferencia"),
                selectedIndex = TYPES.indexOf(state.type),
                onSelect = { actions.onType(TYPES[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
            AmountCard(state, actions.onAmount)
            if (transfer) {
                AccountPicker(
                    "Cuenta de origen",
                    choices.accounts,
                    state.accountId,
                    state.errors[TransactionField.ACCOUNT],
                    actions.onAccount,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = actions.onSwap) {
                        Icon(
                            Icons.Outlined.SwapVert,
                            contentDescription = "Intercambiar origen y destino",
                            tint = CofferTheme.colors.iconOnContainer,
                        )
                    }
                }
                AccountPicker(
                    "Cuenta de destino",
                    choices.accounts,
                    state.destinationId,
                    state.errors[TransactionField.DESTINATION],
                    actions.onDestination,
                )
            } else {
                CategoryCard(state, choices.categories, actions.onCategory)
            }
            DateTimeCard(state.occurredAt, actions.onOccurredAt)
            if (!transfer) {
                AccountPicker("Cuenta", choices.accounts, state.accountId, state.errors[TransactionField.ACCOUNT], actions.onAccount)
            }
            AppCard {
                if (!transfer) TagPicker(state, choices, actions)
                AppTextField(
                    value = state.note,
                    onValueChange = actions.onNote,
                    label = "Nota o concepto",
                    placeholder = if (transfer) "Retiro cajero para efectivo semanal" else "Almuerzo en restaurante",
                    error = state.errors[TransactionField.NOTE]?.message(),
                )
            }
            AppButton(
                when {
                    state.editingId != null -> "Guardar cambios"
                    transfer -> "Confirmar transferencia"
                    else -> "Guardar transacción"
                },
                onClick = actions.onSave,
                icon = Icons.Outlined.Check,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AmountCard(
    state: TransactionFormState,
    onAmount: (String) -> Unit,
) {
    val colors = CofferTheme.colors
    val error = state.errors[TransactionField.AMOUNT]
    AppCard(Modifier.fillMaxWidth()) {
        Text(
            if (state.type == TransactionType.TRANSFER) "Monto a transferir" else "Monto de la transacción",
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$", style = MaterialTheme.typography.headlineMedium, color = colors.textSecondary)
            BasicTextField(
                value = if (state.amountDigits.isEmpty()) "" else formatMoney(Money.ofPesos(state.amountDigits.toLong())).removePrefix("$"),
                onValueChange = onAmount,
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle =
                    MaterialTheme.typography.displaySmall.merge(
                        TextStyle(color = colors.textPrimary, textAlign = TextAlign.Center),
                    ),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (state.amountDigits.isEmpty()) {
                            Text("0", style = MaterialTheme.typography.displaySmall, color = colors.textSecondary)
                        }
                        inner()
                    }
                },
            )
        }
        error?.let { Text(it.message(), style = MaterialTheme.typography.bodySmall, color = colors.expense) }
    }
}

private fun categoryLabel(
    category: Category,
    all: List<Category>,
) = category.parentId?.let { parent -> all.firstOrNull { it.id == parent }?.name + " > " + category.name } ?: category.name

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryCard(
    state: TransactionFormState,
    categories: List<Category>,
    onCategory: (UUID) -> Unit,
) {
    val colors = CofferTheme.colors
    var picking by rememberSaveable { mutableStateOf(false) }
    val kind = if (state.type == TransactionType.EXPENSE) CategoryKind.EXPENSE else CategoryKind.INCOME
    val ofKind = categories.filter { it.kind == kind }
    val selected = ofKind.firstOrNull { it.id == state.categoryId }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Categoría", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            TextButton(onClick = { picking = true }) { Text("Ver todas", color = colors.primary) }
        }
        if (selected != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircleIcon(categoryIcon(selected.icon ?: ofKind.firstOrNull { it.id == selected.parentId }?.icon))
                Text(categoryLabel(selected, ofKind), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            }
        }
        // Quick choice: the main categories; their subcategories are in "Ver todas"
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 8.dp)) {
            items(ofKind.filter { it.parentId == null }, key = { it.id }) { c ->
                AppChip(c.name, selected = c.id == state.categoryId, onClick = { onCategory(c.id) })
            }
        }
        state.errors[TransactionField.CATEGORY]?.let {
            Text(
                it.message(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.expense,
            )
        }
    }
    if (picking) {
        AppBottomSheet(onDismiss = { picking = false }, title = "Categoría") {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ofKind.filter { it.parentId == null }.forEach { main ->
                    Text(main.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (listOf(main) + ofKind.filter { it.parentId == main.id }).forEach { c ->
                            AppChip(
                                if (c.id == main.id) "Solo ${c.name}" else c.name,
                                selected = c.id == state.categoryId,
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

@Composable
private fun CircleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val colors = CofferTheme.colors
    Box(Modifier.size(40.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = colors.iconOnContainer, modifier = Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeCard(
    occurredAt: Instant,
    onChange: (Instant) -> Unit,
) {
    val colors = CofferTheme.colors
    val zone = ZoneId.systemDefault()
    var step by remember { mutableStateOf(0) } // 0 closed, 1 date, 2 time
    var date by remember { mutableStateOf(occurredAt.atZone(zone).toLocalDate()) }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircleIcon(Icons.Outlined.Schedule)
            Column(Modifier.weight(1f)) {
                Text("Fecha y hora", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                Text(dateTimeLabel(occurredAt), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            }
            TextButton(onClick = { step = 1 }) { Text("Modificar", color = colors.primary) }
        }
    }
    if (step == 1) {
        // The date picker works in UTC midnights
        val picker = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    step = 2
                }) { Text("Siguiente") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("Cancelar") } },
        ) { DatePicker(state = picker) }
    }
    if (step == 2) {
        val time = occurredAt.atZone(zone).toLocalTime()
        val picker = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)
        AlertDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    onChange(LocalDateTime.of(date, LocalTime.of(picker.hour, picker.minute)).atZone(zone).toInstant())
                    step = 0
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("Cancelar") } },
            text = { TimePicker(state = picker) },
        )
    }
}

@Composable
private fun AccountPicker(
    title: String,
    accounts: List<AccountBalance>,
    selectedId: UUID?,
    error: com.dev.julian09.financeapp.domain.model.LedgerError?,
    onSelect: (UUID) -> Unit,
) {
    val colors = CofferTheme.colors
    AppCard {
        Text(title, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        if (accounts.isEmpty()) {
            Text(
                "Crea una cuenta en Cuentas para registrar movimientos.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 8.dp)) {
            items(accounts, key = { it.account.id }) { item ->
                val selected = item.account.id == selectedId
                Surface(
                    modifier =
                        Modifier
                            .width(
                                180.dp,
                            ).selectable(selected, role = Role.RadioButton, onClick = { onSelect(item.account.id) }),
                    shape = CofferShapes.card,
                    color = if (selected) colors.primaryContainer else colors.surface,
                    border = BorderStroke(1.dp, if (selected) colors.onPrimaryContainer else colors.border),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircleIcon(accountIcon(item.account.icon, item.account.type))
                        Column {
                            Text(item.account.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, maxLines = 1)
                            Text(formatMoney(item.balance), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
        error?.let { Text(it.message(), style = MaterialTheme.typography.bodySmall, color = colors.expense) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagPicker(
    state: TransactionFormState,
    choices: TransactionChoices,
    actions: TransactionFormActions,
) {
    val colors = CofferTheme.colors
    var creating by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    Text("Etiqueta", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.tags.forEach { tag -> AppChip("#${tag.name}", selected = tag.id == state.tagId, onClick = { actions.onTag(tag.id) }) }
        Surface(
            modifier = Modifier.selectable(false, role = Role.Button, onClick = { creating = true }),
            shape = CofferShapes.pill,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                Text("Nueva etiqueta", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
            }
        }
    }
    // A created tag gets selected and closes the dialog; an error keeps it open
    LaunchedEffect(state.createdTags) {
        creating = false
        name = ""
    }
    if (creating) {
        AlertDialog(
            onDismissRequest = {
                creating = false
                actions.onClearTagError()
            },
            title = { Text("Nueva etiqueta") },
            text = {
                AppTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        actions.onClearTagError()
                    },
                    label = "Nombre",
                    placeholder = "Viaje Cartagena",
                    error = state.tagError?.message(),
                )
            },
            confirmButton = { TextButton(onClick = { actions.onNewTag(name) }) { Text("Crear") } },
            dismissButton = {
                TextButton(onClick = {
                    creating = false
                    actions.onClearTagError()
                }) { Text("Cancelar") }
            },
        )
    }
}
