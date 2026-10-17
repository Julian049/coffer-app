package com.dev.julian09.financeapp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** How an amount reads: its color and sign follow design-system.md §2 (financial meaning beats brand). */
enum class MoneyKind { Neutral, Income, Expense, Transfer, Debt }

@Composable
fun MoneyText(
    money: Money,
    modifier: Modifier = Modifier,
    kind: MoneyKind = MoneyKind.Neutral,
    style: TextStyle = MaterialTheme.typography.titleSmall,
) {
    val colors = CofferTheme.colors
    val color: Color =
        when (kind) {
            MoneyKind.Neutral -> colors.textPrimary
            MoneyKind.Income -> colors.income
            MoneyKind.Expense, MoneyKind.Debt -> colors.expense
            MoneyKind.Transfer -> colors.transfer
        }
    Text(text = formatMoney(money, kind), modifier = modifier, color = color, style = style)
}

private const val MINUS = "−"
private val COP_SYMBOLS = DecimalFormatSymbols(Locale.forLanguageTag("es-CO"))

/**
 * Colombian pesos without decimals and with dots for thousands: `$1.250.000`.
 * Income gets `+`, expenses and debts `−` (U+2212), transfers no sign; neutral shows `−` only when negative.
 */
fun formatMoney(
    money: Money,
    kind: MoneyKind = MoneyKind.Neutral,
): String {
    val format = DecimalFormat("#,##0", COP_SYMBOLS).apply { roundingMode = RoundingMode.HALF_UP }
    val amount = "$" + format.format(money.toBigDecimal().abs())
    val sign =
        when (kind) {
            MoneyKind.Income -> "+"
            MoneyKind.Expense, MoneyKind.Debt -> MINUS
            MoneyKind.Transfer -> ""
            MoneyKind.Neutral -> if (money.isNegative) MINUS else ""
        }
    return sign + amount
}
