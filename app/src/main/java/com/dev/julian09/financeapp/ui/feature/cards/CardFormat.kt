package com.dev.julian09.financeapp.ui.feature.cards

import com.dev.julian09.financeapp.domain.finance.RateConverter
import com.dev.julian09.financeapp.domain.model.RateType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val PERCENT = DecimalFormat("#,##0.0##", DecimalFormatSymbols(Locale.forLanguageTag("es-CO")))
private val HUNDRED = BigDecimal(100)

/** "2,0" or "2.0" as typed → the fraction 0.02; null when it is not a number. */
fun parsePercent(text: String): BigDecimal? =
    text
        .trim()
        .replace(',', '.')
        .toBigDecimalOrNull()
        ?.divide(HUNDRED)

/** 0.02 → "2,0", for the rate field. */
fun percentText(fraction: BigDecimal): String = PERCENT.format(fraction.multiply(HUNDRED).setScale(3, RoundingMode.HALF_EVEN))

fun rateTypeLabel(type: RateType) = if (type == RateType.MV) "M.V." else "E.A."

/** REQ-TC-02, the conversion visible: "2,0 % M.V. · 26,824 % E.A.". */
fun rateLabel(
    rate: BigDecimal,
    type: RateType,
): String {
    val monthly = RateConverter.toMonthly(rate, type)
    val annual = RateConverter.toAnnual(rate, type)
    return "${percentText(monthly)} % M.V. · ${percentText(annual)} % E.A."
}
