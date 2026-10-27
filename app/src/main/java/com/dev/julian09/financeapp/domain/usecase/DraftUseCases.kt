package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionDraft
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.DraftRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/** What [SuggestDraft] found for a draft; either may be missing. */
data class DraftSuggestion(
    val accountId: UUID?,
    val categoryId: UUID?,
)

/** The movement a draft becomes as it came, or null while its account or category is unknown. */
fun TransactionDraft.toTransaction(s: DraftSuggestion): Transaction? {
    val account = s.accountId ?: return null
    val category = s.categoryId ?: return null
    return Transaction(UUID.randomUUID(), type, amount, occurredAt, account, categoryId = category, note = merchant)
}

/** The account whose last 4 digits match (REQ-COR-06) and the category last used with the same merchant (REQ-COR-08). */
class SuggestDraft
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val transactions: TransactionRepository,
    ) {
        suspend operator fun invoke(draft: TransactionDraft): DraftSuggestion =
            DraftSuggestion(
                accountId =
                    draft.lastDigits?.let { digits ->
                        accounts
                            .observeActive()
                            .first()
                            .firstOrNull { it.lastDigits == digits && it.type != AccountType.LOAN }
                            ?.id
                    },
                categoryId =
                    draft.merchant?.let { merchant ->
                        transactions
                            .suggestNotes(
                                draft.type,
                                merchant,
                            ).firstOrNull { it.note.equals(merchant, ignoreCase = true) }
                            ?.categoryId
                    },
            )
    }

/** REQ-COR-05 «Aplicar»: the draft as it came, when [SuggestDraft] found both its account and its category. */
class ApplyDraft
    @Inject
    constructor(
        private val drafts: DraftRepository,
        private val suggest: SuggestDraft,
        private val validator: TransactionValidator,
        private val scheduler: SyncScheduler,
    ) {
        /** False when something is missing: the caller opens Revisar transacción instead. */
        suspend operator fun invoke(draftId: UUID): Boolean {
            val draft = drafts.get(draftId) ?: return false
            val transaction = draft.toTransaction(suggest(draft))?.normalized() ?: return false
            if (validator.check(transaction).isNotEmpty()) return false
            drafts.apply(draftId, transaction)
            scheduler.requestSync()
            return true
        }
    }

/** REQ-COR-05 «Descartar», and the end of Revisar transacción once the form saved its movement. */
class DiscardDraft
    @Inject
    constructor(
        private val drafts: DraftRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(draftId: UUID) {
            drafts.discard(draftId)
            scheduler.requestSync()
        }
    }
