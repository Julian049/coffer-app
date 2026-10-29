package com.dev.julian09.financeapp.ui.feature.history

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.finance.FoxState
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.TransactionDraft
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppChip
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.BubbleRow
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.CategoryGridSheet
import com.dev.julian09.financeapp.ui.components.EmptyState
import com.dev.julian09.financeapp.ui.components.FabClearance
import com.dev.julian09.financeapp.ui.components.FoxMascot
import com.dev.julian09.financeapp.ui.components.IconBubble
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.iconOf
import com.dev.julian09.financeapp.ui.navigation.HistoryLink
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
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
    onReviewDraft: (UUID) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val link by HistoryLink.filters.collectAsStateWithLifecycle()
    LaunchedEffect(link) {
        link?.let {
            viewModel.applyFilters(it)
            HistoryLink.filters.value = null
        }
    }
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
                onApplyDraft = { viewModel.apply(it, onReviewDraft) },
                onReviewDraft = onReviewDraft,
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
    val onApplyDraft: (UUID) -> Unit = {},
    val onReviewDraft: (UUID) -> Unit = {},
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
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = FabClearance),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
            // motion.md §7: the section and the days below move together when a draft is applied or discarded
            if (state.drafts.isNotEmpty()) {
                item(key = "drafts") {
                    DraftsSection(
                        state.drafts,
                        actions,
                        today,
                        Modifier.animateItem(fadeInSpec = tween(400), fadeOutSpec = tween(400), placementSpec = tween(400)),
                    )
                }
            }
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
                item(key = day.date.toString()) {
                    DayGroup(
                        day,
                        today,
                        Modifier.animateItem(placementSpec = tween(400)).animateContentSize(tween(400)),
                        showTime = true,
                        onOpen = actions.onOpenTransaction,
                    )
                }
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                state.fox?.takeUnless { f.customRange }?.let {
                    Text(monthMood(it.state), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
            // M14 (#97): how the month closed, or how it goes when it is the current one
            state.fox?.takeUnless { f.customRange }?.let {
                // INC-07: the same animated fox as Inicio
                FoxMascot(it.state.name.lowercase(), size = 48.dp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryItem("Ingresos", state.totals.income, MoneyKind.Income, Modifier.weight(1f))
            SummaryItem("Gastos", state.totals.expense, MoneyKind.Expense, Modifier.weight(1f))
            SummaryItem("Balance neto", state.totals.income - state.totals.expense, MoneyKind.Neutral, Modifier.weight(1f))
        }
    }
}

private fun monthMood(state: FoxState) =
    when (state) {
        FoxState.RESTING -> "Sin movimientos"
        FoxState.DEBT -> "Saldo en rojo"
        FoxState.ATTENTIVE -> "Con un pago cerca"
        FoxState.WORRIED -> "Gastos por encima de los ingresos"
        FoxState.GOAL -> "Mes anterior cerrado en positivo"
        FoxState.GOOD -> "Ingresos por encima de los gastos"
        FoxState.NORMAL -> "Todo en orden"
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
    var pickingCategories by remember { mutableStateOf(false) }
    if (pickingCategories) {
        CategoryGridSheet(
            mostUsedAll(choices.categories, choices.usage),
            choices.categories,
            selected = f.categoryIds,
            multiple = true,
            onApply = {
                f = f.copy(categoryIds = it)
                pickingCategories = false
            },
            onDismiss = { pickingCategories = false },
        )
        return
    }
    AppBottomSheet(onDismiss = onDismiss, title = "Filtros", subtitle = "Se combinan entre sí y con la búsqueda.") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Section("Tipo") {
                TYPE_CHIPS.drop(1).forEach { (type, label) ->
                    AppChip(label, type in f.types, onClick = {
                        f =
                            f.copy(types = f.types.toggle(type!!))
                    })
                }
            }
            // Figma, Hoja · Filtros del historial: the icon row with several choices; «Más» opens Elegir categorías (varias)
            Text("Categorías", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            val ranked = mostUsedAll(choices.categories, choices.usage)
            val quick = (ranked.filter { it.id in f.categoryIds } + ranked).distinctBy { it.id }.take(4)
            BubbleRow(quick.size, onMore = { pickingCategories = true }) { i, cell ->
                val c = quick[i]
                val on = c.id in f.categoryIds
                IconBubble(c.name, iconOf(c, choices.categories), selected = on, checked = on, multiple = true, modifier = cell) {
                    f = f.copy(categoryIds = f.categoryIds.toggle(c.id))
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
internal fun DateField(
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

/** Every category, the most used first, the main ones before their subcategories on a tie (S13). */
internal fun mostUsedAll(
    categories: List<Category>,
    usage: Map<UUID, Int>,
): List<Category> =
    categories.sortedWith(compareByDescending<Category> { usage[it.id] ?: 0 }.thenBy { it.parentId != null }.thenBy { it.sortOrder })

/**
 * «Por revisar» (changes-ui-2.0 §1, REQ-COR-04): between the summary and the first day, in a dashed card — not real yet —
 * with gray amounts, no time and a ✓ to apply as it came.
 */
@Composable
private fun DraftsSection(
    drafts: List<TransactionDraft>,
    actions: HistoryActions,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.padding(horizontal = 4.dp)) {
            // A count, not an amount: it cross-fades (motion.md §7)
            Crossfade(drafts.size, animationSpec = tween(150), label = "drafts") { n ->
                Text("Por revisar · $n", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            }
            Text(
                "Llegaron por correo. No afectan tu saldo hasta que las apliques.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(CofferShapes.card)
                .background(colors.surface)
                .drawBehind {
                    drawRoundRect(
                        colors.borderHighlight,
                        style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                    )
                }.animateContentSize(tween(400)),
        ) {
            drafts.forEachIndexed { i, d ->
                key(d.id) {
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = colors.border)
                    DraftRow(d, actions, today)
                }
            }
        }
    }
}

private val DRAFT_DAY = DateTimeFormatter.ofPattern("d MMM", ES_CO)

@Composable
private fun DraftRow(
    d: TransactionDraft,
    actions: HistoryActions,
    today: LocalDate,
) {
    val colors = CofferTheme.colors
    var applying by remember { mutableStateOf(false) }
    // The ✓ fills in 100 ms (motion.short) and the row leaves without waiting for anything else
    val fill by animateColorAsState(if (applying) colors.primary else colors.surface, tween(100), label = "apply")
    // Back to empty when the draft needed Revisar transacción instead (it stays in the list)
    LaunchedEffect(applying) {
        if (applying) {
            delay(600)
            applying = false
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Revisar", onClick = { actions.onReviewDraft(d.id) })
            .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Email, contentDescription = null, tint = colors.iconOnContainer, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(d.merchant ?: d.source.bank, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, maxLines = 1)
            val day = d.occurredAt.atZone(ZoneId.systemDefault()).toLocalDate()
            val digits = d.lastDigits?.let { " · *$it" }.orEmpty()
            Text(
                "${d.source.bank}$digits · ${if (day == today) "Hoy" else DRAFT_DAY.format(day).replace(".", "")}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        // Gray on purpose, whatever the type: it is not real yet
        Text(
            (if (d.type == TransactionType.EXPENSE) "−" else "+") + formatMoney(d.amount),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textSecondary,
        )
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = "Aplicar") {
                    applying = true
                    actions.onApplyDraft(d.id)
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(40.dp).background(fill, CircleShape).border(1.5.dp, colors.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = "Aplicar",
                    tint = if (applying) colors.onPrimary else colors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
