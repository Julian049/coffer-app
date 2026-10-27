package com.dev.julian09.financeapp.ui.feature.reports

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.finance.PeriodSums
import com.dev.julian09.financeapp.domain.finance.ReportFilter
import com.dev.julian09.financeapp.domain.finance.ReportPeriod
import com.dev.julian09.financeapp.domain.finance.shareOf
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppChip
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.BubbleRow
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.CategoryGridSheet
import com.dev.julian09.financeapp.ui.components.IconBubble
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.PesosTransformation
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.iconOf
import com.dev.julian09.financeapp.ui.feature.history.DateField
import com.dev.julian09.financeapp.ui.feature.history.TagDot
import com.dev.julian09.financeapp.ui.feature.history.mostUsedAll
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ES_CO: Locale = Locale.forLanguageTag("es-CO")
private val MONTH = DateTimeFormatter.ofPattern("MMM", ES_CO)
private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", ES_CO)
private val MONTH_LONG = DateTimeFormatter.ofPattern("MMMM", ES_CO)

private val TYPES =
    listOf(
        TransactionType.EXPENSE to "Gastos",
        TransactionType.INCOME to "Ingresos",
        TransactionType.TRANSFER to "Transferencias",
    )

@Composable
fun SummaryScreen(
    onBack: () -> Unit,
    onOpenCategories: (LocalDate, LocalDate) -> Unit,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SummaryContent(state, onBack, viewModel::apply, viewModel::toggleMonth, onOpenCategories = {
        state
            ?.summary
            ?.shown
            ?.period
            ?.let { onOpenCategories(it.from, it.to) }
    })
}

/** Reportes · Resumen general (Figma `169:193`, `169:326`): the filters and the tapped month change every number. */
@Composable
fun SummaryContent(
    state: SummaryState?,
    onBack: () -> Unit,
    onApply: (SummaryFilters) -> Unit,
    onMonth: (YearMonth) -> Unit,
    onOpenCategories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filtering by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Resumen general", onBack = onBack, subtitle = state?.let { periodLabel(it.period) })
        if (state == null) return@Column
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FilterChips(state) { filtering = true }
            Totals(state)
            ByMonth(state, onMonth)
            WhereItWent(state, onOpenCategories)
        }
    }
    if (filtering && state != null) {
        SummaryFiltersSheet(state, onDismiss = { filtering = false }) {
            onApply(it)
            filtering = false
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChips(
    state: SummaryState,
    onOpen: () -> Unit,
) {
    val colors = CofferTheme.colors
    val f = state.filters
    val choices = state.choices
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // «Filtros · n» in espresso; every chip opens the same sheet for now (changes-ui-2.0 §3, pending of design)
        Row(
            Modifier
                .clip(CofferShapes.pill)
                .background(colors.primary)
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
            Text(
                if (f.count >
                    0
                ) {
                    "Filtros · ${f.count}"
                } else {
                    "Filtros"
                },
                style = MaterialTheme.typography.labelLarge,
                color = colors.onPrimary,
            )
        }
        AppChip(f.range.label, f.range != SummaryRange.LAST_6, onOpen)
        AppChip(
            f.filter.types
                .takeIf { it.isNotEmpty() }
                ?.let { set -> TYPES.filter { it.first in set }.joinToString { it.second } } ?: "Tipo",
            f.filter.types.isNotEmpty(),
            onOpen,
        )
        AppChip(
            chipLabel(
                "Categorías",
                f.filter.categoryIds.mapNotNull { id ->
                    choices.categories.firstOrNull { it.id == id }?.name
                },
            ),
            f.filter.categoryIds.isNotEmpty(),
            onOpen,
        )
        AppChip(
            chipLabel(
                "Etiquetas",
                f.filter.tagIds.mapNotNull { id ->
                    choices.tags.firstOrNull { it.id == id }?.name
                },
            ),
            f.filter.tagIds.isNotEmpty(),
            onOpen,
        )
        AppChip(
            chipLabel(
                "Cuentas",
                f.filter.accountIds.mapNotNull { id ->
                    choices.accounts
                        .firstOrNull { it.account.id == id }
                        ?.account
                        ?.name
                },
            ),
            f.filter.accountIds.isNotEmpty(),
            onOpen,
        )
        val min = f.filter.minAmount
        val max = f.filter.maxAmount
        AppChip(
            when {
                min != null && max != null -> "${shortMoney(min)} – ${shortMoney(max)}"
                min != null -> "Desde ${shortMoney(min)}"
                max != null -> "Hasta ${shortMoney(max)}"
                else -> "Valor"
            },
            min != null || max != null,
            onOpen,
        )
    }
}

/** «Comida», «Comida +2», or the plain name of the filter. */
private fun chipLabel(
    name: String,
    picked: List<String>,
): String =
    when (picked.size) {
        0 -> name
        1 -> picked.single()
        else -> "${picked.first()} +${picked.size - 1}"
    }

@Composable
private fun Totals(state: SummaryState) {
    val colors = CofferTheme.colors
    val report = state.summary.shown
    AppCard(Modifier.fillMaxWidth().animateContentSize(tween(300)), highlight = true) {
        Text(
            state.focus?.let { MONTH_YEAR.format(it).replaceFirstChar { c -> c.uppercase() } } ?: periodLabel(state.period),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        MoneyText(report.balance, style = MaterialTheme.typography.displaySmall)
        report.savingsRate?.let { rate ->
            Text(
                buildAnnotatedString {
                    if (rate >= 0) append("Ahorraste el ") else append("Gastaste ")
                    withStyle(SpanStyle(color = if (rate >= 0) colors.income else colors.expense, fontWeight = FontWeight.Medium)) {
                        append(if (rate >= 0) "$rate %" else "${-rate} % más")
                    }
                    append(" de lo que ganaste")
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        HorizontalDivider(thickness = 1.dp, color = colors.border)
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Ingresos", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                MoneyText(report.income, kind = MoneyKind.Income, style = MaterialTheme.typography.titleSmall)
            }
            Column(Modifier.weight(1f)) {
                Text("Gastos", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                MoneyText(report.expense, kind = MoneyKind.Expense, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/** A round step for the guides: 1, 2 or 5 times a power of ten, about a third of [top]. */
internal fun guideStep(top: Long): Long {
    if (top <= 0) return 1
    var power = 1L
    while (power * 10 <= top / 3) power *= 10
    return listOf(1L, 2L, 5L, 10L).map { it * power }.first { it * 3 >= top }
}

/** Ingresos y gastos por mes: grouped bars, guides, the running month with «*»; a tap focuses the month. */
@Composable
private fun ByMonth(
    state: SummaryState,
    onMonth: (YearMonth) -> Unit,
) {
    val colors = CofferTheme.colors
    val months = state.summary.months
    val top = months.maxOfOrNull { maxOf(it.income.scaled, it.expense.scaled) }?.coerceAtLeast(1) ?: 1
    val step = guideStep(top)
    val scale = (step * ((top + step - 1) / step)).toFloat()
    val barArea = 140.dp
    val thisMonth = YearMonth.from(state.today)
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ingresos y gastos por mes", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            if (state.focus != null) {
                Text(
                    "Ver los ${months.size} meses",
                    Modifier.clip(CofferShapes.pill).clickable { onMonth(state.focus) }.padding(4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Legend(colors.income, "Ingresos")
            Legend(colors.expense, "Gastos")
        }
        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            val guide = colors.border
            Canvas(Modifier.fillMaxWidth().height(barArea)) {
                var v = step
                while (v <= scale) {
                    val y = size.height - v / scale * size.height
                    drawLine(guide, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                    v += step
                }
            }
            // The guides' values on the left, as Figma's «1 M», «2 M», «3 M»
            generateSequence(step) { it + step }.takeWhile { it <= scale }.forEach { v ->
                Text(
                    shortMoney(Money(v)).replace("$", ""),
                    Modifier.offset(y = barArea * (1f - v / scale) - 14.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
            Row(Modifier.fillMaxWidth().padding(start = 28.dp)) {
                months.forEach { m -> MonthBars(m, state, scale, barArea, thisMonth, onMonth, Modifier.weight(1f)) }
            }
        }
        Text(
            "Toca un mes para ver su detalle." +
                if (months.any { YearMonth.from(it.period.from) == thisMonth }) {
                    " *" + MONTH_LONG.format(state.today).replaceFirstChar { it.uppercase() } + " va en curso."
                } else {
                    ""
                },
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun MonthBars(
    m: PeriodSums,
    state: SummaryState,
    scale: Float,
    barArea: androidx.compose.ui.unit.Dp,
    thisMonth: YearMonth,
    onMonth: (YearMonth) -> Unit,
    modifier: Modifier,
) {
    val colors = CofferTheme.colors
    val month = YearMonth.from(m.period.from)
    val focused = state.focus == month
    val dimmed by animateFloatAsState(if (state.focus == null || focused) 1f else 0.3f, tween(300), label = "dim")
    val label = MONTH.format(m.period.from).replace(".", "").replaceFirstChar { it.uppercase() } + if (month == thisMonth) "*" else ""
    Column(
        modifier
            .clickable(onClickLabel = if (focused) "Ver todos los meses" else "Ver $label") { onMonth(month) }
            .semantics { contentDescription = "$label: ingresos ${formatMoney(m.income)}, gastos ${formatMoney(m.expense)}" }
            .alpha(dimmed),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.height(barArea), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
            // Figma `169:326`: the values of the tapped month above its bars, one per line so they never wrap
            if (focused) {
                Text(
                    shortMoney(m.income).replace("$", ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.income,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    shortMoney(m.expense).replace("$", ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.expense,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(m.income to colors.income, m.expense to colors.expense).forEach { (value, color) ->
                    Box(
                        Modifier
                            .width(12.dp)
                            .height(barArea * (value.scaled / scale).coerceIn(0.01f, 1f) * if (focused) 0.75f else 1f)
                            .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                    )
                }
            }
        }
        Text(
            label,
            Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (focused) colors.textPrimary else colors.textSecondary,
            fontWeight = if (focused) FontWeight.Medium else null,
        )
    }
}

@Composable
private fun Legend(
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.bodySmall, color = CofferTheme.colors.textSecondary)
    }
}

/** «En qué se fue»: the small pie with its legend and shares; «Ver detalle» opens Gastos por categoría of the same period. */
@Composable
private fun WhereItWent(
    state: SummaryState,
    onOpen: () -> Unit,
) {
    val colors = CofferTheme.colors
    val report = state.summary.shown
    val sums = report.expenseByCategory
    if (sums.isEmpty()) return
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val months = state.summary.months.size
            Text(
                if (state.focus == null &&
                    months > 1
                ) {
                    "En qué se fue · $months meses"
                } else {
                    "En qué se fue"
                },
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                "Ver detalle",
                Modifier.clip(CofferShapes.pill).clickable(onClick = onOpen).padding(4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val slices = pieSlices(sums)
            DonutChart(slices, 128.dp, 24.dp, "Gastos ${formatMoney(report.expense)}") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Gastos", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    Text(shortMoney(report.expense), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // The legend names what the pie paints: the coloured categories and «Otras»
                val named = sums.filter { categoryColor(it.category) != null }
                named.forEach { s -> LegendLine(s.color(), s.category.name, shareOf(s.total, report.expense)) }
                val others = sums.filter { categoryColor(it.category) == null }.fold(Money.ZERO) { a, s -> a + s.total }
                if (others > Money.ZERO) LegendLine(OTHER_CATEGORY_COLOR, "Otras", shareOf(others, report.expense))
            }
        }
    }
}

@Composable
private fun LegendLine(
    color: Color,
    name: String,
    share: BigDecimal?,
) {
    val colors = CofferTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorSquare(color)
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.textPrimary, maxLines = 1)
        Text(share.percentLabel(), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    }
}

private fun <T> Set<T>.toggle(item: T) = if (item in this) this - item else this + item

/** Hoja · Filtros del resumen (Figma `169:462`): Periodo, Tipo, Categorías, Cuentas, Etiquetas and Valor. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryFiltersSheet(
    state: SummaryState,
    onDismiss: () -> Unit,
    onApply: (SummaryFilters) -> Unit,
) {
    val colors = CofferTheme.colors
    val choices = state.choices
    var f by remember { mutableStateOf(state.filters) }
    var min by remember {
        mutableStateOf(
            f.filter.minAmount
                ?.toBigDecimal()
                ?.toBigInteger()
                ?.toString()
                .orEmpty(),
        )
    }
    var max by remember {
        mutableStateOf(
            f.filter.maxAmount
                ?.toBigDecimal()
                ?.toBigInteger()
                ?.toString()
                .orEmpty(),
        )
    }
    var pickingCategories by remember { mutableStateOf(false) }

    fun filter(change: (ReportFilter) -> ReportFilter) {
        f = f.copy(filter = change(f.filter))
    }
    if (pickingCategories) {
        CategoryGridSheet(
            mostUsedAll(choices.categories, emptyMap()),
            choices.categories,
            selected = f.filter.categoryIds,
            multiple = true,
            onApply = {
                filter { r -> r.copy(categoryIds = it) }
                pickingCategories = false
            },
            onDismiss = { pickingCategories = false },
        )
        return
    }
    AppBottomSheet(
        onDismiss = onDismiss,
        title = "Filtros del resumen",
        subtitle = "Cambian los totales, el histórico y la distribución.",
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppButton("Limpiar", onClick = { onApply(SummaryFilters()) }, kind = ButtonKind.Secondary, modifier = Modifier.weight(1f))
                AppButton(
                    "Aplicar filtros",
                    onClick = {
                        val custom = f.custom?.let { p -> if (p.to < p.from) ReportPeriod(p.to, p.from) else p }
                        onApply(
                            f.copy(
                                custom = custom,
                                filter =
                                    f.filter.copy(
                                        minAmount = min.toLongOrNull()?.let(Money::ofPesos),
                                        maxAmount = max.toLongOrNull()?.let(Money::ofPesos),
                                    ),
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Section("Periodo") {
                SummaryRange.entries.forEach { r -> AppChip(r.label, f.range == r, onClick = { f = f.copy(range = r) }) }
            }
            if (f.range == SummaryRange.CUSTOM) {
                val shown = f.custom ?: SummaryRange.LAST_6.period(state.today, null)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateField("Desde", shown.from, Modifier.weight(1f)) { f = f.copy(custom = shown.copy(from = it)) }
                    DateField("Hasta", shown.to, Modifier.weight(1f)) { f = f.copy(custom = shown.copy(to = it)) }
                }
            }
            Section("Tipo") {
                TYPES.forEach { (type, label) ->
                    AppChip(label, type in f.filter.types, onClick = { filter { it.copy(types = it.types.toggle(type)) } })
                }
            }
            Text("Categorías", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            val ranked = mostUsedAll(choices.categories, emptyMap()).filter { it.parentId == null }
            val quick = (ranked.filter { it.id in f.filter.categoryIds } + ranked).distinctBy { it.id }.take(4)
            BubbleRow(quick.size, onMore = { pickingCategories = true }) { i, cell ->
                val c = quick[i]
                val on = c.id in f.filter.categoryIds
                IconBubble(c.name, iconOf(c, choices.categories), selected = on, checked = on, multiple = true, modifier = cell) {
                    filter { it.copy(categoryIds = it.categoryIds.toggle(c.id)) }
                }
            }
            Section("Cuentas") {
                choices.accounts.forEach { a ->
                    AppChip(a.account.name, a.account.id in f.filter.accountIds, onClick = {
                        filter { it.copy(accountIds = it.accountIds.toggle(a.account.id)) }
                    })
                }
            }
            if (choices.tags.isNotEmpty()) {
                Section("Etiquetas") {
                    choices.tags.forEach { t ->
                        val on = t.id in f.filter.tagIds
                        Row(
                            Modifier
                                .clip(CofferShapes.pill)
                                .background(if (on) colors.primaryContainer else colors.surface)
                                .clickable(role = Role.Checkbox) { filter { it.copy(tagIds = it.tagIds.toggle(t.id)) } }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            TagDot(t.shownColor)
                            Text(
                                t.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (on) colors.onPrimaryContainer else colors.textPrimary,
                            )
                        }
                    }
                }
            }
            Text("Valor", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(
                    value = min,
                    onValueChange = { v -> min = v.filter(Char::isDigit) },
                    label = "Mínimo",
                    modifier = Modifier.weight(1f),
                    visualTransformation = remember { PesosTransformation() },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                )
                AppTextField(
                    value = max,
                    onValueChange = { v -> max = v.filter(Char::isDigit) },
                    label = "Máximo",
                    modifier = Modifier.weight(1f),
                    visualTransformation = remember { PesosTransformation() },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
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
