package com.dev.julian09.financeapp.ui.components

import androidx.compose.ui.text.AnnotatedString
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

    @Test
    fun `an amount field keeps the digits and the cursor where the user types (E1)`() {
        val shown = PesosTransformation().filter(AnnotatedString("1250000"))
        assertEquals("$1.250.000", shown.text.text)
        // Typing at the end keeps the cursor at the end, whatever dots appeared
        assertEquals("$1.250.000".length, shown.offsetMapping.originalToTransformed(7))
        assertEquals(7, shown.offsetMapping.originalToTransformed(4)) // before the 5th digit, past its dot
        assertEquals(1, shown.offsetMapping.originalToTransformed(0))
        assertEquals(4, shown.offsetMapping.transformedToOriginal(6))
        assertEquals(7, shown.offsetMapping.transformedToOriginal(10))

        val plain = PesosTransformation(symbol = false).filter(AnnotatedString("1000"))
        assertEquals("1.000", plain.text.text)
        assertEquals(2, plain.offsetMapping.originalToTransformed(1))
        assertEquals("", PesosTransformation().filter(AnnotatedString("")).text.text)
    }
}
