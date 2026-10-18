package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import com.dev.julian09.financeapp.domain.port.TagRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

private const val NOTE_MAX = 500

/** The fields a movement got wrong, by field; empty when it can be saved. */
typealias TransactionErrors = Map<TransactionField, LedgerError>

enum class TransactionField { AMOUNT, ACCOUNT, DESTINATION, CATEGORY, NOTE }

/** REQ-TRX-01, 02: the same rules on create and on edit; a card purchase (S7) will branch here. */
class TransactionValidator
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val categories: CategoryRepository,
    ) {
        suspend fun check(t: Transaction): TransactionErrors {
            val errors = mutableMapOf<TransactionField, LedgerError>()
            if (t.amount <= Money.ZERO) errors[TransactionField.AMOUNT] = LedgerError.AMOUNT_REQUIRED
            if ((t.note?.length ?: 0) > NOTE_MAX) errors[TransactionField.NOTE] = LedgerError.NOTE_TOO_LONG
            val active =
                accounts
                    .observeBalances()
                    .first()
                    .filter { it.account.active }
                    .map { it.account.id }
                    .toSet()
            if (t.accountId !in active) errors[TransactionField.ACCOUNT] = LedgerError.ACCOUNT_REQUIRED
            if (t.type == TransactionType.TRANSFER) {
                when {
                    t.destinationAccountId == null || t.destinationAccountId !in active ->
                        errors[TransactionField.DESTINATION] = LedgerError.ACCOUNT_REQUIRED
                    t.destinationAccountId == t.accountId -> errors[TransactionField.DESTINATION] = LedgerError.SAME_ACCOUNT
                }
            } else {
                val category = categories.observeAll().first().firstOrNull { it.id == t.categoryId }
                val kind = if (t.type == TransactionType.EXPENSE) CategoryKind.EXPENSE else CategoryKind.INCOME
                when {
                    category == null -> errors[TransactionField.CATEGORY] = LedgerError.CATEGORY_REQUIRED
                    category.kind != kind -> errors[TransactionField.CATEGORY] = LedgerError.KIND_MISMATCH
                }
            }
            return errors
        }
    }

/** SEQ-04 and SEQ-05: an expense, an income or a transfer; offline first, then synced (REQ-TRX-08). */
class RegisterTransaction
    @Inject
    constructor(
        private val repository: TransactionRepository,
        private val validator: TransactionValidator,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(transaction: Transaction): TransactionErrors {
            val clean = transaction.normalized()
            validator.check(clean).takeIf { it.isNotEmpty() }?.let { return it }
            repository.insert(clean)
            scheduler.requestSync()
            return emptyMap()
        }
    }

/** SEQ-14 §14.1: same form and rules; the history keeps the value before and after. */
class UpdateTransaction
    @Inject
    constructor(
        private val repository: TransactionRepository,
        private val validator: TransactionValidator,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(transaction: Transaction): TransactionErrors {
            val clean = transaction.normalized()
            validator.check(clean).takeIf { it.isNotEmpty() }?.let { return it }
            repository.update(clean)
            scheduler.requestSync()
            return emptyMap()
        }
    }

/** REQ-TRX-07: direct after the confirmation dialog; a logical delete that syncs (REQ-SYN-03). */
class DeleteTransaction
    @Inject
    constructor(
        private val repository: TransactionRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(id: UUID) {
            repository.delete(id)
            scheduler.requestSync()
        }
    }

private const val TAG_NAME_MAX = 50

/** Inline "Nueva etiqueta" of the form (REQ-ETQ-01): unique name ignoring case, like the server's uq_tag_name. */
class CreateTag
    @Inject
    constructor(
        private val repository: TagRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(tag: Tag): LedgerError? {
            val name =
                tag.name
                    .trim()
                    .removePrefix("#")
                    .trim()
            when {
                name.isEmpty() -> return LedgerError.NAME_REQUIRED
                name.length > TAG_NAME_MAX -> return LedgerError.NAME_TOO_LONG
                repository.observeAll().first().any { it.name.equals(name, ignoreCase = true) } -> return LedgerError.NAME_TAKEN
            }
            repository.create(tag.copy(name = name))
            scheduler.requestSync()
            return null
        }
    }

/** A transfer has no category and an expense or income no destination (server ck_tx_transfer, ck_tx_category). */
private fun Transaction.normalized() =
    copy(
        note = note?.trim()?.ifEmpty { null },
        categoryId = categoryId.takeIf { type != TransactionType.TRANSFER },
        destinationAccountId = destinationAccountId.takeIf { type == TransactionType.TRANSFER },
    )
