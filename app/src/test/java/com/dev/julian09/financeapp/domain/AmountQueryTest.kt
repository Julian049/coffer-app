package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.model.AmountQuery
import com.dev.julian09.financeapp.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/** S15-QA03: the search text read as an amount (S15-DOM03). */
class AmountQueryTest {
    @Test
    fun readsTheColombianFormats() {
        val expected = Money.ofPesos(45_000)
        listOf("45000", "45.000", "$45.000", "$ 45.000", " 45.000 ").forEach { assertEquals(it, expected, AmountQuery.parse(it)) }
        assertEquals(Money.ofPesos(1_250_000), AmountQuery.parse("1.250.000"))
        assertEquals(Money.of(BigDecimal("45000.50")), AmountQuery.parse("45.000,50"))
        assertEquals(Money.of(BigDecimal("12.5")), AmountQuery.parse("12,5"))
    }

    @Test
    fun anythingElseIsJustText() {
        listOf("", "  ", "$", "almuerzo", "45.00", "45.0000", "1.25", "4,5,6", "12abc", "-5", "0", "0,00").forEach {
            assertNull(it, AmountQuery.parse(it))
        }
        assertNull(AmountQuery.parse("99999999999999999999"))
    }
}
