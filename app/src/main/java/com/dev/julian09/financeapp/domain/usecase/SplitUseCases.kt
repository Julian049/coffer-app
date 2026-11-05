package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.DraftPrefs
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/**
 * One part of a split expense (2.4 §6). The first part is the user's own and stays an expense; each other part [accountId] is
 * where somebody paid the user back, and becomes a transfer from the account that paid.
 */
data class SplitPart(
    val amount: Money,
    val accountId: UUID? = null,
)

enum class SplitError { NOT_AN_EXPENSE, INSTALLMENTS, TOO_FEW_PARTS, PART_EMPTY, ACCOUNT_REQUIRED, SUM_MISMATCH }

/** «Mitad»: what the user paid and what came back, summing exactly [total] (the odd cent-of-peso stays with the other part). */
fun halves(total: Money): Pair<Money, Money> {
    val own = Money(total.scaled / 2)
    return own to total - own
}

/** The first thing wrong with [parts] for [original], or null. Own part first; at least one other. */
fun splitError(
    original: Transaction,
    parts: List<SplitPart>,
): SplitError? =
    when {
        original.type != TransactionType.EXPENSE -> SplitError.NOT_AN_EXPENSE
        original.installments > 1 || original.paymentKind != null -> SplitError.INSTALLMENTS
        parts.size < 2 -> SplitError.TOO_FEW_PARTS
        parts.any { it.amount <= Money.ZERO } -> SplitError.PART_EMPTY
        parts.drop(1).any { it.accountId == null } -> SplitError.ACCOUNT_REQUIRED
        parts.fold(Money.ZERO) { sum, p -> sum + p.amount } != original.amount -> SplitError.SUM_MISMATCH
        else -> null
    }

/**
 * 2.4-11 Dividir: the user's part stays the expense (same id, so its history is kept) and each part somebody else owes
 * becomes a transfer from the account that paid to the account the money came to, with the same title. A part that came back
 * to the same account is no transfer: it only lowers the expense. The parts share a `splitGroupId`; nothing says «dividido».
 * [original] may not be stored yet (Revisar transacción): then everything is registered together.
 */
class SplitTransaction
    @Inject
    constructor(
        private val transactions: TransactionRepository,
        private val validator: TransactionValidator,
        private val prefs: DraftPrefs,
        private val clock: Clock,
        private val scheduler: SyncScheduler,
    ) {
        /** Null when it worked. */
        suspend operator fun invoke(
            original: Transaction,
            parts: List<SplitPart>,
        ): SplitError? {
            splitError(original, parts)?.let { return it }
            val group = UUID.randomUUID()
            val own = original.copy(amount = parts.first().amount, splitGroupId = group)
            val transfers =
                parts
                    .drop(1)
                    .filter { it.accountId != original.accountId }
                    .map {
                        Transaction(
                            UUID.randomUUID(),
                            TransactionType.TRANSFER,
                            it.amount,
                            original.occurredAt,
                            original.accountId,
                            destinationAccountId = it.accountId,
                            note = original.note,
                            sourceMerchant = original.sourceMerchant,
                            draftId = original.draftId,
                            splitGroupId = group,
                        )
                    }
            (listOf(own) + transfers).forEach { if (validator.check(it).isNotEmpty()) return SplitError.ACCOUNT_REQUIRED }
            if (transactions.observe(original.id).first() != null) {
                transactions.update(own)
                transactions.insertAll(transfers)
            } else {
                transactions.insertAll(listOf(own) + transfers)
            }
            // Dividing restarts what is learned of the merchant, or the next full amount would enter by itself
            original.sourceMerchant?.let { prefs.resetLearning(learningKey(original.accountId, it), clock.now()) }
            scheduler.requestSync()
            return null
        }
    }
