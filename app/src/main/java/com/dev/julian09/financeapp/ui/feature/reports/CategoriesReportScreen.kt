package com.dev.julian09.financeapp.ui.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.finance.ReportPeriod
import com.dev.julian09.financeapp.domain.finance.shareOf
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.EmptyState
import com.dev.julian09.financeapp.ui.components.SegmentedSelector
import com.dev.julian09.financeapp.ui.components.categoryIcon
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val ES_CO: Locale = Locale.forLanguageTag("es-CO")
private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", ES_CO)
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", ES_CO)
private val DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", ES_CO)

/** «Octubre 2026» for a whole month, else «1 may – 27 oct 2026». */
internal fun periodLabel(period: ReportPeriod): String =
    if (period.from.dayOfMonth == 1 && period.from.month == period.to.month && period.from.year == period.to.year) {
        MONTH_YEAR.format(period.from).replaceFirstChar { it.uppercase() }
    } else {
        "${DAY_MONTH.format(period.from)} – ${DAY_MONTH_YEAR.format(period.to)}".replace(".", "")
    }

@Composable
fun CategoriesReportScreen(
    onBack: () -> Unit,
    onOpenCategory: (UUID, LocalDate) -> Unit,
    viewModel: CategoriesReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CategoriesReportContent(state, onBack, viewModel::showIncome, onOpenCategory = { id ->
        state?.let { onOpenCategory(id, it.period.to) }
    })
}

/** Reportes · Gastos por categoría (Figma `168:187`): the pie, and every category with its exact value. */
@Composable
fun CategoriesReportContent(
    state: CategoriesReportState?,
    onBack: () -> Unit,
    onIncome: (Boolean) -> Unit,
    onOpenCategory: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(
            title = if (state?.income == true) "Ingresos por categoría" else "Gastos por categoría",
            onBack = onBack,
            subtitle = state?.let { periodLabel(it.period) },
        )
        if (state == null) return@Column
        val report = state.summary.shown
        val sums = if (state.income) report.incomeByCategory else report.expenseByCategory
        val total = if (state.income) report.income else report.expense
        val count = state.summary.counts[if (state.income) TransactionType.INCOME else TransactionType.EXPENSE] ?: 0
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SegmentedSelector(listOf("Gastos", "Ingresos"), if (state.income) 1 else 0, { onIncome(it == 1) }, Modifier.fillMaxWidth())
            if (sums.isEmpty()) {
                EmptyState(
                    title = "Sin movimientos en este periodo",
                    message = "Cuando registres movimientos verás en qué se va tu dinero.",
                )
                return@Column
            }
            AppCard(Modifier.fillMaxWidth(), highlight = true) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    val label = if (state.income) "Total recibido" else "Total gastado"
                    DonutChart(pieSlices(sums), 200.dp, 36.dp, "$label ${formatMoney(total)}") {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            Text(formatMoney(total), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        }
                    }
                    Text(
                        "${if (sums.size == 1) "1 categoría" else "${sums.size} categorías"} · " +
                            if (count == 1) "1 movimiento" else "$count movimientos",
                        Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            AppCard {
                sums.forEachIndexed { i, sum ->
                    if (i > 0) HorizontalDivider(thickness = 1.dp, color = colors.border)
                    // The detail exists for spending only (changes-ui-2.0 §3: Ingresos is still pending of design)
                    val open =
                        if (state.income) {
                            Modifier
                        } else {
                            Modifier.clickable(
                                onClickLabel = "Ver detalle",
                            ) { onOpenCategory(sum.category.id) }
                        }
                    Row(
                        Modifier.fillMaxWidth().then(open).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ColorSquare(sum.color())
                        Box(Modifier.size(36.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(
                                categoryIcon(sum.category.icon),
                                contentDescription = null,
                                tint = colors.iconOnContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(sum.category.name, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                            Text(
                                "${shareOf(sum.total, total).percentLabel()} del ${if (state.income) "ingreso" else "gasto"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                        Text(formatMoney(sum.total), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        if (!state.income) {
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}
