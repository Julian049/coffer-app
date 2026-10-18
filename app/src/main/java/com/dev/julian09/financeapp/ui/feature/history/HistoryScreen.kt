package com.dev.julian09.financeapp.ui.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.EmptyState
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    avatarInitial: Char? = null,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val days by viewModel.days.collectAsStateWithLifecycle()
    HistoryContent(days, onOpenSettings, modifier, avatarInitial)
}

@Composable
fun HistoryContent(
    days: List<HistoryDay>?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    avatarInitial: Char? = null,
    today: LocalDate = LocalDate.now(),
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Historial", actions = { Avatar(onClick = onOpenSettings, initial = avatarInitial) })
        when {
            days == null -> Unit
            days.isEmpty() ->
                EmptyState(
                    title = "Aún no hay movimientos",
                    message = "Tus gastos, ingresos y transferencias aparecerán aquí.",
                )
            else ->
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(days, key = { it.date.toString() }) { day ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(dayLabel(day.date, today), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                            AppCard {
                                day.rows.forEachIndexed { i, row ->
                                    if (i > 0) HorizontalDivider(thickness = 1.dp, color = colors.border)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text(row.title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                                            Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                        }
                                        MoneyText(
                                            row.amount,
                                            kind =
                                                when (row.type) {
                                                    TransactionType.EXPENSE -> MoneyKind.Expense
                                                    TransactionType.INCOME -> MoneyKind.Income
                                                    TransactionType.TRANSFER -> MoneyKind.Transfer
                                                },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }
}

private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"))

private fun dayLabel(
    date: LocalDate,
    today: LocalDate,
) = when (date) {
    today -> "Hoy"
    today.minusDays(1) -> "Ayer"
    else -> DAY_FORMAT.format(date).replaceFirstChar { it.uppercase() }
}
