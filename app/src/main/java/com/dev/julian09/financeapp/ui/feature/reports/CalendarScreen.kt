package com.dev.julian09.financeapp.ui.feature.reports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.DayBalance
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppChip
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.feature.history.TagDots
import com.dev.julian09.financeapp.ui.feature.history.monthLabel
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val ES_CO: Locale = Locale.forLanguageTag("es-CO")
private val DAY_TITLE = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES_CO)
private val WEEK = listOf("L", "M", "M", "J", "V", "S", "D")

@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onOpenTransaction: (UUID) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CalendarContent(state, CalendarActions(onBack, viewModel::move, viewModel::select, viewModel::choose, onOpenTransaction))
}

data class CalendarActions(
    val onBack: () -> Unit = {},
    val onMove: (Long) -> Unit = {},
    val onSelect: (LocalDate) -> Unit = {},
    val onChoose: (Set<UUID>) -> Unit = {},
    val onOpenTransaction: (UUID) -> Unit = {},
)

/** «2,75 M», «869 mil»: the short balance of a cell, without the sign of pesos. */
private fun cellMoney(value: Money): String = shortMoney(value).replace("$", "")

/** Reportes · Calendario (Figma `171:186`, `171:354`): the balance at the end of each day of the chosen accounts. */
@Composable
fun CalendarContent(
    state: CalendarState?,
    actions: CalendarActions,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    var choosing by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Calendario", onBack = actions.onBack, subtitle = "Saldo al final de cada día")
        if (state == null) {
            // INC-05: the first month takes a moment; never a blank screen
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = colors.primary) }
            return@Column
        }
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Figma `171:186`: the month on the left, the accounts chip on the right
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { actions.onMove(-1) }, Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Mes anterior", tint = colors.textPrimary)
                }
                Text(monthLabel(state.month), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                IconButton(onClick = { actions.onMove(1) }, Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Mes siguiente", tint = colors.textSecondary)
                }
                Spacer(Modifier.weight(1f))
                val n = state.chosen.size
                AppChip(if (n == 1) "Saldo de 1 cuenta ▾" else "Saldo de $n cuentas ▾", selected = true, onClick = { choosing = true })
            }
            if (state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.primary, trackColor = colors.border)
            }
            Column(Modifier.alpha(if (state.loading) 0.4f else 1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Grid(state, actions.onSelect)
                DayCard(state, actions.onOpenTransaction)
            }
        }
    }
    if (choosing && state != null) {
        CalendarAccountsSheet(state.accounts, state.chosen, onDismiss = { choosing = false }) {
            actions.onChoose(it)
            choosing = false
        }
    }
}

/** Monday to Sunday; future days in grey with «—»; the chosen day in caramel, today with a thin border. */
@Composable
private fun Grid(
    state: CalendarState,
    onSelect: (LocalDate) -> Unit,
) {
    val colors = CofferTheme.colors
    val byDay = state.days.associateBy { it.date }
    val first = state.month.atDay(1)
    val lead = first.dayOfWeek.value - DayOfWeek.MONDAY.value
    val cells: List<LocalDate?> = List(lead) { null } + (1..state.month.lengthOfMonth()).map { state.month.atDay(it) }
    AppCard(Modifier.fillMaxWidth()) {
        Row {
            WEEK.forEach {
                Text(
                    it,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until 7).forEach { i ->
                    val day = week.getOrNull(i)
                    if (day == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        DayCell(day, byDay[day], day == state.selected, day == state.today, onSelect, Modifier.weight(1f))
                    }
                }
            }
        }
        Text(
            "Toca un día para ver el valor exacto y sus movimientos.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    balance: DayBalance?,
    selected: Boolean,
    today: Boolean,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier,
) {
    val colors = CofferTheme.colors
    val shape = RoundedCornerShape(8.dp)
    val future = balance == null
    Column(
        modifier
            .aspectRatio(0.8f)
            .clip(shape)
            .background(if (selected) colors.primaryContainer else Color.Transparent)
            .then(
                when {
                    selected -> Modifier.border(BorderStroke(1.5.dp, colors.onPrimaryContainer), shape)
                    today -> Modifier.border(BorderStroke(1.dp, colors.onPrimaryContainer), shape)
                    else -> Modifier
                },
            ).clickable(enabled = !future, onClickLabel = "Ver el día") { onSelect(day) }
            .semantics { contentDescription = "${day.dayOfMonth}: " + (balance?.let { formatMoney(it.closing) } ?: "sin saldo todavía") }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "${day.dayOfMonth}",
            style = MaterialTheme.typography.labelLarge,
            color = if (future) colors.textSecondary else colors.textPrimary,
        )
        Text(
            balance?.let { cellMoney(it.closing) } ?: "—",
            style = MaterialTheme.typography.labelSmall,
            color = if (balance == null || balance.closing.isNegative) colors.textSecondary else colors.textPrimary,
            maxLines = 1,
        )
    }
}

/** The tapped day: its exact balance, what it moved and its movements (with their tag dots). */
@Composable
private fun DayCard(
    state: CalendarState,
    onOpenTransaction: (UUID) -> Unit,
) {
    val colors = CofferTheme.colors
    val day = state.selected ?: return
    val balance = state.days.firstOrNull { it.date == day } ?: return
    // changes-ui-2.0 §3: changing the day is a 200 ms smart animate
    AnimatedContent(balance to state.selectedDay, transitionSpec = {
        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
    }, label = "day") { (b, rows) ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard(Modifier.fillMaxWidth(), highlight = true) {
                Text(
                    DAY_TITLE.format(b.date).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                Text("Saldo al final del día", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                MoneyText(b.closing, style = MaterialTheme.typography.displaySmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Movimiento del día", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    MoneyText(
                        if (b.change.isNegative) -b.change else b.change,
                        kind =
                            when {
                                b.change == Money.ZERO -> MoneyKind.Neutral
                                b.change.isNegative -> MoneyKind.Expense
                                else -> MoneyKind.Income
                            },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                rows?.rows?.forEach { row ->
                    HorizontalDivider(thickness = 1.dp, color = colors.border)
                    Row(
                        Modifier.fillMaxWidth().clickable(onClickLabel = "Ver detalle") { row.id?.let(onOpenTransaction) },
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

/** Hoja · Cuentas del calendario (Figma `171:514`): accounts and debts with a round mark; a debt subtracts. */
@Composable
private fun CalendarAccountsSheet(
    accounts: List<AccountBalance>,
    chosen: Set<UUID>,
    onDismiss: () -> Unit,
    onApply: (Set<UUID>) -> Unit,
) {
    val colors = CofferTheme.colors
    var picked by remember { mutableStateOf(chosen) }
    AppBottomSheet(
        onDismiss = onDismiss,
        title = "Cuentas del calendario",
        subtitle = "El saldo de cada día suma las cuentas marcadas; una deuda marcada se resta.",
        actions = {
            AppButton(
                "Aplicar",
                onClick = { onApply(picked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = picked.isNotEmpty(),
            )
        },
    ) {
        val (debts, assets) = accounts.partition { it.account.type.isDebt }
        listOf("Cuentas" to assets, "Deudas" to debts).filter { it.second.isNotEmpty() }.forEach { (title, list) ->
            Text(title, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            AppCard {
                list.forEachIndexed { i, a ->
                    if (i > 0) HorizontalDivider(thickness = 1.dp, color = colors.border)
                    val on = a.account.id in picked
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(on, role = Role.Checkbox) { picked = if (it) picked + a.account.id else picked - a.account.id }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (on) colors.primary else colors.surface)
                                .border(1.5.dp, if (on) colors.primary else colors.borderHighlight, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (on) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = colors.onPrimary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                        Text(a.account.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                        // A debt not marked shows its amount in grey
                        if (a.account.type.isDebt && !on) {
                            Text(formatMoney(a.balance), style = MaterialTheme.typography.titleSmall, color = colors.textSecondary)
                        } else {
                            MoneyText(a.balance)
                        }
                    }
                }
            }
        }
    }
}
