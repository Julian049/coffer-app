package com.dev.julian09.financeapp.ui.feature.reports

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.finance.CategorySum
import com.dev.julian09.financeapp.domain.finance.PeriodSums
import com.dev.julian09.financeapp.domain.finance.ReportSpan
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.EmptyState
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.SegmentedSelector
import com.dev.julian09.financeapp.ui.components.categoryIcon
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.feature.history.DateField
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val onSpan: (ReportSpan) -> Unit = {},
    val onMove: (Long) -> Unit = {},
    val onFrom: (LocalDate) -> Unit = {},
    val onTo: (LocalDate) -> Unit = {},
)

/** Reportes (S9-UI01, REQ-REP-01 to 05): period, summary, evolution, categories, interest and activity. */
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
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val spans = ReportSpan.entries
            SegmentedSelector(
                listOf("Semana", "Mes", "Rango"),
                selectedIndex = spans.indexOf(state.span),
                onSelect = { actions.onSpan(spans[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
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
                    Text(
                        periodTitle(state),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    IconButton(onClick = { actions.onMove(1) }) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Periodo siguiente", tint = colors.textPrimary)
                    }
                }
            }
            AppCard {
                Row(Modifier.fillMaxWidth()) {
                    Summary("Ingresos", report.income, MoneyKind.Income, Modifier.weight(1f))
                    Summary("Gastos", report.expense, MoneyKind.Expense, Modifier.weight(1f))
                    Summary("Balance", report.balance, if (report.balance.isNegative) MoneyKind.Expense else MoneyKind.Neutral, Modifier.weight(1f))
                }
            }
            if (report.count == 0 && report.evolution.all { it.expense == Money.ZERO && it.income == Money.ZERO }) {
                EmptyState(title = "Sin movimientos en este periodo", message = "Cuando registres movimientos verás en qué se va tu dinero.")
            } else {
                Evolution(state)
                if (report.expenseByCategory.isNotEmpty()) Categories("Gastos por categoría", report.expenseByCategory, report.expense)
                if (report.incomeByCategory.isNotEmpty()) Categories("Ingresos por categoría", report.incomeByCategory, report.income)
                if (report.interestByCard.isNotEmpty()) {
                    AppCard {
                        Text("Intereses y cargos financieros", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        MoneyText(report.interest, kind = MoneyKind.Expense, style = MaterialTheme.typography.headlineSmall)
                        report.interestByCard.entries.sortedByDescending { it.value.scaled }.forEach { (card, amount) ->
                            Line(state.accounts[card] ?: "Tarjeta", formatMoney(amount))
                        }
                    }
                }
                AppCard {
                    Text("Actividad", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    Line("Transacciones registradas", report.count.toString())
                    Line("Promedio por día", report.perDay.toPlainString().replace('.', ','))
                }
            }
            AppCard(onClick = actions.onOpenTags) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Sell, contentDescription = null, tint = colors.iconOnContainer, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Ver reportes por etiqueta", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text("Viajes, salidas y otros motivos", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary)
                }
            }
        }
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
private fun Evolution(state: ReportsState) {
    val colors = CofferTheme.colors
    val evolution = state.report.evolution
    val max = Money(evolution.maxOf { maxOf(it.expense.scaled, it.income.scaled) }.coerceAtLeast(1))
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Evolución", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Legend(colors.expense, "Gastos")
            Legend(colors.income, "Ingresos")
        }
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            evolution.forEach { sums ->
                val label = barLabel(state.span, sums)
                Row(
                    Modifier.fillMaxHeight().semantics {
                        contentDescription = "$label: gastos ${formatMoney(sums.expense)}, ingresos ${formatMoney(sums.income)}"
                    },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Bar(sums.expense, max, colors.expense)
                    Bar(sums.income, max, colors.income)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            evolution.forEach { Text(barLabel(state.span, it), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
        }
        state.report.expenseChange?.let { pct ->
            val previous = evolution[evolution.size - 2].period.from
            val against =
                when (state.span) {
                    ReportSpan.WEEK -> "la semana anterior"
                    else -> MONTH_LONG.format(previous)
                }
            val text =
                when {
                    pct > 0 -> "Gastaste $pct % más que en $against."
                    pct < 0 -> "Gastaste ${-pct} % menos que en $against."
                    else -> "Gastaste lo mismo que en $against."
                }
            Text(text, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
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

@Composable
private fun Bar(
    value: Money,
    max: Money,
    color: Color,
) {
    val fraction = (value.scaled.toFloat() / max.scaled.toFloat()).coerceIn(0f, 1f)
    Box(Modifier.width(10.dp).fillMaxHeight(fraction.coerceAtLeast(0.02f)).background(color, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
}

private fun percent(
    part: Money,
    whole: Money,
): Int =
    if (whole == Money.ZERO) {
        0
    } else {
        part.toBigDecimal().multiply(BigDecimal(100)).divide(whole.toBigDecimal(), 0, RoundingMode.HALF_UP).toInt()
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
                    .clickable(enabled = sum.children.isNotEmpty(), onClickLabel = "Ver subcategorías") { open = if (open == id) null else id },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(28.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(categoryIcon(sum.category.icon), contentDescription = null, tint = colors.iconOnContainer, modifier = Modifier.size(16.dp))
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
