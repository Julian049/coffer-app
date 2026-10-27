package com.dev.julian09.financeapp.ui.feature.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.TransferIcon
import com.dev.julian09.financeapp.ui.components.categoryIcon
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

data class HistoryRow(
    val title: String,
    val subtitle: String,
    val amount: Money,
    val type: TransactionType,
    val icon: String? = null,
    /** Opens the detail (S5-H03); null in previews. */
    val id: UUID? = null,
    /** Shown in Historial, not in Home. */
    val time: LocalTime? = null,
    val categoryId: UUID? = null,
    /** One 8 dp dot per tag, in its colour, after the title (changes-ui-2.0 §2). */
    val tagColors: List<String> = emptyList(),
)

data class HistoryDay(
    val date: LocalDate,
    val rows: List<HistoryRow>,
) {
    /** Income minus expenses of the day; transfers do not change what you have. */
    val net: Money
        get() =
            rows.fold(Money.ZERO) { sum, row ->
                when (row.type) {
                    TransactionType.INCOME -> sum + row.amount
                    TransactionType.EXPENSE -> sum - row.amount
                    TransactionType.TRANSFER -> sum
                }
            }
}

/** Newest first, grouped by local day; names come from every not deleted account and category. */
fun buildDays(
    transactions: List<Transaction>,
    accountNames: Map<UUID, String>,
    categories: List<Category>,
    zone: ZoneId = ZoneId.systemDefault(),
    tags: List<Tag> = emptyList(),
): List<HistoryDay> {
    val byId = categories.associateBy { it.id }
    val tagById = tags.associateBy { it.id }
    return transactions
        .groupBy { it.occurredAt.atZone(zone).toLocalDate() }
        .map { (date, dayTransactions) ->
            HistoryDay(
                date,
                dayTransactions.map { tx ->
                    val from = accountNames[tx.accountId] ?: "Cuenta"
                    val category = tx.categoryId?.let(byId::get)
                    val parent = category?.parentId?.let(byId::get)
                    val where = tx.destinationAccountId?.let { "$from → ${accountNames[it] ?: "Cuenta"}" } ?: from
                    val categoryLabel = category?.name
                    HistoryRow(
                        title = tx.note ?: category?.name ?: typeLabel(tx.type),
                        subtitle = listOfNotNull(categoryLabel?.takeIf { tx.note != null }, where).joinToString(" · "),
                        amount = tx.amount,
                        type = tx.type,
                        icon = category?.icon ?: parent?.icon,
                        id = tx.id,
                        time = tx.occurredAt.atZone(zone).toLocalTime(),
                        categoryId = tx.categoryId,
                        tagColors = tx.tagIds.mapNotNull { tagById[it]?.shownColor },
                    )
                },
            )
        }
}

internal fun typeLabel(type: TransactionType) =
    when (type) {
        TransactionType.EXPENSE -> "Gasto"
        TransactionType.INCOME -> "Ingreso"
        TransactionType.TRANSFER -> "Transferencia"
    }

private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE d 'de' MMM", Locale.forLanguageTag("es-CO"))
private val DAY_YEAR_FORMAT = DateTimeFormatter.ofPattern("EEEE d 'de' MMM 'de' yyyy", Locale.forLanguageTag("es-CO"))

/** REQ-TRX-11: "Hoy", "Ayer", "Antier" and then the date. */
fun dayLabel(
    date: LocalDate,
    today: LocalDate,
): String =
    when (date) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        today.minusDays(2) -> "Antier"
        else ->
            (if (date.year == today.year) DAY_FORMAT else DAY_YEAR_FORMAT)
                .format(date)
                .replace(".", "")
                .replaceFirstChar { it.uppercase() }
    }

/** One day: header with the net of the day, then its movements in a card. */
@Composable
fun DayGroup(
    day: HistoryDay,
    today: LocalDate,
    modifier: Modifier = Modifier,
    showTime: Boolean = false,
    onOpen: ((UUID) -> Unit)? = null,
) {
    val colors = CofferTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text(dayLabel(day.date, today), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            if (day.net != Money.ZERO) {
                Text(
                    (if (day.net.isNegative) "" else "+") + formatMoney(day.net),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                )
            }
        }
        // AppCard without its padding: the press highlight fills the row up to the card's edges (#129)
        Surface(shape = CofferShapes.card, color = colors.surface, border = BorderStroke(1.dp, colors.border)) {
            Column {
                day.rows.forEachIndexed { i, row ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = colors.border)
                    MovementRow(
                        row,
                        showTime,
                        onOpen,
                        PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = if (i == 0) 16.dp else 12.dp,
                            bottom = if (i == day.rows.lastIndex) 16.dp else 12.dp,
                        ),
                    )
                }
            }
        }
    }
}

private val TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-CO"))

@Composable
private fun MovementRow(
    row: HistoryRow,
    showTime: Boolean,
    onOpen: ((UUID) -> Unit)?,
    padding: PaddingValues,
) {
    val colors = CofferTheme.colors
    val click = if (onOpen != null && row.id != null) Modifier.clickable(onClickLabel = "Ver detalle") { onOpen(row.id) } else Modifier
    Row(
        Modifier.fillMaxWidth().then(click).padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(
                if (row.type == TransactionType.TRANSFER) TransferIcon else categoryIcon(row.icon),
                contentDescription = null,
                tint = colors.iconOnContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(row.title, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                TagDots(row.tagColors)
            }
            Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(
                row.amount,
                kind =
                    when (row.type) {
                        TransactionType.EXPENSE -> MoneyKind.Expense
                        TransactionType.INCOME -> MoneyKind.Income
                        TransactionType.TRANSFER -> MoneyKind.Transfer
                    },
            )
            if (showTime && row.time != null) {
                Text(TIME.format(row.time), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }
}

/** The tags of a movement as 8 dp dots in their colours, 4 dp apart; nothing at all without tags (changes-ui-2.0 §2). */
@Composable
fun TagDots(
    colors: List<String>,
    modifier: Modifier = Modifier,
) {
    if (colors.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        colors.forEach { TagDot(it) }
    }
}

@Composable
fun TagDot(
    hex: String,
    modifier: Modifier = Modifier,
) = Box(modifier.size(8.dp).background(tagColor(hex), CircleShape))

/** `#RRGGBB` of TagColors. */
fun tagColor(hex: String): Color = Color(android.graphics.Color.parseColor(hex))
