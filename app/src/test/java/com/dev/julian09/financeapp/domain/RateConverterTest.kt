package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.finance.RateConverter
import com.dev.julian09.financeapp.domain.model.RateType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/** S7-QA01, REQ-TC-02, detailed-design §6.9.1. */
class RateConverterTest {
    private fun six(value: BigDecimal) = value.setScale(6, RoundingMode.HALF_EVEN).toPlainString()

    @Test
    fun twoPercentMonthlyIsAbout26_8242PercentEffectiveAnnual() {
        assertEquals("0.268242", six(RateConverter.toAnnual(BigDecimal("0.02"), RateType.MV)))
        assertEquals("0.020000", six(RateConverter.toMonthly(BigDecimal("0.268242"), RateType.EA)))
    }

    @Test
    fun aRateAlreadyOfTheAskedTypeStaysTheSame() {
        assertEquals("0.020000", six(RateConverter.toMonthly(BigDecimal("0.02"), RateType.MV)))
        assertEquals("0.300000", six(RateConverter.toAnnual(BigDecimal("0.3"), RateType.EA)))
    }

    @Test
    fun goingToMonthlyAndBackGivesTheSameAnnualRate() {
        listOf("0", "0.12", "0.3", "0.5", "1").forEach { ea ->
            val monthly = RateConverter.toMonthly(BigDecimal(ea), RateType.EA)
            assertEquals(ea, six(BigDecimal(ea)), six(RateConverter.toAnnual(monthly, RateType.MV)))
        }
    }
}
