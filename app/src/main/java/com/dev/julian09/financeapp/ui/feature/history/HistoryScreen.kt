package com.dev.julian09.financeapp.ui.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppChip
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.EmptyState
import com.dev.julian09.financeapp.ui.components.FabClearance
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.feature.transactions.categoryLabel
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@Composable
fun HistoryScreen(
    onOpenSettings: () -> Unit,
    onOpenTransaction: (UUID) -> Unit,
    modifier: Modifier = Modifier,
    avatarInitial: Char? = null,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryContent(
        state = state,
        actions =
            HistoryActions(
                onOpenSettings = onOpenSettings,
                onOpenTransaction = onOpenTransaction,
                onMonth = viewModel::selectMonth,
                onQuery = viewModel::setQuery,
                onType = viewModel::setType,
                onFilters = viewModel::applyFilters,
                onLoadMore = viewModel::loadMore,
            ),
        modifier = modifier,
        avatarInitial = avatarInitial,
    )
}

class HistoryActions(
    val onOpenSettings: () -> Unit = {},
    val onOpenTransaction: (UUID) -> Unit = {},
    val onMonth: (YearMonth) -> Unit = {},
    val onQuery: (String) -> Unit = {},
    val onType: (TransactionType?) -> Unit = {},
    val onFilters: (HistoryFilters) -> Unit = {},
    val onLoadMore: () -> Unit = {},
)

private val ES_CO: Locale = Locale.forLanguageTag("es-CO")

fun monthLabel(month: YearMonth): String =
    month.month.getDisplayName(TextStyle.FULL, ES_CO).replaceFirstChar { it.uppercase() } + " ${month.year}"

private val TYPE_CHIPS =
    listOf(
        null to "Todos",
        TransactionType.EXPENSE to "Gastos",
        TransactionType.INCOME to "Ingresos",
        TransactionType.TRANSFER to "Transferencias",
    )

/** Historial (S5-UI03): grouped by month and day, newest first (REQ-TRX-12). */
@Composable
fun HistoryContent(
    state: HistoryUiState?,
    actions: HistoryActions,
    modifier: Modifier = Modifier,
    avatarInitial: Char? = null,
    today: LocalDate = LocalDate.now(),
) {
    val colors = CofferTheme.colors
    var filtering by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        AppHeader(
            title = "Historial",
            actions = {
                if (state != null) {
                    IconButton(onClick = { filtering = true }) {
                        BadgedBox(badge = { if (state.filters.count > 0) Badge { Text("${state.filters.count}") } }) {
                            Icon(Icons.Outlined.FilterList, contentDescription = "Filtros", tint = colors.textPrimary)
                        }
                    }
                }
                Avatar(onClick = actions.onOpenSettings, initial = avatarInitial)
            },
        )
        if (state == null) return@Column
        val listState = rememberLazyListState()
        val atEnd by remember {
            derivedStateOf {
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ==
                    listState.layoutInfo.totalItemsCount - 1
            }
        }
        LaunchedEffect(atEnd, state.hasMore) { if (atEnd && state.hasMore) actions.onLoadMore() }
        val spansMonths =
            state.days
                .map { YearMonth.from(it.date) }
                .distinct()
                .size > 1
        // The field owns its text: echoing it back from the search flow moved the cursor while typing (E1, #75)
        var query by rememberSaveable { mutableStateOf(state.query) }
        LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = FabClearance), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.months) { m ->
                        AppChip(monthLabel(m), selected = !state.filters.customRange && m == state.month, onClick = { actions.onMonth(m) })
                    }
                }
            }
            item {
                AppTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        actions.onQuery(it)
                    },
                    label = "Buscar",
                    placeholder = "Nota, categoría, cuenta o etiqueta",
                    leadingIcon = Icons.Outlined.Search,
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TYPE_CHIPS) { (type, label) ->
                        val selected = if (type == null) state.filters.types.isEmpty() else state.filters.types == setOf(type)
                        AppChip(label, selected = selected, onClick = { actions.onType(type) })
                    }
                }
            }
            item { Summary(state) }
            if (state.days.isEmpty()) {
                item {
                    EmptyState(
                        title = "No hay movimientos",
                        message =
                            if (state.query.isBlank() && state.filters == HistoryFilters()) {
                                "Registra un gasto, un ingreso o una transferencia con el botón +."
                            } else {
                                "Prueba con otra búsqueda u otros filtros."
                            },
                    )
                }
            }
            state.days.forEachIndexed { i, day ->
                val month = YearMonth.from(day.date)
                if (spansMonths && (i == 0 || YearMonth.from(state.days[i - 1].date) != month)) {
                    item(
                        key = "m$month",
                    ) { Text(monthLabel(month), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary) }
                }
                item(key = day.date.toString()) { DayGroup(day, today, showTime = true, onOpen = actions.onOpenTransaction) }
            }
        }
    }
    if (filtering) {
        FiltersSheet(state.filtersOrDefault(), state?.choices ?: HistoryChoices(), onDismiss = { filtering = false }) {
            actions.onFilters(it)
            filtering = false
        }
    }
}

private fun HistoryUiState?.filtersOrDefault() = this?.filters ?: HistoryFilters()

private val RANGE = DateTimeFormatter.ofPattern("d MMM yyyy", ES_CO)

@Composable
private fun Summary(state: HistoryUiState) {
    val colors = CofferTheme.colors
    val f = state.filters
    val title =
        if (f.customRange) {
            "Resumen del ${RANGE.format(f.from).replace(".", "")} al ${RANGE.format(f.to).replace(".", "")}"
        } else {
            "Resumen de ${monthLabel(state.month)}"
        }
    AppCard {
        Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryItem("Ingresos", state.totals.income, MoneyKind.Income, Modifier.weight(1f))
            SummaryItem("Gastos", state.totals.expense, MoneyKind.Expense, Modifier.weight(1f))
            SummaryItem("Balance neto", state.totals.income - state.totals.expense, MoneyKind.Neutral, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    amount: Money,
    kind: MoneyKind,
    modifier: Modifier,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = CofferTheme.colors.textSecondary)
        MoneyText(amount, kind = if (amount == Money.ZERO) MoneyKind.Neutral else kind, style = MaterialTheme.typography.titleSmall)
    }
}

private fun <T> Set<T>.toggle(item: T) = if (item in this) this - item else this + item

/** Hoja · Filtros del historial: all combinable with each other and with the search (REQ-TRX-09, 10). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersSheet(
    initial: HistoryFilters,
    choices: HistoryChoices,
    onDismiss: () -> Unit,
    onApply: (HistoryFilters) -> Unit,
) {
    val colors = CofferTheme.colors
    var f by remember { mutableStateOf(initial) }
    AppBottomSheet(onDismiss = onDismiss, title = "Filtros", subtitle = "Se combinan entre sí y con la búsqueda.") {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Section("Tipo") {
                TYPE_CHIPS.drop(1).forEach { (type, label) ->
                    AppChip(label, type in f.types, onClick = {
                        f =
                            f.copy(types = f.types.toggle(type!!))
                    })
                }
            }
            Section("Categoría y subcategoría") {
                // "Padre > Sub", sorted so each subcategory follows its parent (E3, #77)
                choices.categories.map { it to categoryLabel(it, choices.categories) }.sortedBy { it.second }.forEach { (c, label) ->
                    AppChip(label, c.id in f.categoryIds, onClick = {
                        f =
                            f.copy(categoryIds = f.categoryIds.toggle(c.id))
                    })
                }
            }
            Section("Cuenta") {
                choices.accounts.forEach { a ->
                    AppChip(a.account.name, a.account.id in f.accountIds, onClick = {
                        f =
                            f.copy(accountIds = f.accountIds.toggle(a.account.id))
                    })
                }
            }
            if (choices.tags.isNotEmpty()) {
                Section("Etiqueta") {
                    choices.tags.forEach { t ->
                        AppChip("#${t.name}", t.id in f.tagIds, onClick = {
                            f =
                                f.copy(tagIds = f.tagIds.toggle(t.id))
                        })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField("Desde", f.from, Modifier.weight(1f)) { f = f.copy(from = it) }
                DateField("Hasta", f.to, Modifier.weight(1f)) { f = f.copy(to = it) }
            }
            if ((f.from == null) != (f.to == null)) {
                Text("Elige las dos fechas para usar un rango.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppButton("Limpiar", onClick = { onApply(HistoryFilters()) }, kind = ButtonKind.Secondary, modifier = Modifier.weight(1f))
                AppButton(
                    "Aplicar filtros",
                    onClick = {
                        // A range ending before it starts is read the other way round
                        val (a, b) = f.from to f.to
                        onApply(if (a != null && b != null && b < a) f.copy(from = b, to = a) else f)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Text(title, style = MaterialTheme.typography.bodyMedium, color = CofferTheme.colors.textSecondary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    value: LocalDate?,
    modifier: Modifier,
    onChange: (LocalDate) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    Box(modifier) {
        AppButton(
            value?.let { RANGE.format(it).replace(".", "") } ?: label,
            onClick = { picking = true },
            kind = ButtonKind.Secondary,
            icon = Icons.Outlined.CalendarToday,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (picking) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = value?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
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
