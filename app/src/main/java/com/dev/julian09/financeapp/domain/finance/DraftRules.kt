package com.dev.julian09.financeapp.domain.finance

import com.dev.julian09.financeapp.domain.model.DraftRule
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RuleLevel
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionDraft
import java.time.Instant
import java.util.UUID

/**
 * 2.5: the enabled rule that decides an expense of [merchant] for [amount] in [accountId] (null: unknown, so a rule with an
 * account never matches). Any SURE beats any PROBABLE; then the lowest position, then the id.
 */
fun matchRule(
    merchant: String?,
    amount: Money,
    accountId: UUID?,
    rules: List<DraftRule>,
): DraftRule? {
    val key = merchant?.let(::normalizeMerchant)?.takeIf { it.isNotEmpty() } ?: return null
    return rules
        .filter { r ->
            r.enabled &&
                r.merchantKey == key &&
                (r.amount == null || r.amount == amount) &&
                (r.accountId == null || r.accountId == accountId)
        }.minWithOrNull(compareBy<DraftRule> { it.level != RuleLevel.SURE }.thenBy { it.position }.thenBy { it.id })
}

fun matchRule(
    draft: TransactionDraft,
    accountId: UUID?,
    rules: List<DraftRule>,
) = matchRule(draft.merchant, draft.amount, accountId, rules)

/** How many of amount and account a rule sets: the more, the higher it goes in the list. */
private fun DraftRule.specificity() = listOfNotNull(amount, accountId).size

/**
 * The rows to save so [rule] (new, or taken out to be placed again) sits just above the first rule of its level that is less
 * specific, or at the end; the others keep their order (manual drags) and positions run 0..n-1.
 */
fun placeRule(
    rules: List<DraftRule>,
    rule: DraftRule,
): List<DraftRule> {
    val others = rules.filter { it.id != rule.id }
    val at =
        others
            .indexOfFirst { it.level == rule.level && it.specificity() < rule.specificity() }
            .takeIf { it >= 0 } ?: others.size
    val ordered = others.toMutableList().apply { add(at, rule) }
    return ordered.mapIndexedNotNull { i, r -> r.copy(position = i).takeIf { it != r || r.id == rule.id } }
}

/** The title and category a movement ended with, compared to learn a rule. */
fun Transaction.outcome() = note?.trim().orEmpty() to categoryId

/** What the last movements of a merchant say: a rule with these conditions, resting on [basis]. */
data class LearnedRule(
    val id: UUID,
    val amount: Money?,
    val accountId: UUID?,
    val level: RuleLevel,
    val note: String,
    val categoryId: UUID,
    val basis: List<Transaction>,
)

/** Learned rules have an id that depends on their conditions, so two phones learn the same row. */
fun learnedRuleId(
    merchantKey: String,
    amount: Money?,
    accountId: UUID?,
): UUID = UUID.nameUUIDFromBytes("$merchantKey|${amount?.scaled ?: ""}|${accountId ?: ""}".toByteArray())

/**
 * 2.5 learning over [history] (one merchant's email expenses, newest first): the last [SURE_AFTER] equal → SURE, the last 2
 * → PROBABLE; [amount] null is «any amount», else only movements of that amount count. The account is a condition only when
 * every movement it rests on shares it. A learned rule the user turned off ([disabled]) only counts movements created after it.
 */
fun learnRule(
    merchantKey: String,
    history: List<Transaction>,
    amount: Money?,
    disabled: Map<UUID, DraftRule>,
): LearnedRule? {
    val pool = if (amount == null) history else history.filter { it.amount == amount }
    val basis =
        listOf(SURE_AFTER, 2).firstNotNullOfOrNull { n ->
            pool.take(n).takeIf { it.size == n && it.map { t -> t.outcome() }.distinct().size == 1 }
        } ?: return null
    val (note, category) = basis.first().outcome()
    if (note.isEmpty() || category == null) return null
    val account = basis.map { it.accountId }.distinct().singleOrNull()
    val id = learnedRuleId(merchantKey, amount, account)
    val off = disabled[id] ?: return LearnedRule(id, amount, account, levelOf(basis), note, category, basis)
    val since = off.learnedSince ?: return null
    val created = { t: Transaction -> t.createdAt ?: Instant.EPOCH }
    if (basis.all { created(it) > since }) return LearnedRule(id, amount, account, levelOf(basis), note, category, basis)
    // Shrinks every time, so it ends
    return learnRule(merchantKey, history.filter { created(it) > since }, amount, disabled)
}

private fun levelOf(basis: List<Transaction>) = if (basis.size >= SURE_AFTER) RuleLevel.SURE else RuleLevel.PROBABLE
