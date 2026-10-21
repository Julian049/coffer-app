package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.finance.BalanceCalculator
import com.dev.julian09.financeapp.domain.finance.FoxMood
import com.dev.julian09.financeapp.domain.finance.FoxRules
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

/**
 * M14 (#97, PO 2026-10-21): the mascot of a month in Historial. The current month shows Home's; a past month the
 * state it closed with, by the same rules as on its last day, without pending payments (they belong to today).
 */
class ObserveMonthFox
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val transactions: TransactionRepository,
        private val home: ObserveHomeDashboard,
        private val clock: Clock,
    ) {
        operator fun invoke(month: YearMonth): Flow<FoxMood> {
            if (month >= YearMonth.from(clock.today())) return home().map { it.fox }
            val zone = clock.zone()
            val start = month.atDay(1).atStartOfDay(zone).toInstant()
            val end =
                month
                    .plusMonths(1)
                    .atDay(1)
                    .atStartOfDay(zone)
                    .toInstant()
            val previousStart =
                month
                    .minusMonths(1)
                    .atDay(1)
                    .atStartOfDay(zone)
                    .toInstant()
            return combine(
                accounts.observeBalances(before = end),
                accounts.observeBalances(before = start),
                transactions.observeTotals(start, end),
                transactions.observeTotals(previousStart, start),
            ) { atClose, atOpen, totals, previous ->
                val available = BalanceCalculator.available(atClose)
                val existing = atClose.filter { (it.account.createdAt ?: start) < start }.map { it.account.id }.toSet()
                val opening = BalanceCalculator.available(atOpen.filter { it.account.id in existing })
                FoxRules.evaluate(
                    hasMovements = totals.income > Money.ZERO || totals.expense > Money.ZERO,
                    available = available,
                    monthlyChange = BalanceCalculator.monthlyChange(available, opening),
                    month = totals,
                    previousMonth = previous,
                    today = month.atEndOfMonth(),
                )
            }
        }
    }
