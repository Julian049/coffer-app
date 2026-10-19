package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.finance.InstallmentCalculator
import com.dev.julian09.financeapp.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** S7-QA01, REQ-TC-03, 07, 15, detailed-design §6.9.2. */
class InstallmentCalculatorTest {
    private val first = LocalDate.parse("2026-11-05")

    @Test
    fun theExampleOfHu08HasFixedPrincipalAndInterestOnTheBalance() {
        val plan = InstallmentCalculator.plan(Money.ofPesos(1_200_000), 12, BigDecimal("0.02"), first, 5, false)
        assertEquals(12, plan.size)
        assertTrue(plan.all { it.principal == Money.ofPesos(100_000) })
        assertEquals((12 downTo 1).map { Money.ofPesos(it * 2_000L) }, plan.map { it.interest })
        assertEquals(LocalDate.parse("2026-11-05"), plan.first().dueDate)
        assertEquals(LocalDate.parse("2027-10-05"), plan.last().dueDate)
    }

    @Test
    fun theFirstInstallmentCanBeInterestFree() {
        val plan = InstallmentCalculator.plan(Money.ofPesos(1_200_000), 12, BigDecimal("0.02"), first, 5, true)
        assertEquals(Money.ZERO, plan.first().interest)
        assertEquals(Money.ofPesos(100_000), plan.first().principal + plan.first().interest)
        assertEquals(Money.ofPesos(22_000), plan[1].interest)
    }

    @Test
    fun aZeroRateAgreementHasNoInterest() {
        val plan = InstallmentCalculator.plan(Money.ofPesos(480_000), 6, BigDecimal.ZERO, first, 5, false)
        assertTrue(plan.all { it.interest == Money.ZERO })
    }

    @Test
    fun theLastInstallmentAbsorbsTheRoundingSoThePrincipalAlwaysAddsUp() {
        listOf(1L, 4L, 999L, 100_000L, 1_234_567L, 9_999_999L).forEach { pesos ->
            (2..72).forEach { count ->
                val plan = InstallmentCalculator.plan(Money.ofPesos(pesos), count, BigDecimal("0.0187"), first, 5, false)
                val total = plan.fold(Money.ZERO) { sum, it -> sum + it.principal }
                assertEquals("$pesos in $count", Money.ofPesos(pesos), total)
                assertTrue("$pesos in $count", plan.none { it.principal.isNegative || it.interest.isNegative })
            }
        }
    }

    @Test
    fun aPaymentDayOf31FallsOnTheLastDayOfShortMonths() {
        val plan = InstallmentCalculator.plan(Money.ofPesos(300_000), 3, BigDecimal.ZERO, LocalDate.parse("2027-01-31"), 31, false)
        assertEquals(listOf("2027-01-31", "2027-02-28", "2027-03-31"), plan.map { it.dueDate.toString() })
    }
}
