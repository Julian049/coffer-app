package com.dev.julian09.financeapp.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/** An amount as a Long of ten-thousandths (ADR-0004): 28,000 pesos = 280_000_000. Never a Double. */
@JvmInline
value class Money(
    val scaled: Long,
) {
    fun toBigDecimal(): BigDecimal = BigDecimal.valueOf(scaled, SCALE)

    val isNegative: Boolean get() = scaled < 0

    operator fun plus(other: Money) = Money(Math.addExact(scaled, other.scaled))

    operator fun minus(other: Money) = Money(Math.subtractExact(scaled, other.scaled))

    operator fun unaryMinus() = Money(Math.negateExact(scaled))

    operator fun compareTo(other: Money) = scaled.compareTo(other.scaled)

    companion object {
        const val SCALE = 4
        val ZERO = Money(0)

        fun of(amount: BigDecimal): Money = Money(amount.setScale(SCALE, RoundingMode.HALF_EVEN).unscaledValue().longValueExact())

        fun ofPesos(pesos: Long): Money = Money(Math.multiplyExact(pesos, 10_000L))
    }
}
