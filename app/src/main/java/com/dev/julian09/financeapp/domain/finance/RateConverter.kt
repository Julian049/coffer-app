package com.dev.julian09.financeapp.domain.finance

import com.dev.julian09.financeapp.domain.model.RateType
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** REQ-TC-02, detailed-design §6.9.1: MV = (1 + EA)^(1/12) − 1 and back, as fractions with 10 decimals. */
object RateConverter {
    const val SCALE = 10
    private val MC = MathContext.DECIMAL128
    private val TWELVE = BigDecimal(12)

    fun toMonthly(
        rate: BigDecimal,
        type: RateType,
    ): BigDecimal =
        when (type) {
            RateType.MV -> rate
            RateType.EA -> twelfthRoot(BigDecimal.ONE + rate) - BigDecimal.ONE
        }.setScale(SCALE, RoundingMode.HALF_EVEN)

    fun toAnnual(
        rate: BigDecimal,
        type: RateType,
    ): BigDecimal =
        when (type) {
            RateType.EA -> rate
            RateType.MV -> (BigDecimal.ONE + rate).pow(12, MC) - BigDecimal.ONE
        }.setScale(SCALE, RoundingMode.HALF_EVEN)

    /** Newton's method on x¹² = a: BigDecimal has no fractional power and a Double is not allowed (ADR-0004). */
    private fun twelfthRoot(a: BigDecimal): BigDecimal {
        var x = BigDecimal.ONE + (a - BigDecimal.ONE).divide(TWELVE, MC)
        repeat(100) {
            val next = x - (x.pow(12, MC) - a).divide(TWELVE.multiply(x.pow(11, MC), MC), MC)
            if (next.compareTo(x) == 0) return next
            x = next
        }
        return x
    }
}
