package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.finance.learnRule
import com.dev.julian09.financeapp.domain.finance.matchRule
import com.dev.julian09.financeapp.domain.finance.normalizeMerchant
import com.dev.julian09.financeapp.domain.finance.outcome
import com.dev.julian09.financeapp.domain.finance.placeRule
import com.dev.julian09.financeapp.domain.model.DraftRule
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RuleLevel
import com.dev.julian09.financeapp.domain.model.RuleOrigin
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.DraftRepository
import com.dev.julian09.financeapp.domain.port.DraftRuleRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/** An expense that came by email from a draft, not the payment of a recurring expense: what the rules learn from. */
private fun Transaction.ruleable() = type == TransactionType.EXPENSE && sourceMerchant != null && draftId != null && recurringId == null

/**
 * 2.5 «Registro automático»: after a movement made from a draft is saved (Aplicar, registered alone, Revisar transacción or
 * its edit) the app learns a rule from the merchant's last movements; a wrong learned rule is turned off ([forget]).
 */
class LearnRules
    @Inject
    constructor(
        private val transactions: TransactionRepository,
        private val rules: DraftRuleRepository,
        private val clock: Clock,
    ) {
        /** [original] is the stored movement when [saved] is an edit: a new title or category means its rule was wrong. */
        suspend operator fun invoke(
            saved: Transaction,
            original: Transaction? = null,
        ) {
            if (original != null && original.outcome() != saved.outcome()) forget(original)
            learn(saved)
        }

        /** Deshacer or a correction: the learned rule that decided [t] as it was is turned off; the user's rules never are. */
        suspend fun forget(t: Transaction) {
            if (!t.ruleable()) return
            val rule = matchRule(t.sourceMerchant, t.amount, t.accountId, rules.observeAll().first()) ?: return
            if (rule.origin == RuleOrigin.LEARNED && (rule.note to rule.categoryId) == t.outcome()) {
                rules.saveAll(listOf(rule.copy(enabled = false, learnedSince = clock.now())))
            }
        }

        private suspend fun learn(saved: Transaction) {
            if (!saved.ruleable()) return
            val merchant = saved.sourceMerchant!!
            val key = normalizeMerchant(merchant).ifEmpty { return }
            // shortcut: only the latest 500 email expenses are read; enough for one person's mail
            val history =
                transactions.fromMail(TransactionType.EXPENSE).filter {
                    it.recurringId == null && normalizeMerchant(it.sourceMerchant!!) == key
                }
            val all = rules.observeAll().first()
            val mine = all.filter { it.merchantKey == key }
            val disabled = mine.filterNot { it.enabled }.associateBy { it.id }
            // A rule for any amount makes one for an exact amount redundant
            val learned =
                learnRule(key, history, null, disabled)
                    ?: (if (mine.none { it.enabled && it.amount == null }) learnRule(key, history, saved.amount, disabled) else null)
                    ?: return
            if (mine.any {
                    it.enabled && it.origin == RuleOrigin.USER && it.amount == learned.amount && it.accountId == learned.accountId
                }
            ) {
                return
            }
            val existing = all.firstOrNull { it.id == learned.id }
            if (existing == null) {
                val rule =
                    DraftRule(
                        learned.id,
                        merchant,
                        key,
                        learned.amount,
                        learned.accountId,
                        learned.note,
                        learned.categoryId,
                        learned.level,
                        RuleOrigin.LEARNED,
                    )
                rules.saveAll(placeRule(all, rule))
                return
            }
            if (existing.origin == RuleOrigin.USER) return
            // A sure rule is never demoted because the last two say so; only Deshacer or a correction turns it off
            if (existing.enabled && existing.level == RuleLevel.SURE && learned.level == RuleLevel.PROBABLE) return
            val next =
                existing.copy(
                    merchant = merchant,
                    note = learned.note,
                    categoryId = learned.categoryId,
                    level = learned.level,
                    enabled = true,
                )
            if (next != existing) rules.saveAll(listOf(next))
        }
    }

/** What the rule form sends; [id] null = a new rule. */
data class DraftRuleInput(
    val id: UUID? = null,
    val merchant: String,
    val amount: Money? = null,
    val accountId: UUID? = null,
    val note: String,
    val categoryId: UUID,
)

sealed interface SaveRuleResult {
    data class Saved(
        val rule: DraftRule,
    ) : SaveRuleResult

    /** Another enabled rule already has the same merchant, amount and account. */
    data class Duplicate(
        val existing: DraftRule,
    ) : SaveRuleResult

    data object Invalid : SaveRuleResult
}

/** Nueva regla and its edit: whatever the user saves is theirs and sure (PO, 2.5). */
class SaveDraftRule
    @Inject
    constructor(
        private val rules: DraftRuleRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(input: DraftRuleInput): SaveRuleResult {
            val merchant = input.merchant.trim()
            val note = input.note.trim()
            val key = normalizeMerchant(merchant)
            if (key.isEmpty() || note.isEmpty()) return SaveRuleResult.Invalid
            val all = rules.observeAll().first()
            all
                .firstOrNull {
                    it.enabled && it.id != input.id && it.merchantKey == key && it.amount == input.amount && it.accountId == input.accountId
                }?.let { return SaveRuleResult.Duplicate(it) }
            val old = input.id?.let { id -> all.firstOrNull { it.id == id } ?: return SaveRuleResult.Invalid }
            val rule =
                (
                    old
                        ?: DraftRule(
                            UUID.randomUUID(),
                            merchant,
                            key,
                            note = note,
                            categoryId = input.categoryId,
                            level = RuleLevel.SURE,
                            origin = RuleOrigin.USER,
                        )
                ).copy(
                    merchant = merchant,
                    merchantKey = key,
                    amount = input.amount,
                    accountId = input.accountId,
                    note = note,
                    categoryId = input.categoryId,
                    level = RuleLevel.SURE,
                    origin = RuleOrigin.USER,
                    enabled = true,
                )
            // A new rule, or a probable one that becomes sure, takes its place among the sure ones; an edit keeps its place
            val rows = if (old == null || old.level != RuleLevel.SURE) placeRule(all, rule) else listOf(rule)
            rules.saveAll(rows)
            scheduler.requestSync()
            return SaveRuleResult.Saved(rows.first { it.id == rule.id })
        }
    }

/** «Eliminar regla»: a learned one is only turned off, so the same movements do not teach it again. */
class DeleteDraftRule
    @Inject
    constructor(
        private val rules: DraftRuleRepository,
        private val clock: Clock,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(id: UUID) {
            val rule = rules.get(id) ?: return
            if (rule.origin == RuleOrigin.USER) {
                rules.delete(id)
            } else {
                rules.saveAll(listOf(rule.copy(enabled = false, learnedSince = clock.now())))
            }
            scheduler.requestSync()
        }
    }

/** The sure rules dragged into [orderedIds]' order; probable ones (and turned off ones) stay after them. */
class ReorderDraftRules
    @Inject
    constructor(
        private val rules: DraftRuleRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(orderedIds: List<UUID>) {
            val all = rules.observeAll().first()
            val sure = all.filter { it.enabled && it.level == RuleLevel.SURE }
            val ordered =
                orderedIds.mapNotNull { id -> sure.firstOrNull { it.id == id } } +
                    sure.filter { it.id !in orderedIds } +
                    all.filter { it.enabled && it.level == RuleLevel.PROBABLE } +
                    all.filterNot { it.enabled }
            val changed = ordered.mapIndexedNotNull { i, r -> r.copy(position = i).takeIf { it != r } }
            if (changed.isEmpty()) return
            rules.saveAll(changed)
            scheduler.requestSync()
        }
    }

/** The enabled rules for Registro automático, each group in list order. */
data class DraftRuleList(
    val sure: List<DraftRule> = emptyList(),
    val probable: List<DraftRule> = emptyList(),
)

class ObserveDraftRules
    @Inject
    constructor(
        private val rules: DraftRuleRepository,
    ) {
        operator fun invoke(): Flow<DraftRuleList> =
            rules.observeAll().map { all ->
                val (sure, probable) = all.filter { it.enabled }.partition { it.level == RuleLevel.SURE }
                DraftRuleList(sure, probable)
            }
    }

/** The merchants the rule form offers: as they came in the emails, newest first. */
class MerchantSuggestions
    @Inject
    constructor(
        private val transactions: TransactionRepository,
        private val drafts: DraftRepository,
        private val rules: DraftRuleRepository,
    ) {
        /** Recent merchants of email expenses and pending drafts that have no enabled rule yet. */
        suspend operator fun invoke(limit: Int = 8): List<String> {
            val ruled =
                rules
                    .observeAll()
                    .first()
                    .filter { it.enabled }
                    .map { it.merchantKey }
                    .toSet()
            return seen()
                .distinctBy(::normalizeMerchant)
                .filter { normalizeMerchant(it).let { key -> key.isNotEmpty() && key !in ruled } }
                .take(limit)
        }

        /** «En tus correos aparece como …»: every spelling seen for [merchantKey]. */
        suspend fun merchantAliases(merchantKey: String): List<String> = seen().filter { normalizeMerchant(it) == merchantKey }.distinct()

        private suspend fun seen(): List<String> {
            val pending =
                drafts
                    .observeAll()
                    .first()
                    .filter { it.type == TransactionType.EXPENSE }
                    .map { it.occurredAt to it.merchant }
            val saved = transactions.fromMail(TransactionType.EXPENSE).map { it.occurredAt to it.sourceMerchant }
            return (pending + saved).sortedByDescending { it.first }.mapNotNull { it.second?.trim()?.ifEmpty { null } }
        }
    }
