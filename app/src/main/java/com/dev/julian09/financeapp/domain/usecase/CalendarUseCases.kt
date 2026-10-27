package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.Clock
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

/** REQ-CAL-03 default: cash and banks; loans out; cards only when the user picks them. */
fun defaultCalendarAccounts(accounts: List<Account>): Set<UUID> =
    accounts
        .filter { it.active && it.type != AccountType.LOAN && it.type != AccountType.CREDIT_CARD }
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
            var previous = closing(month.atDay(1).minusDays(1))
            val days = mutableListOf<DayBalance>()
            var day = month.atDay(1)
            while (day <= last) {
                val value = closing(day)
                days += DayBalance(day, value, value - previous)
                previous = value
                day = day.plusDays(1)
            }
            return days
        }
    }
