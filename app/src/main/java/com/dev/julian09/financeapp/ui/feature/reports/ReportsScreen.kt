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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.finance.CategorySum
import com.dev.julian09.financeapp.domain.finance.Pace
import com.dev.julian09.financeapp.domain.finance.PeriodSums
import com.dev.julian09.financeapp.domain.finance.ReportSpan
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppChip
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.EmptyState
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.categoryIcon
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.feature.history.DateField
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val ES_CO: Locale = Locale.forLanguageTag("es-CO")
private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", ES_CO)
private val MONTH = DateTimeFormatter.ofPattern("MMM", ES_CO)
private val MONTH_LONG = DateTimeFormatter.ofPattern("MMMM", ES_CO)
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", ES_CO)
private val DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", ES_CO)

@Composable
fun ReportsScreen(
    onOpenSettings: () -> Unit,
    onOpenTags: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTag: (UUID) -> Unit = {},
    avatarInitial: Char? = null,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportsContent(
        state = state,
        actions =
            ReportsActions(
                onOpenSettings = onOpenSettings,
                onOpenTags = onOpenTags,
                onOpenTag = onOpenTag,
                onSpan = viewModel::selectSpan,
                onMove = viewModel::move,
                onFrom = { viewModel.setRange(from = it) },
                onTo = { viewModel.setRange(to = it) },
            ),
        modifier = modifier,
        avatarInitial = avatarInitial,
    )
}

data class ReportsActions(
    val onOpenSettings: () -> Unit = {},
    val onOpenTags: () -> Unit = {},
    val onOpenTag: (UUID) -> Unit = {},
    val onSpan: (ReportSpan) -> Unit = {},
    val onMove: (Long) -> Unit = {},
    val onFrom: (LocalDate) -> Unit = {},
    val onTo: (LocalDate) -> Unit = {},
)

/**
 * Reportes (Figma `20:497`, S13-H01): fixed filters, then one card per question — how much you saved, the pace of spending,
 * what changed, the saving by month, tags and interest — and the activity as a closing line.
 */
@Composable
fun ReportsContent(
    state: ReportsState?,
    actions: ReportsActions,
    modifier: Modifier = Modifier,
    avatarInitial: Char? = null,
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Reportes", actions = { Avatar(onClick = actions.onOpenSettings, initial = avatarInitial) })
        if (state == null) return@Column
        val report = state.report
        // The filters stay in the header while the cards scroll (Figma: «no se va al desplazar»)
        Filters(state, actions)
        HorizontalDivider(thickness = 1.dp, color = colors.border)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (report.count == 0 && report.evolution.all { it.expense == Money.ZERO && it.income == Money.ZERO }) {
                EmptyState(
                    title = "Sin movimientos en este periodo",
                    message = "Cuando registres movimientos verás en qué se va tu dinero.",
                )
                return@Column
            }
            SavingCard(state)
            report.pace?.takeIf { it.previous.lastOrNull()?.let { last -> last > Money.ZERO } == true }?.let { PaceCard(state, it) }
            if (report.changes.isNotEmpty()) {
                ChangesCard(state)
            } else if (report.expenseByCategory.isNotEmpty()) {
                Categories("Gastos por categoría", report.expenseByCategory, report.expense)
            }
            SavingsByPeriod(state)
            if (state.tags.isNotEmpty()) TagsCard(state, actions)
            if (report.interestByCard.isNotEmpty()) {
                AppCard {
                    CardHeader(
                        "Intereses y cargos",
                    ) { MoneyText(report.interest, kind = MoneyKind.Expense, style = MaterialTheme.typography.titleSmall) }
                    report.interestByCard.entries.sortedByDescending { it.value.scaled }.forEach { (card, amount) ->
                        Line(state.accounts[card] ?: "Tarjeta", formatMoney(amount))
                    }
                }
            }
            Text(
                "${report.count} movimientos registrados en ${periodName(
                    state,
                )}, ${report.perDay.toPlainString().replace('.', ',')} por día.",
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Filters(
    state: ReportsState,
    actions: ReportsActions,
) {
    val colors = CofferTheme.colors
    var spanMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.span == ReportSpan.RANGE) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Desde", state.period.from, Modifier.weight(1f), actions.onFrom)
                DateField("Hasta", state.period.to, Modifier.weight(1f), actions.onTo)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { actions.onMove(-1) }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Periodo anterior", tint = colors.textPrimary)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(periodTitle(state), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text(periodSpan(state), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                IconButton(onClick = { actions.onMove(1) }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Periodo siguiente",
                        tint = colors.textPrimary,
                    )
                }
            }
        }
        Box {
            AppChip(
                when (state.span) {
                    ReportSpan.WEEK -> "Semana ▾"
                    ReportSpan.MONTH -> "Mes ▾"
                    ReportSpan.RANGE -> "Rango ▾"
                },
                selected = true,
                onClick = { spanMenu = true },
            )
            DropdownMenu(expanded = spanMenu, onDismissRequest = { spanMenu = false }) {
                listOf(ReportSpan.WEEK to "Semana", ReportSpan.MONTH to "Mes", ReportSpan.RANGE to "Rango").forEach { (span, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        actions.onSpan(span)
                        spanMenu = false
                    })
                }
            }
        }
    }
}

/** A card's short header: the title on the left and its reference in grey on the right. */
@Composable
private fun CardHeader(
    title: String,
    trailing: @Composable () -> Unit = {},
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = CofferTheme.colors.textPrimary)
        trailing()
    }
}

@Composable
private fun Reference(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = CofferTheme.colors.textSecondary)

@Composable
private fun SavingCard(state: ReportsState) {
    val colors = CofferTheme.colors
    val report = state.report
    AppCard(highlight = true) {
        Text(
            when (state.span) {
                ReportSpan.WEEK -> "Ahorro de la semana"
                ReportSpan.MONTH -> "Ahorro del mes"
                ReportSpan.RANGE -> "Ahorro del periodo"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        // The main figure is never coloured (Figma: «los montos principales no van en color»)
        MoneyText(report.balance, style = MaterialTheme.typography.displaySmall)
        report.savingsRate?.let { rate ->
            val line =
                buildAnnotatedString {
                    withStyle(SpanStyle(color = if (rate >= 0) colors.income else colors.expense, fontWeight = FontWeight.Medium)) {
                        append("$rate %")
                    }
                    append(" de tus ingresos")
                    report.averageRate?.let { append(" · tu promedio es $it %") }
                }
            Text(line, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        HorizontalDivider(thickness = 1.dp, color = colors.border)
        Row(Modifier.fillMaxWidth()) {
            Summary("Ingresos", report.income, MoneyKind.Income, Modifier.weight(1f))
            Summary("Gastos", report.expense, MoneyKind.Expense, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PaceCard(
    state: ReportsState,
    pace: Pace,
) {
    val colors = CofferTheme.colors
    val previousName =
        MONTH_LONG.format(
            state.report.evolution[state.report.evolution.size - 2]
                .period.from,
        )
    AppCard {
        CardHeader("Ritmo de gasto") { Reference("vs. $previousName") }
        val ahead = pace.daysAhead
        Text(
            when {
                ahead == null -> "Más que todo $previousName"
                ahead > 0 -> if (ahead == 1) "1 día adelantado" else "$ahead días adelantado"
                ahead < 0 -> if (ahead == -1) "1 día atrás" else "${-ahead} días atrás"
                else -> "Al mismo ritmo"
            },
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
        )
        Text(
            pace.matchDay?.let { "Hoy llevas lo que en $previousName gastaste al día $it" }
                ?: "Ya gastaste más que en todo $previousName",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        PaceChart(pace)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Legend(colors.primary, MONTH_LONG.format(state.period.from).replaceFirstChar { it.uppercase() })
            Legend(colors.textSecondary, previousName.replaceFirstChar { it.uppercase() })
        }
    }
}

/** Cumulative spending: this month a solid walnut line up to today, the month before a dashed grey one. */
@Composable
private fun PaceChart(pace: Pace) {
    val colors = CofferTheme.colors
    val days = maxOf(pace.previous.size, pace.current.size, 2)
    val top = maxOf(pace.previous.maxOfOrNull { it.scaled } ?: 0, pace.spent.scaled).coerceAtLeast(1).toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                "Hoy ${formatMoney(pace.spent)}",
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textPrimary,
            )
            pace.previous.lastOrNull()?.let {
                Text("${formatMoney(it)} todo el mes", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
        }
        Canvas(
            Modifier.fillMaxWidth().height(120.dp).semantics {
                contentDescription = "Gasto acumulado: hoy ${formatMoney(pace.spent)}"
            },
        ) {
            fun x(day: Int) = (day - 1) / (days - 1).toFloat() * size.width

            fun y(m: Money) = size.height - m.scaled / top * size.height

            fun line(
                points: List<Money>,
                color: Color,
                width: Float,
                dashed: Boolean,
            ) {
                val path = Path()
                points.forEachIndexed { i, m -> if (i == 0) path.moveTo(x(1), y(m)) else path.lineTo(x(i + 1), y(m)) }
                drawPath(
                    path,
                    color,
                    style =
                        Stroke(
                            width = width,
                            cap = StrokeCap.Round,
                            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null,
                        ),
                )
            }
            line(pace.previous, colors.textSecondary.copy(alpha = 0.7f), 2.dp.toPx(), dashed = true)
            line(pace.current, colors.primary, 2.5.dp.toPx(), dashed = false)
            // Today
            val tx = x(pace.today)
            drawLine(colors.border, Offset(tx, 0f), Offset(tx, size.height), 1.dp.toPx())
            drawCircle(colors.primary, 4.dp.toPx(), Offset(tx, y(pace.spent)))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(1, 11, 21, days).forEach { Text("$it", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary) }
        }
    }
}

@Composable
private fun ChangesCard(state: ReportsState) {
    val colors = CofferTheme.colors
    val report = state.report
    val previous = report.evolution[report.evolution.size - 2].period.from
    val shown = report.changes.take(6)
    val biggest = shown.maxOf { kotlin.math.abs(it.delta.scaled) }.coerceAtLeast(1)
    AppCard {
        CardHeader("Cambios por categoría") { Reference("vs. ${MONTH.format(previous).replace(".", "")} a la fecha") }
        report.changePercent?.let { pct ->
            val up = pct >= 0
            val leaders =
                shown
                    .filter { (it.delta > Money.ZERO) == up && it.delta != Money.ZERO }
                    .take(
                        2,
                    ).map { it.category.name.lowercase() }
            if (leaders.isNotEmpty()) {
                Text(
                    leaders.joinToString(" y ").replaceFirstChar { it.uppercase() } +
                        (if (leaders.size == 1) " explica " else " explican ") +
                        (if (up) "la subida del $pct %" else "la bajada del ${-pct} %"),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        shown.forEachIndexed { i, change ->
            if (i > 0) HorizontalDivider(thickness = 1.dp, color = colors.border)
            val up = change.delta > Money.ZERO
            val tint = if (up) colors.expense else colors.income
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(36.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(
                        categoryIcon(change.category.icon),
                        contentDescription = null,
                        tint = colors.iconOnContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(change.category.name, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                    Text(
                        (if (up) "↑ +" else "↓ −") +
                            (change.percent?.let { "${kotlin.math.abs(it)} % · " } ?: "") +
                            formatMoney(if (up) change.delta else -change.delta),
                        style = MaterialTheme.typography.bodySmall,
                        color = tint,
                    )
                }
                // A diverging bar from the centre: right and red when spending went up, left and green when it went down
                Row(Modifier.width(56.dp).height(6.dp)) {
                    val fraction = kotlin.math.abs(change.delta.scaled).toFloat() / biggest
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
                        if (!up) Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(tint, RoundedCornerShape(3.dp)))
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(colors.border))
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                        if (up) Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(tint, RoundedCornerShape(3.dp)))
                    }
                }
                Text(formatMoney(change.now), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            }
        }
    }
}

/** Ahorro por mes: one bar per period with its value above, the best one in walnut, the running one dashed. */
@Composable
private fun SavingsByPeriod(state: ReportsState) {
    val colors = CofferTheme.colors
    val report = state.report
    val savings = report.evolution.map { it.income - it.expense }
    val top = savings.maxOf { it.scaled }.coerceAtLeast(1).toFloat()
    val closed = savings.dropLast(if (report.closed) 0 else 1)
    val best = closed.maxByOrNull { it.scaled }
    AppCard {
        CardHeader(
            when (state.span) {
                ReportSpan.WEEK -> "Ahorro por semana"
                else -> "Ahorro por mes"
            },
        ) { report.averageSavings?.let { Reference("Promedio ${formatMoney(it)}") } }
        Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            report.evolution.forEachIndexed { i, sums ->
                val value = savings[i]
                val running = i == savings.lastIndex && !report.closed
                val fraction = (value.scaled.coerceAtLeast(0) / top).coerceIn(0.02f, 1f)
                Column(
                    Modifier.fillMaxHeight().semantics { contentDescription = "${barLabel(state.span, sums)}: ${formatMoney(value)}" },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(shortMoney(value), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .width(32.dp)
                            .fillMaxHeight(fraction * 0.8f)
                            .then(
                                if (running) {
                                    Modifier.dashedBorder(colors.textSecondary)
                                } else {
                                    Modifier.background(
                                        if (value == best) colors.primary else colors.borderHighlight,
                                        RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                                    )
                                },
                            ),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            report.evolution.forEachIndexed { i, sums ->
                val running = i == report.evolution.lastIndex && !report.closed
                Text(
                    barLabel(state.span, sums) + if (running) " · en curso" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

private fun Modifier.dashedBorder(color: Color) =
    drawBehind {
        drawRoundRect(
            color,
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
            cornerRadius = CornerRadius(6.dp.toPx()),
        )
    }

/** «$1,32 M», «$850 mil»: bar labels that fit. */
private fun shortMoney(value: Money): String {
    val pesos = value.toBigDecimal()
    val abs = pesos.abs()
    val sign = if (pesos.signum() < 0) "−" else ""
    return when {
        abs >= BigDecimal(1_000_000) ->
            "$sign$" +
                abs
                    .divide(BigDecimal(1_000_000), 2, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString()
                    .replace('.', ',') +
                " M"
        abs >= BigDecimal(1_000) -> "$sign$" + abs.divide(BigDecimal(1_000), 0, RoundingMode.HALF_UP).toPlainString() + " mil"
        else -> formatMoney(value)
    }
}

@Composable
private fun TagsCard(
    state: ReportsState,
    actions: ReportsActions,
) {
    val colors = CofferTheme.colors
    AppCard {
        CardHeader("Por etiqueta") {
            Text(
                "Ver todas",
                Modifier.clip(CofferShapes.pill).clickable(onClick = actions.onOpenTags).padding(4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
            )
        }
        state.tags.take(5).forEachIndexed { i, line ->
            if (i > 0) HorizontalDivider(thickness = 1.dp, color = colors.border)
            Row(
                Modifier.fillMaxWidth().clickable { actions.onOpenTag(line.tag.id) }.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(36.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Sell, contentDescription = null, tint = colors.iconOnContainer, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(line.tag.name, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                    Text(
                        if (line.count == 1) "1 movimiento" else "${line.count} movimientos",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                Text(
                    formatMoney(if (line.net.isNegative) -line.net else line.net),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary)
            }
        }
    }
}

private fun periodName(state: ReportsState): String =
    when (state.span) {
        ReportSpan.MONTH -> MONTH_LONG.format(state.period.from)
        ReportSpan.WEEK -> "la semana"
        ReportSpan.RANGE -> "el periodo"
    }

/** «Del 1 al 21 · hoy» while the period runs, else its days. */
private fun periodSpan(state: ReportsState): String {
    val p = state.period
    val today = LocalDate.now()
    return if (today in p.from..p.to) {
        "Del ${p.from.dayOfMonth} al ${today.dayOfMonth} · hoy"
    } else {
        "Del ${p.from.dayOfMonth} al ${p.to.dayOfMonth}"
    }
}

private fun periodTitle(state: ReportsState): String {
    val p = state.period
    return when (state.span) {
        ReportSpan.MONTH -> MONTH_YEAR.format(p.from).replaceFirstChar { it.uppercase() }
        else -> "${DAY_MONTH.format(p.from)} – ${DAY_MONTH_YEAR.format(p.to)}".replace(".", "")
    }
}

private fun barLabel(
    span: ReportSpan,
    sums: PeriodSums,
): String =
    when (span) {
        ReportSpan.WEEK -> DAY_MONTH.format(sums.period.from)
        else -> MONTH.format(sums.period.from)
    }.replace(".", "").replaceFirstChar { it.uppercase() }

@Composable
private fun Summary(
    label: String,
    money: Money,
    kind: MoneyKind,
    modifier: Modifier,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = CofferTheme.colors.textSecondary)
        MoneyText(money, kind = kind, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun Line(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = CofferTheme.colors.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = CofferTheme.colors.textPrimary)
    }
}

@Composable
private fun Legend(
    color: Color,
    label: String,
) {
    Row(Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(" $label", style = MaterialTheme.typography.bodySmall, color = CofferTheme.colors.textSecondary)
    }
}

private fun percent(
    part: Money,
    whole: Money,
): Int =
    if (whole == Money.ZERO) {
        0
    } else {
        part
            .toBigDecimal()
            .multiply(BigDecimal(100))
            .divide(whole.toBigDecimal(), 0, RoundingMode.HALF_UP)
            .toInt()
    }

@Composable
private fun Categories(
    title: String,
    sums: List<CategorySum>,
    whole: Money,
) {
    val colors = CofferTheme.colors
    var open by rememberSaveable(title) { mutableStateOf<String?>(null) }
    AppCard {
        Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
        sums.forEach { sum ->
            val id = sum.category.id.toString()
            val pct = percent(sum.total, whole)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = sum.children.isNotEmpty(), onClickLabel = "Ver subcategorías") {
                        open =
                            if (open == id) null else id
                    },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(28.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(
                            categoryIcon(sum.category.icon),
                            contentDescription = null,
                            tint = colors.iconOnContainer,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(sum.category.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                    Text("$pct %", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    Text(formatMoney(sum.total), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                }
                Box(Modifier.fillMaxWidth().height(6.dp).background(colors.border, RoundedCornerShape(3.dp))) {
                    Box(Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().background(colors.primary, RoundedCornerShape(3.dp)))
                }
                if (open == id) {
                    sum.children.forEach { (child, total) ->
                        Row(Modifier.padding(start = 36.dp)) {
                            Text(child.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                            Text(formatMoney(total), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}
