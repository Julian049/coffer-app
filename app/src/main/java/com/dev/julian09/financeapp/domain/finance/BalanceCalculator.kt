package com.dev.julian09.financeapp.domain.finance

import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountFlows
import com.dev.julian09.financeapp.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode

/** Balances (REQ-CTA-06, 07; detailed-design §6.9.5). */
object BalanceCalculator {
    fun balance(
        account: Account,
        flows: AccountFlows,
    ): Money =
        if (account.type.isDebt) {
            // Only the principal of a payment lowers the debt; the query already sums coalesce(principal, amount).
            // An income (a refund, a payment recorded as income) lowers it too (coffer-app#72)
            -account.initialBalance + flows.income - flows.expense - flows.transfersOut + flows.transfersIn
        } else {
            account.initialBalance + flows.income - flows.expense - flows.transfersOut + flows.transfersIn
        }

    /** Total shown in Home: active asset accounts included in the total (REQ-CTA-07, 08). */
    fun available(balances: List<AccountBalance>): Money =
        balances
            .filter { it.account.active && it.account.includeInTotal && !it.account.type.isDebt }
            .fold(Money.ZERO) { sum, it -> sum + it.balance }

    /**
     * REQ-CTA-08: the available total minus what the cards and loans ask for in this period, so what is left to spend.
     * [dues] are the current statement of each card and the next installment of each loan.
     */
    fun net(
        available: Money,
        dues: List<Money>,
    ): Money = dues.fold(available) { left, due -> left - due }

    /** Active cards and loans that owe something, for the net balance by total debt (S12-H03). */
    fun debts(balances: List<AccountBalance>): List<AccountBalance> =
        balances.filter { it.account.active && it.account.type.isDebt && it.balance.isNegative }

    /** Percent change against the close of the previous month, one decimal; null when there is nothing to compare. */
    fun monthlyChange(
        now: Money,
        previousClose: Money,
    ): BigDecimal? {
        if (previousClose == Money.ZERO) return null
        return (now - previousClose)
            .toBigDecimal()
            .multiply(BigDecimal(100))
            .divide(previousClose.toBigDecimal().abs(), 1, RoundingMode.HALF_UP)
    }
}
