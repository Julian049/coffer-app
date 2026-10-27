package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

/** One cell of the calendar (REQ-CAL-01, 02): the balance at the end of [date] and what it moved that day. */
data class DayBalance(
    val date: LocalDate,
    val closing: Money,
    val change: Money,
)

/** REQ-CAL-03 default: the accounts in Inicio's total; cards and loans only when the user picks them (PO, 2026-10-26). */
fun defaultCalendarAccounts(accounts: List<Account>): Set<UUID> =
    accounts
        .filter { it.active && it.includeInTotal && !it.type.isDebt }
        .map { it.id }
        .toSet()

/**
 * The closing balance of each day of [month] up to today, for [accountIds], with the same balance rules as Inicio:
 * each day is the balance of the accounts before the next midnight (detailed-design §6.6.3), so the last cell of the
 * current month equals Inicio's balance of those accounts (roadmap-v2.0 §4.16).
 */
class ObserveDailyBalances
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            month: YearMonth,
            accountIds: Set<UUID>,
        ): List<DayBalance> {
            val today = clock.today()
            val last = minOf(month.atEndOfMonth(), today)
            if (last < month.atDay(1)) return emptyList()
            val zone = clock.zone()

            suspend fun closing(date: LocalDate): Money =
                accounts
                    .observeBalances(before = date.plusDays(1).atStartOfDay(zone).toInstant())
                    .first()
                    .filter { it.account.id in accountIds }
                    .fold(Money.ZERO) { s, b -> s + b.balance }
            // INC-05: one query per day, so they run side by side instead of one after another
            val dates = generateSequence(month.atDay(1).minusDays(1)) { it.plusDays(1) }.takeWhile { it <= last }.toList()
            val closings = coroutineScope { dates.map { async { closing(it) } }.awaitAll() }
            return dates.drop(1).mapIndexed { i, day -> DayBalance(day, closings[i + 1], closings[i + 1] - closings[i]) }
        }
    }
