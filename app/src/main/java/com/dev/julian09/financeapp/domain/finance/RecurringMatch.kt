package com.dev.julian09.financeapp.domain.finance

import com.dev.julian09.financeapp.domain.model.Recurring
import com.dev.julian09.financeapp.domain.model.TransactionDraft
import com.dev.julian09.financeapp.domain.model.TransactionType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/** How many days around its due date an email may arrive and still be the payment of a recurring expense (roadmap-v2.4 2.4-07). */
const val RECURRING_WINDOW_DAYS = 3L

/**
 * 2.4-07: the pending recurring expense an expense draft pays: same account, the exact amount (the estimated one or the last
 * paid) and a due date at most [RECURRING_WINDOW_DAYS] from the draft's [day]. The closest due date wins. The amount is exact on
 * purpose (PO, 2026-11-05): charges in another currency do not match yet and a margin would come later as a patch.
 */
fun matchRecurring(
    draft: TransactionDraft,
    accountId: UUID,
    day: LocalDate,
    recurring: List<Recurring>,
): Recurring? =
    if (draft.type != TransactionType.EXPENSE) {
        null
    } else {
        recurring
            .filter { r ->
                r.active &&
                    r.accountId == accountId &&
                    (r.estimatedAmount == draft.amount || r.lastPaidAmount == draft.amount) &&
                    ChronoUnit.DAYS.between(day, r.nextDueDate) in -RECURRING_WINDOW_DAYS..RECURRING_WINDOW_DAYS
            }.minByOrNull { Math.abs(ChronoUnit.DAYS.between(day, it.nextDueDate)) }
    }
