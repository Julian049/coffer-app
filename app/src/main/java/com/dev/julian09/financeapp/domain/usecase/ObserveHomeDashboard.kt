package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.finance.BalanceCalculator
import com.dev.julian09.financeapp.domain.finance.FoxMood
import com.dev.julian09.financeapp.domain.finance.FoxRules
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.DuePayment
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject

data class HomeDashboard(
    val available: Money,
    /** Percent against the close of [previousMonth]; null when there is nothing to compare (PO, 2026-10-17). */
    val monthlyChange: BigDecimal?,
    val previousMonth: YearMonth,
    /** Every not deleted account; Home shows the active ones and names movements with all of them. */
    val accounts: List<AccountBalance>,
    /** REQ-TRX-11: today and the 4 days before. */
    val recent: List<Transaction>,
    val fox: FoxMood,
    /** REQ-INI-03: recurring expenses, card statements and loan installments inside the reminder window, by due date. */
    val pending: List<DuePayment> = emptyList(),
)

/** S4-DOM03: total available, accounts, last 5 days and the mascot state. */
class ObserveHomeDashboard
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val transactions: TransactionRepository,
        private val pendingPayments: ObservePendingPayments,
        private val cardDues: ObserveCardDues,
        private val loanDues: ObserveLoanDues,
        private val clock: Clock,
    ) {
        operator fun invoke(): Flow<HomeDashboard> {
            val zone = clock.zone()
            val today = clock.today()
            val month = YearMonth.from(today)
            val monthStart = month.atDay(1).atStartOfDay(zone).toInstant()
            val previousStart =
                month
                    .minusMonths(1)
                    .atDay(1)
                    .atStartOfDay(zone)
                    .toInstant()
            val nextStart =
                month
                    .plusMonths(1)
                    .atDay(1)
                    .atStartOfDay(zone)
                    .toInstant()
            val balances =
                combine(accounts.observeBalances(), accounts.observeBalances(before = monthStart)) { now, atClose -> now to atClose }
            val totals =
                combine(
                    transactions.observeTotals(monthStart, nextStart),
                    transactions.observeTotals(previousStart, monthStart),
                    transactions.observeCount(),
                ) { thisMonth, lastMonth, count -> Triple(thisMonth, lastMonth, count) }
            return combine(
                balances,
                totals,
                transactions.observeSince(today.minusDays(4).atStartOfDay(zone).toInstant()),
                combine(pendingPayments(), cardDues(), loanDues()) { recurring, cards, loans ->
                    (recurring + cards + loans).sortedBy { it.dueDate }
                },
            ) { (now, atClose), (thisMonth, lastMonth, count), recent, pending ->
                val available = BalanceCalculator.available(now)
                // Same accounts as today that already existed then: a new account has no previous close (PO, 2026-10-17)
                val activeNow = now.filter { it.account.active }.map { it.account.id }.toSet()
                val previousClose =
                    BalanceCalculator.available(
                        atClose.filter { it.account.id in activeNow && (it.account.createdAt ?: monthStart) < monthStart },
                    )
                val change = BalanceCalculator.monthlyChange(available, previousClose)
                HomeDashboard(
                    available = available,
                    monthlyChange = change,
                    previousMonth = month.minusMonths(1),
                    accounts = now,
                    recent = recent,
                    fox = FoxRules.evaluate(count > 0, available, change, thisMonth, lastMonth, today, pending),
                    pending = pending.filter { it.inWindow },
                )
            }
        }
    }
