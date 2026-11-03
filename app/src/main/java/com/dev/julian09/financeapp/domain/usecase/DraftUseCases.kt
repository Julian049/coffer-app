package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionDraft
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.model.accountFor
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.DraftRepository
import com.dev.julian09.financeapp.domain.port.MailRuleRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/** What [SuggestDraft] found for a draft; any may be missing. A transfer's [destinationId] is its income half's account. */
data class DraftSuggestion(
    val accountId: UUID?,
    val categoryId: UUID?,
    val destinationId: UUID? = null,
)

/** The movement a draft becomes as it came, or null while an account (or an expense's or income's category) is unknown. */
fun TransactionDraft.toTransaction(s: DraftSuggestion): Transaction? {
    val account = s.accountId ?: return null
    if (type == TransactionType.TRANSFER) {
        val destination = s.destinationId ?: return null
        return Transaction(UUID.randomUUID(), type, amount, occurredAt, account, destinationAccountId = destination)
    }
    val category = s.categoryId ?: return null
    return Transaction(UUID.randomUUID(), type, amount, occurredAt, account, categoryId = category, note = merchant)
}

/**
 * The account of the user's mail rule or of the matching last 4 digits ([accountFor], REQ-COR-06, REQ-COR-11) and the
 * category last used with the same merchant (REQ-COR-08); a transfer (REQ-COR-12) gets both accounts and no category.
 */
class SuggestDraft
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val transactions: TransactionRepository,
        private val rules: MailRuleRepository,
    ) {
        suspend operator fun invoke(draft: TransactionDraft): DraftSuggestion {
            val active = accounts.observeActive().first()
            val ruled = rules.observeAll().first()
            return DraftSuggestion(
                accountId = accountFor(draft.source, draft.lastDigits, active, ruled),
                destinationId = draft.destinationSource?.let { accountFor(it, draft.destinationDigits, active, ruled) },
                categoryId =
                    draft.merchant?.takeIf { draft.type != TransactionType.TRANSFER }?.let { merchant ->
                        transactions
                            .suggestNotes(
                                draft.type,
                                merchant,
                            ).firstOrNull { it.note.equals(merchant, ignoreCase = true) }
                            ?.categoryId
                    },
            )
        }
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
