package com.dev.julian09.financeapp.domain.finance

import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionDraft
import com.dev.julian09.financeapp.domain.model.TransactionType
import java.text.Normalizer
import java.util.UUID

/** How well the app knows a draft (roadmap-v2.4 §3). Internal: the screens show it only as the circle of the row. */
enum class DraftLevel { UNKNOWN, PROBABLE, SURE }

/** What past decisions say about a draft: its [level], and the title and category the user gave last time. */
data class Learned(
    val level: DraftLevel,
    val note: String? = null,
    val categoryId: UUID? = null,
    /** How many earlier movements the answer rests on, for «Según tus N últimos…». */
    val basedOn: Int = 0,
)

/** How many equal results in a row make a draft sure. */
const val SURE_AFTER = 3

private val ACCENTS = Regex("\\p{InCombiningDiacriticalMarks}+")

/** A merchant without case, accents or extra spaces, so «Éxito  Calle 80» and «exito calle 80» are the same one. */
fun normalizeMerchant(merchant: String): String =
    Normalizer.normalize(merchant, Normalizer.Form.NFD).replace(ACCENTS, "").lowercase().trim().replace(Regex("\\s+"), " ")

/**
 * ADR-0010: learns from the movements the user already made from drafts. [history] holds the earlier movements of the same
 * account and type that came from a draft, newest first. An expense is known by its merchant; an income carries no merchant in
 * the PO's use, so account and type are its key (2.4-05) and a blank title stays blank. Never decides alone: whoever applies
 * a [DraftLevel.SURE] draft still excludes transfers, unknown accounts and amounts the history never had.
 */
fun learn(
    draft: TransactionDraft,
    history: List<Transaction>,
): Learned {
    if (draft.type == TransactionType.TRANSFER) return Learned(DraftLevel.UNKNOWN)
    val key = draft.merchant?.let(::normalizeMerchant)?.takeIf { it.isNotEmpty() }
    val matches =
        history.filter { t ->
            t.type == draft.type &&
                if (draft.type == TransactionType.INCOME) true else key != null && t.sourceMerchant?.let(::normalizeMerchant) == key
        }
    val latest = matches.firstOrNull() ?: return Learned(DraftLevel.UNKNOWN)
    val outcomes = matches.take(SURE_AFTER).map { it.note?.trim().orEmpty() to it.categoryId }
    val same = matches.size >= SURE_AFTER && outcomes.distinct().size == 1
    val sameAmount = matches.take(SURE_AFTER).all { it.amount == draft.amount }
    return Learned(
        level = if (same && sameAmount) DraftLevel.SURE else DraftLevel.PROBABLE,
        note = latest.note?.trim()?.ifEmpty { null },
        categoryId = latest.categoryId,
        basedOn = matches.take(SURE_AFTER).size,
    )
}

