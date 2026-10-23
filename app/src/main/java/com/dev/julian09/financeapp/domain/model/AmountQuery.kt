package com.dev.julian09.financeapp.domain.model

import java.math.BigDecimal

/**
 * S15-DOM03: the search text read as an amount in Colombian format, «.» for thousands and «,» for decimals:
 * `45000`, `45.000`, `$45.000`, `1.250.000`, `45.000,50`. Anything else is not an amount and is only searched as text.
 */
object AmountQuery {
    private val thousands = Regex("""\d{1,3}(\.\d{3})+(,\d{1,4})?""")
    private val plain = Regex("""\d+(,\d{1,4})?""")

    fun parse(text: String): Money? {
        val body = text.trim().removePrefix("$").trim()
        if (!thousands.matches(body) && !plain.matches(body)) return null
        return try {
            Money.of(BigDecimal(body.replace(".", "").replace(',', '.'))).takeIf { it.scaled > 0 }
        } catch (_: ArithmeticException) {
            null
        }
    }
}
