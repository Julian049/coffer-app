package com.dev.julian09.financeapp.ui.components

import com.dev.julian09.financeapp.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MoneyFormatTest {
    @Test
    fun `pesos use dots for thousands and no decimals`() {
        assertEquals("$1.250.000", formatMoney(Money.ofPesos(1_250_000)))
        assertEquals("$0", formatMoney(Money(0)))
        assertEquals("$950", formatMoney(Money.ofPesos(950)))
    }

    @Test
    fun `cents round half up`() {
        assertEquals("$1.001", formatMoney(Money.of(BigDecimal("1000.5"))))
        assertEquals("$1.000", formatMoney(Money.of(BigDecimal("1000.4999"))))
    }

    @Test
    fun `sign follows the financial meaning`() {
        val amount = Money.ofPesos(28_000)
        assertEquals("+$28.000", formatMoney(amount, MoneyKind.Income))
        assertEquals("−$28.000", formatMoney(amount, MoneyKind.Expense))
        assertEquals("$28.000", formatMoney(amount, MoneyKind.Transfer))
        assertEquals("−$28.000", formatMoney(Money.ofPesos(-28_000), MoneyKind.Neutral))
        assertEquals("−$28.000", formatMoney(Money.ofPesos(-28_000), MoneyKind.Debt))
    }
}
