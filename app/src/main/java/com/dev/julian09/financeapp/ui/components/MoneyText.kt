package com.dev.julian09.financeapp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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

/**
 * RNF-13: decimals shown in every amount, 0 or 2. It is snapshot state, so any composable that formats an amount
 * redraws when Ajustes changes it; the root keeps it in step with user_settings.amount_decimals.
 */
object AmountFormat {
    var decimals by mutableIntStateOf(0)
}

private const val MINUS = "−"
private val COP_SYMBOLS = DecimalFormatSymbols(Locale.forLanguageTag("es-CO"))

/**
 * Colombian pesos with dots for thousands: `$1.250.000`, or `$1.250.000,50` with 2 [decimals].
 * Income gets `+`, expenses and debts `−` (U+2212), transfers no sign; neutral shows `−` only when negative.
 */
fun formatMoney(
    money: Money,
    kind: MoneyKind = MoneyKind.Neutral,
    decimals: Int = AmountFormat.decimals,
): String {
    val pattern = if (decimals == 2) "#,##0.00" else "#,##0"
    val format = DecimalFormat(pattern, COP_SYMBOLS).apply { roundingMode = RoundingMode.HALF_UP }
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

/**
 * Shows the whole-peso digits of an amount field as `$1.250.000` (or `1.250.000` without [symbol]) while the field
 * keeps only the digits, so the cursor stays where the user types when a dot or the `$` appears (E1, #75).
 */
class PesosTransformation(
    private val symbol: Boolean = true,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val pesos = text.text.toLongOrNull() ?: return TransformedText(text, OffsetMapping.Identity)
        // Fields take whole pesos, whatever Ajustes says about decimals
        val formatted = formatMoney(Money.ofPesos(pesos), decimals = 0)
        val shown = if (symbol) formatted else formatted.removePrefix("$")
        val mapping =
            object : OffsetMapping {
                // Just before the digit after [offset] digits: past the dot that precedes it
                override fun originalToTransformed(offset: Int): Int {
                    var seen = 0
                    shown.forEachIndexed { i, c -> if (c.isDigit() && seen++ == offset) return i }
                    return shown.length
                }

                override fun transformedToOriginal(offset: Int): Int = shown.take(offset).count(Char::isDigit)
            }
        return TransformedText(AnnotatedString(shown), mapping)
    }

    override fun equals(other: Any?) = other is PesosTransformation && other.symbol == symbol

    override fun hashCode() = symbol.hashCode()
}
