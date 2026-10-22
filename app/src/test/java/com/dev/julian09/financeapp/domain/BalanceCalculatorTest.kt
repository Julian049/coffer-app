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
        type: AccountType = AccountType.SAVINGS,
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
    fun `the net balance takes off what cards and loans ask for this period`() {
        // HU-04: $1.000.000 in accounts and $800.000 of card due this period → net $200.000, available $1.000.000
        assertEquals(pesos(200_000), BalanceCalculator.net(pesos(1_000_000), listOf(pesos(800_000))))
        // Figma, Hoja · Detalle del saldo: 2.750.000 − 250.000 (Visa) − 281.958 (loan installment) = 2.218.042
        assertEquals(pesos(2_218_042), BalanceCalculator.net(pesos(2_750_000), listOf(pesos(250_000), pesos(281_958))))
        assertEquals(pesos(1_000_000), BalanceCalculator.net(pesos(1_000_000), emptyList()))
    }

    @Test
    fun `the net balance by total debt takes off every active card and loan that owes`() {
        val balances =
            listOf(
                AccountBalance(account(initial = 2_750_000), AccountFlows()),
                AccountBalance(account(AccountType.CREDIT_CARD, initial = 800_000), AccountFlows()),
                AccountBalance(account(AccountType.LOAN, initial = 4_375_000), AccountFlows()),
                AccountBalance(account(AccountType.LOAN, initial = 100_000, active = false), AccountFlows()),
                AccountBalance(account(AccountType.CREDIT_CARD, initial = 0), AccountFlows()),
            )
        val debts = BalanceCalculator.debts(balances)
        assertEquals(2, debts.size)
        // Figma, Hoja · Detalle del saldo · Neto con deuda total: 2.750.000 − 800.000 − 4.375.000 = −2.425.000
        assertEquals(pesos(-2_425_000), BalanceCalculator.net(BalanceCalculator.available(balances), debts.map { -it.balance }))
    }

    @Test
    fun `a debt shows negative and only what comes in lowers it`() {
        val card = account(AccountType.CREDIT_CARD, initial = 1_000_000)
        val debt = AccountFlows(expense = pesos(200_000), transfersIn = pesos(300_000))
        assertEquals(pesos(-900_000), BalanceCalculator.balance(card, debt))
        // A refund on the card lowers the debt too (coffer-app#72)
        assertEquals(pesos(-850_000), BalanceCalculator.balance(card, debt.copy(income = pesos(50_000))))
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
