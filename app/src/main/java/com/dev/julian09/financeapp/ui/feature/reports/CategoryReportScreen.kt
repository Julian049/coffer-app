package com.dev.julian09.financeapp.ui.feature.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.finance.CategoryDetail
import com.dev.julian09.financeapp.domain.finance.percentOf
import com.dev.julian09.financeapp.domain.finance.shareOf
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.EmptyState
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.feature.history.HistoryDay
import com.dev.julian09.financeapp.ui.feature.history.HistoryFilters
import com.dev.julian09.financeapp.ui.feature.history.TagDots
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

private val ES_CO: Locale = Locale.forLanguageTag("es-CO")
private val MONTH_LONG = DateTimeFormatter.ofPattern("MMMM", ES_CO)
private val MONTH = DateTimeFormatter.ofPattern("MMM", ES_CO)

/** How many movements the detail lists before «Ver los n en Historial». */
private const val LAST_MOVEMENTS = 5

@Composable
fun CategoryReportScreen(
    onBack: () -> Unit,
    onOpenTransaction: (UUID) -> Unit,
    onOpenHistory: (HistoryFilters) -> Unit,
    viewModel: CategoryReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CategoryReportContent(state, onBack, onOpenTransaction, onOpenHistory = {
        onOpenHistory(HistoryFilters(categoryIds = setOf(viewModel.categoryId), from = viewModel.month.from, to = viewModel.month.to))
    })
}

/** Reportes · Detalle de categoría (Figma `168:301`). */
@Composable
fun CategoryReportContent(
    state: CategoryReportState?,
    onBack: () -> Unit,
    onOpenTransaction: (UUID) -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    val detail = state?.detail
    Column(modifier.fillMaxSize()) {
        AppHeader(
            title = detail?.category?.name ?: "Categoría",
            onBack = onBack,
            subtitle = state?.let { periodLabel(it.month) },
        )
        if (state == null) return@Column
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (detail == null) {
                EmptyState(title = "Sin gastos en esta categoría", message = "Aquí verás cuánto gastas en ella mes a mes.")
                return@Column
            }
            Headline(detail, state)
            if (detail.months.size > 1) Months(detail, state)
            if (detail.subcategories.isNotEmpty()) Subcategories(detail)
            if (state.days.isNotEmpty()) Movements(state, onOpenTransaction, onOpenHistory)
        }
    }
}

private val ROW_DAY = DateTimeFormatter.ofPattern("d MMM", ES_CO)

/** Figma `168:301`: the last movements in one card, with «Ver los n en Historial» at its foot. */
@Composable
private fun Movements(
    state: CategoryReportState,
    onOpenTransaction: (UUID) -> Unit,
    onOpenHistory: () -> Unit,
) {
    val colors = CofferTheme.colors
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Movimientos", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text("${state.count} este mes", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        lastRows(state.days, LAST_MOVEMENTS).forEach { day ->
            day.rows.forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClickLabel = "Ver detalle",
                        ) { row.id?.let(onOpenTransaction) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                row.title,
                                Modifier.weight(1f, fill = false),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textPrimary,
                                maxLines = 1,
                            )
                            TagDots(row.tagColors)
                        }
                        Text(
                            listOfNotNull(row.categoryName, ROW_DAY.format(day.date).replace(".", "")).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    MoneyText(row.amount, kind = MoneyKind.Expense)
                }
            }
        }
        Text(
            "Ver los ${state.count} en Historial",
            Modifier
                .fillMaxWidth()
                .clip(CofferShapes.pill)
                .clickable(onClick = onOpenHistory)
                .padding(8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

/** The newest [n] rows, still grouped by day. */
private fun lastRows(
    days: List<HistoryDay>,
    n: Int,
): List<HistoryDay> {
    var left = n
    return days.mapNotNull { d ->
        d.rows.take(left).takeIf { it.isNotEmpty() }?.let {
            left -= it.size
            HistoryDay(d.date, it)
        }
    }
}

@Composable
private fun Headline(
    detail: CategoryDetail,
    state: CategoryReportState,
) {
    val colors = CofferTheme.colors
    val month = MONTH_LONG.format(state.month.from)
    AppCard(Modifier.fillMaxWidth(), highlight = true) {
        Text("Gastado en $month", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        // The main figure is never coloured (Figma: «los montos principales no van en color»)
        MoneyText(detail.spent, style = MaterialTheme.typography.displaySmall)
        val delta = detail.vsPrevious
        val before = detail.previous
        if (delta != null && before != null) {
            val up = delta > Money.ZERO
            val previousName = MONTH_LONG.format(state.month.from.minusMonths(1))
            Text(
                when {
                    delta == Money.ZERO -> "Igual que $previousName a la fecha"
                    else ->
                        (if (up) "↑ +" else "↓ −") +
                            (percentOf(delta, before)?.let { "${abs(it)} %" } ?: formatMoney(if (up) delta else -delta)) +
                            " vs. $previousName a la fecha"
                },
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (delta == Money.ZERO) {
                        colors.textSecondary
                    } else if (up) {
                        colors.expense
                    } else {
                        colors.income
                    },
            )
        }
        detail.average?.let {
            Text("Tu promedio mensual es ${formatMoney(it)}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        detail.weight?.let {
            Text("${it.percentLabel()} de todo tu gasto del mes", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}

/** One series with the value on top and the dashed average; the highest month in espresso, the running one dashed. */
@Composable
private fun Months(
    detail: CategoryDetail,
    state: CategoryReportState,
) {
    val colors = CofferTheme.colors
    val top =
        detail.months
            .maxOf { it.spent.scaled }
            .coerceAtLeast(1)
            .toFloat()
    val highest = detail.months.maxBy { it.spent.scaled }
    val running = !state.today.isAfter(state.month.to)
    val barArea = 120.dp
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Últimos ${detail.months.size} meses",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            detail.average?.let {
                Text(
                    "Promedio ${shortMoney(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        Box {
            Row(Modifier.fillMaxWidth()) {
                detail.months.forEachIndexed { i, m ->
                    val current = i == detail.months.lastIndex && running
                    Column(
                        Modifier.weight(1f).semantics { contentDescription = "${MONTH_LONG.format(m.month)}: ${formatMoney(m.spent)}" },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(
                            Modifier.fillMaxWidth().height(barArea + 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Text(shortMoney(m.spent), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            Box(
                                Modifier
                                    .padding(top = 4.dp)
                                    .width(28.dp)
                                    .height(barArea * (m.spent.scaled / top).coerceIn(0.02f, 1f))
                                    .then(
                                        if (current) {
                                            Modifier.dashedBorder(colors.textSecondary)
                                        } else {
                                            Modifier.background(
                                                if (m == highest) colors.primary else colors.borderHighlight,
                                                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                                            )
                                        },
                                    ),
                            )
                        }
                        Text(
                            MONTH.format(m.month).replace(".", "").replaceFirstChar { it.uppercase() } +
                                if (current) " · en curso" else "",
                            Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            // The average as a dashed line across the bars
            detail.average?.let { avg ->
                val line = colors.textSecondary
                Canvas(Modifier.fillMaxWidth().height(barArea + 20.dp)) {
                    val y = size.height - (avg.scaled / top).coerceIn(0f, 1f) * barArea.toPx()
                    drawLine(
                        line,
                        Offset(0f, y),
                        Offset(size.width, y),
                        1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun Subcategories(detail: CategoryDetail) {
    val colors = CofferTheme.colors
    val bar = colors.primary
    AppCard {
        Text("Subcategorías", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
        detail.subcategories.forEach { (child, total) ->
            val share = shareOf(total, detail.spent)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(child.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                    Text(
                        "${formatMoney(total)} · ${share.percentLabel()}",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                    )
                }
                Box(Modifier.fillMaxWidth().height(4.dp).background(colors.border, CofferShapes.pill)) {
                    Box(
                        Modifier
                            .fillMaxWidth(((share?.toFloat() ?: 0f) / 100f).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(bar, CofferShapes.pill),
                    )
                }
            }
        }
    }
}
