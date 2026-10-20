package com.dev.julian09.financeapp.domain.importing

import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.TransactionType
import java.time.Instant
import java.time.temporal.ChronoUnit

/** REQ-IMP-04, detailed-design §6.9.9: same minute, amount, account and type. [A] is an account id or name. */
data class DuplicateKey<A>(
    val minute: Instant,
    val amount: Money,
    val account: A,
    val type: TransactionType,
) {
    companion object {
        fun <A> of(
            occurredAt: Instant,
            amount: Money,
            account: A,
            type: TransactionType,
        ) = DuplicateKey(occurredAt.truncatedTo(ChronoUnit.MINUTES), amount, account, type)
    }
}

/**
 * A row is a possible duplicate if its key is already stored. Rows of the same file are never compared with each
 * other: two bus fares in the same minute are two real movements (coffer-app#72).
 */
class DuplicateDetector<A>(
    existing: Iterable<DuplicateKey<A>>,
) {
    private val stored = existing.toHashSet()

    fun isDuplicate(key: DuplicateKey<A>): Boolean = key in stored
}
