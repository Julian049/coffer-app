package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.finance.BalanceCalculator
import com.dev.julian09.financeapp.domain.finance.FoxMood
import com.dev.julian09.financeapp.domain.finance.FoxRules
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.CardDue
import com.dev.julian09.financeapp.domain.model.CardOverview
import com.dev.julian09.financeapp.domain.model.DuePayment
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.LoanDue
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.NetBalanceMode
import com.dev.julian09.financeapp.domain.model.NetChoices
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.NetBalancePrefs
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID
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
    /** REQ-CTA-08: [available] minus [dues]. */
    val net: Money = available,
    /** What the cards and loans ask for this period: each card's statement and each loan's next installment. */
    val dues: List<DuePayment> = emptyList(),
    /** Active cards and loans with a debt, for [NetBalanceMode.CUSTOM]. */
    val debts: List<AccountBalance> = emptyList(),
    /** The cards among [debts], by account id, with their statement and installment purchases. */
    val cards: Map<UUID, CardOverview> = emptyMap(),
    val choices: NetChoices = NetChoices(),
) {
    /** REQ-CTA-08 (S12-H03): the available total minus this period's [dues] or what «Personalizado» marks. */
    fun net(mode: NetBalanceMode): Money =
        when (mode) {
            NetBalanceMode.PERIOD -> net
            NetBalanceMode.CUSTOM -> BalanceCalculator.net(available, debts.filter { it.account.includeInNet }.map(::customDebt))
        }

    /**
     * What «Personalizado» subtracts for [debt] (2.5, PO 2026-11-24): a loan's whole debt; a card's whole debt or, when
     * marked «Solo el periodo», this period's payment; less what is left of the purchases left out (their unpaid
     * principal, or their installment of this period).
     */
    fun customDebt(debt: AccountBalance): Money {
        val card = cards[debt.account.id] ?: return -debt.balance
        val out =
            card.installments.filter {
                it.deferredPurchaseId in choices.excludedPurchases && it.status == InstallmentStatus.SCHEDULED
            }
        val left =
            if (debt.account.id in choices.periodCards) {
                val dueDate = card.statement.period.dueDate
                card.statement.minimum - out.filter { it.dueDate == dueDate }.fold(Money.ZERO) { sum, i -> sum + i.total }
            } else {
                -debt.balance - out.fold(Money.ZERO) { sum, i -> sum + i.principal }
            }
        return if (left.isNegative) Money.ZERO else left
    }
}

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
        private val cards: ObserveCards,
        private val netPrefs: NetBalancePrefs,
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
                combine(cards(), netPrefs.observe()) { all, choices -> all.associateBy { it.card.accountId } to choices },
            ) { (now, atClose), (thisMonth, lastMonth, count), recent, pending, (overviews, choices) ->
                val available = BalanceCalculator.available(now)
                // Same accounts as today that already existed then: a new account has no previous close (PO, 2026-10-17)
                val activeNow = now.filter { it.account.active }.map { it.account.id }.toSet()
                val previousClose =
                    BalanceCalculator.available(
                        atClose.filter { it.account.id in activeNow && (it.account.createdAt ?: monthStart) < monthStart },
                    )
                val change = BalanceCalculator.monthlyChange(available, previousClose)
                val dues = pending.filter { (it is CardDue || it is LoanDue) && it.amount > Money.ZERO }
                HomeDashboard(
                    available = available,
                    monthlyChange = change,
                    previousMonth = month.minusMonths(1),
                    accounts = now,
                    recent = recent,
                    fox = FoxRules.evaluate(count > 0, available, change, thisMonth, lastMonth, today, pending),
                    pending = pending.filter { it.inWindow },
                    net = BalanceCalculator.net(available, dues.map { it.amount }),
                    dues = dues,
                    debts = BalanceCalculator.debts(now),
                    cards = overviews,
                    choices = choices,
                )
            }
        }
    }
