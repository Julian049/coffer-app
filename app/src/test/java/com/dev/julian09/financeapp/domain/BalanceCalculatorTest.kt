package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.finance.BalanceCalculator
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountFlows
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.util.UUID

class BalanceCalculatorTest {
    private fun pesos(value: Long) = Money.ofPesos(value)

    private fun account(
        type: AccountType = AccountType.DEBIT,
        initial: Long = 500_000,
        active: Boolean = true,
        include: Boolean = true,
    ) = Account(UUID.randomUUID(), "Cuenta", type, pesos(initial), include, active)

    private val flows = AccountFlows(pesos(2_400_000), pesos(1_264_000), pesos(886_000), pesos(100_000))

    @Test
    fun `an asset is initial plus income minus expenses plus or minus transfers`() {
        // Detalle de cuenta in Figma: 500.000 + 2.400.000 − 1.264.000 − 786.000 (net transfers) = 850.000
        assertEquals(pesos(850_000), BalanceCalculator.balance(account(), flows))
    }

    @Test
    fun `a debt shows negative and only what comes in lowers it`() {
        val card = account(AccountType.CREDIT_CARD, initial = 1_000_000)
        val debt = AccountFlows(expense = pesos(200_000), transfersIn = pesos(300_000))
        assertEquals(pesos(-900_000), BalanceCalculator.balance(card, debt))
    }

    @Test
    fun `available counts only active accounts included in the total and never debts`() {
        val balances =
            listOf(
                AccountBalance(account(initial = 100_000), AccountFlows()),
                AccountBalance(account(initial = 200_000, include = false), AccountFlows()),
                AccountBalance(account(initial = 300_000, active = false), AccountFlows()),
                AccountBalance(account(AccountType.LOAN, initial = 400_000), AccountFlows()),
                AccountBalance(account(AccountType.SAVINGS, initial = 50_000), AccountFlows(expense = pesos(70_000))),
            )
        assertEquals(pesos(80_000), BalanceCalculator.available(balances))
    }

    @Test
    fun `monthly change is a percent against the previous close`() {
        assertEquals(BigDecimal("8.4"), BalanceCalculator.monthlyChange(pesos(2_750_000), pesos(2_537_000)))
        assertEquals(BigDecimal("-50.0"), BalanceCalculator.monthlyChange(pesos(50), pesos(100)))
        // From a negative close, going up still reads as up
        assertEquals(BigDecimal("50.0"), BalanceCalculator.monthlyChange(pesos(-50), pesos(-100)))
        assertNull(BalanceCalculator.monthlyChange(pesos(1_000), Money.ZERO))
    }
}
