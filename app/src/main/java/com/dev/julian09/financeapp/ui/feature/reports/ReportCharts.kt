package com.dev.julian09.financeapp.ui.feature.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.domain.finance.CategorySum
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.math.BigDecimal
import java.math.RoundingMode

/** «Otras» in the pie (changes-ui-2.0 §3): every category without a fixed colour; never an invented one. */
val OTHER_CATEGORY_COLOR = Color(0xFFA8A29E)

private val BASE_COLORS =
    mapOf(
        "Hogar" to Color(0xFF1E9C7E),
        "Comida" to Color(0xFFC2662D),
        "Transporte" to Color(0xFF2F74C0),
        "Entretenimiento" to Color(0xFFD2638F),
        "Salud" to Color(0xFFD9A21B),
    )
private val FINANCIAL_CHARGES_COLOR = Color(0xFF7B5FC0)

/**
 * The colour follows the category, not its place (changes-ui-2.0 §3): the six base ones have theirs, a renamed or user
 * category has none (PO and Claude, 2026-10-26). [CategorySum] already carries the main category.
 */
fun categoryColor(category: Category): Color? =
    when {
        !category.isBase || category.parentId != null -> null
        category.systemKey == "FINANCIAL_CHARGES" -> FINANCIAL_CHARGES_COLOR
        else -> BASE_COLORS[category.name]
    }

fun CategorySum.color(): Color = categoryColor(category) ?: OTHER_CATEGORY_COLOR

/** One slice per coloured category and a single grey «Otras» with the rest, in the list's order. */
fun pieSlices(sums: List<CategorySum>): List<Pair<Color, Money>> {
    val coloured = sums.mapNotNull { s -> categoryColor(s.category)?.let { it to s.total } }
    val others = sums.filter { categoryColor(it.category) == null }.fold(Money.ZERO) { a, s -> a + s.total }
    return coloured + if (others > Money.ZERO) listOf(OTHER_CATEGORY_COLOR to others) else emptyList()
}

/** Donut with 2 dp gaps (changes-ui-2.0 §3: 36 dp thick, 24 in the small one); [center] goes in the hole. */
@Composable
fun DonutChart(
    slices: List<Pair<Color, Money>>,
    size: Dp,
    thickness: Dp,
    description: String,
    modifier: Modifier = Modifier,
    center: @Composable BoxScope.() -> Unit = {},
) {
    val track = CofferTheme.colors.border
    Box(modifier.size(size).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = thickness.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val total = slices.fold(0L) { s, (_, m) -> s + m.scaled }
            if (total <= 0L) {
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                return@Canvas
            }
            // 2 dp of gap measured on the middle of the ring
            val gap = if (slices.size > 1) (2.dp.toPx() / (Math.PI.toFloat() * arcSize.width)) * 360f else 0f
            var start = -90f
            slices.forEach { (color, money) ->
                val sweep = money.scaled.toFloat() / total * 360f
                drawArc(
                    color,
                    start + gap / 2,
                    (sweep - gap).coerceAtLeast(0.5f),
                    false,
                    Offset(inset, inset),
                    arcSize,
                    style = Stroke(stroke),
                )
                start += sweep
            }
        }
        center()
    }
}

internal fun Modifier.dashedBorder(
    color: Color,
    radius: Dp = 6.dp,
    width: Dp = 1.5.dp,
) = drawBehind {
    drawRoundRect(
        color,
        style = Stroke(width = width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))),
        cornerRadius = CornerRadius(radius.toPx()),
    )
}

/** «$1,32 M», «$850 mil»: bar labels that fit. */
internal fun shortMoney(value: Money): String {
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

/** «37,8 %» from [com.dev.julian09.financeapp.domain.finance.shareOf]. */
internal fun BigDecimal?.percentLabel(): String = this?.let { it.stripTrailingZeros().toPlainString().replace('.', ',') + " %" } ?: "—"

/** A square colour mark, the legend of the pie. */
@Composable
internal fun ColorSquare(
    color: Color,
    modifier: Modifier = Modifier,
) = Box(modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
