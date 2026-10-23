package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountFlows
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.finance.FoxMood
import com.dev.julian09.financeapp.domain.finance.FoxState
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.NetBalanceMode
import com.dev.julian09.financeapp.domain.usecase.HomeDashboard
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth
import java.util.UUID

/** S15-QA07: the custom net balance subtracts only the debts marked (S15-DOM07). */
class CustomNetBalanceTest {
    private fun debt(
        name: String,
        type: AccountType,
        owed: Long,
        include: Boolean,
    ): AccountBalance {
        val account = Account(UUID.randomUUID(), name, type, Money.ZERO, includeInNet = include)
        return AccountBalance(account, AccountFlows(expense = Money.ofPesos(owed)))
    }

    private fun dashboard(vararg debts: AccountBalance) =
        HomeDashboard(
            available = Money.ofPesos(3_000_000),
            monthlyChange = null,
            previousMonth = YearMonth.of(2026, 9),
            accounts = emptyList(),
            recent = emptyList(),
            fox = FoxMood(FoxState.NORMAL),
            debts = debts.toList(),
        )

    @Test
    fun onlyTheMarkedDebtsAreSubtracted() {
        val visa = debt("Visa", AccountType.CREDIT_CARD, 800_000, include = true)
        val loan = debt("Préstamo", AccountType.LOAN, 4_000_000, include = false)
        assertEquals(Money.ofPesos(2_200_000), dashboard(visa, loan).net(NetBalanceMode.CUSTOM))
    }

    @Test
    fun withEveryDebtMarkedItIsTheWholeDebtLikeBefore() {
        val visa = debt("Visa", AccountType.CREDIT_CARD, 800_000, include = true)
        val loan = debt("Préstamo", AccountType.LOAN, 4_000_000, include = true)
        assertEquals(Money.ofPesos(-1_800_000), dashboard(visa, loan).net(NetBalanceMode.CUSTOM))
    }
}
