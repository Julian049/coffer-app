package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.finance.DraftLevel
import com.dev.julian09.financeapp.domain.finance.Learned
import com.dev.julian09.financeapp.domain.finance.learn
import com.dev.julian09.financeapp.domain.finance.normalizeMerchant
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionDraft
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.model.accountFor
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.DraftPrefs
import com.dev.julian09.financeapp.domain.port.DraftRepository
import com.dev.julian09.financeapp.domain.port.MailRuleRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * What [SuggestDraft] found for a draft; any may be missing. A transfer's [destinationId] is its income half's account.
 * [note] is the title the user gave last time (null: keep the email's merchant); [level] is how sure the app is (2.4).
 */
data class DraftSuggestion(
    val accountId: UUID?,
    val categoryId: UUID?,
    val destinationId: UUID? = null,
    val level: DraftLevel = DraftLevel.UNKNOWN,
    val note: String? = null,
    /** True when the history spoke, even to say «no title» (an income): [note] then replaces the email's merchant. */
    val titleLearned: Boolean = false,
    val basedOn: Int = 0,
)

/** The key `DraftPrefs.resetLearning` uses: one account and one merchant. */
fun learningKey(
    accountId: UUID,
    merchant: String?,
) = "$accountId|${merchant?.let(::normalizeMerchant).orEmpty()}"

/** The movement a draft becomes as it came, or null while an account (or an expense's or income's category) is unknown. */
fun TransactionDraft.toTransaction(s: DraftSuggestion): Transaction? {
    val account = s.accountId ?: return null
    if (type == TransactionType.TRANSFER) {
        val destination = s.destinationId ?: return null
        return Transaction(UUID.randomUUID(), type, amount, occurredAt, account, destinationAccountId = destination, draftId = id)
    }
    val category = s.categoryId ?: return null
    return Transaction(
        UUID.randomUUID(),
        type,
        amount,
        occurredAt,
        account,
        categoryId = category,
        note = if (s.titleLearned) s.note else merchant,
        sourceMerchant = merchant,
        draftId = id,
    )
}

/**
 * The account of the user's mail rule or of the matching last 4 digits ([accountFor], REQ-COR-06, REQ-COR-11) and what the
 * user decided the last times for the same merchant ([learn], 2.4); a transfer (REQ-COR-12) gets both accounts and no category.
 * Drafts from before 2.4 left no merchant on their movement, so the category last used with the same note still answers.
 */
class SuggestDraft
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val transactions: TransactionRepository,
        private val rules: MailRuleRepository,
        private val prefs: DraftPrefs,
    ) {
        suspend operator fun invoke(draft: TransactionDraft): DraftSuggestion {
            val active = accounts.observeActive().first()
            val ruled = rules.observeAll().first()
            val account = accountFor(draft.source, draft.lastDigits, active, ruled)
            val destination = draft.destinationSource?.let { accountFor(it, draft.destinationDigits, active, ruled) }
            if (draft.type == TransactionType.TRANSFER) return DraftSuggestion(account, null, destination)
            val reset = account?.let { prefs.observeResets().first()[learningKey(it, draft.merchant)] }
            val learned =
                account
                    ?.let { transactions.fromDrafts(it, draft.type) }
                    ?.filter { reset == null || (it.createdAt ?: Instant.EPOCH) > reset }
                    ?.let { learn(draft, it) }
                    ?: Learned(DraftLevel.UNKNOWN)
            val category =
                learned.categoryId
                    ?: draft.merchant?.let { merchant ->
                        transactions
                            .suggestNotes(
                                draft.type,
                                merchant,
                            ).firstOrNull { it.note.equals(merchant, ignoreCase = true) }
                            ?.categoryId
                    }
            return DraftSuggestion(
                accountId = account,
                categoryId = category,
                destinationId = destination,
                level = learned.level,
                note = learned.note,
                titleLearned = learned.level != DraftLevel.UNKNOWN,
                basedOn = learned.basedOn,
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

/**
 * 2.4-04 «Guardar solos los repetidos»: saves on its own the drafts that are [DraftLevel.SURE] (same merchant and amount, the
 * last 3 times ended the same). Never a transfer between own accounts, a draft whose account is unknown or an amount the history
 * never had ([learn] needs it equal); cards and installments may. Each is told to the user once, in the registered notice.
 */
class AutoApplyDrafts
    @Inject
    constructor(
        private val drafts: DraftRepository,
        private val suggest: SuggestDraft,
        private val validator: TransactionValidator,
        private val prefs: DraftPrefs,
        private val scheduler: SyncScheduler,
    ) {
        /** How many movements were saved. */
        suspend operator fun invoke(): Int {
            if (!prefs.observeAutoSave().first()) return 0
            var saved = 0
            for (draft in drafts.observeAll().first()) {
                if (draft.type == TransactionType.TRANSFER) continue
                val suggestion = suggest(draft)
                if (suggestion.level != DraftLevel.SURE) continue
                val transaction = draft.toTransaction(suggestion)?.normalized() ?: continue
                if (validator.check(transaction).isNotEmpty()) continue
                drafts.apply(draft.id, transaction)
                prefs.addRegistered(transaction.id)
                saved++
            }
            if (saved > 0) scheduler.requestSync()
            return saved
        }
    }

/** 2.4 Deshacer: a movement that came by email goes back to «Por revisar» (all the parts of a split, the draft and its pair). */
class UndoDraft
    @Inject
    constructor(
        private val transactions: TransactionRepository,
        private val drafts: DraftRepository,
        private val prefs: DraftPrefs,
        private val clock: Clock,
        private val scheduler: SyncScheduler,
    ) {
        /** False when [transaction] did not come from a draft. */
        suspend operator fun invoke(transaction: Transaction): Boolean {
            val draftId = transaction.draftId ?: return false
            val parts = transactions.byDraft(draftId).ifEmpty { listOf(transaction) }
            parts.forEach { transactions.delete(it.id) }
            drafts.restore(draftId)
            prefs.resetLearning(learningKey(transaction.accountId, transaction.sourceMerchant), clock.now())
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
