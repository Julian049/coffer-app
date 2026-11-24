package com.dev.julian09.financeapp.domain.model

import java.time.Instant
import java.util.UUID

/** How much a [DraftRule] is trusted: SURE registers the email by itself, PROBABLE only suggests (2.5). */
enum class RuleLevel { SURE, PROBABLE }

/** Who made a [DraftRule]: the user by hand, or the app by learning from the user's decisions. */
enum class RuleOrigin { USER, LEARNED }

/** A merchant (and maybe an exact amount and account) that gets a title and a category; synced as `draftRule`. */
data class DraftRule(
    val id: UUID,
    val merchant: String,
    val merchantKey: String,
    val amount: Money? = null,
    val accountId: UUID? = null,
    val note: String,
    val categoryId: UUID,
    val level: RuleLevel,
    val origin: RuleOrigin,
    /** False = a learned rule the user «deleted»: hidden, kept so it is not learned again. */
    val enabled: Boolean = true,
    val learnedSince: Instant? = null,
    /** List order, lower first. */
    val position: Int = 0,
    val updatedAt: Instant? = null,
)
