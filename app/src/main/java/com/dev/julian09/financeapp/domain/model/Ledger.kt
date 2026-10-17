package com.dev.julian09.financeapp.domain.model

import java.time.Instant
import java.util.UUID

enum class AccountType { CASH, DEBIT, SAVINGS, CREDIT_CARD, LOAN }

data class Account(
    val id: UUID,
    val name: String,
    val type: AccountType,
    val initialBalance: Money,
    val includeInTotal: Boolean = true,
    val active: Boolean = true,
    val deactivatedAt: Instant? = null,
    val institution: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val sortOrder: Int = 0,
)

enum class CategoryKind { EXPENSE, INCOME }

data class Category(
    val id: UUID,
    val name: String,
    val kind: CategoryKind,
    val parentId: UUID? = null,
    val isBase: Boolean = false,
    /** Categories with logic carry a stable key: FINANCIAL_CHARGES (REQ-TC-09). */
    val systemKey: String? = null,
    val icon: String? = null,
    val sortOrder: Int = 0,
)

data class Tag(
    val id: UUID,
    val name: String,
    val pinned: Boolean = false,
)

enum class TransactionType { EXPENSE, INCOME, TRANSFER }

/** Amounts are always positive; [type] gives the direction (V6, ADR-0004). */
data class Transaction(
    val id: UUID,
    val type: TransactionType,
    val amount: Money,
    val occurredAt: Instant,
    val accountId: UUID,
    val destinationAccountId: UUID? = null,
    val categoryId: UUID? = null,
    val tagId: UUID? = null,
    val note: String? = null,
    val installments: Int = 1,
    val principal: Money? = null,
    val interest: Money? = null,
    val fees: Money? = null,
    val installmentId: UUID? = null,
    val recurringId: UUID? = null,
    val paymentKind: String? = null,
)

data class HomeCard(
    val key: String,
    val visible: Boolean,
)

/** One per user (user_settings). */
data class Settings(
    val reminderLeadDays: Int = 5,
    val notificationsEnabled: Boolean = true,
    val homeBalanceView: String = "AVAILABLE",
    val amountDecimals: Int = 0,
    val homeCards: List<HomeCard> = emptyList(),
)
