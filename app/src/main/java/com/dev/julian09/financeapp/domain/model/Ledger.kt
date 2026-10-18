package com.dev.julian09.financeapp.domain.model

import com.dev.julian09.financeapp.domain.finance.BalanceCalculator
import java.time.Instant
import java.util.UUID

enum class AccountType {
    CASH,
    DEBIT,
    SAVINGS,
    CREDIT_CARD,
    LOAN,
    ;

    /** Credit cards and loans hold a debt: their balance shows negative (REQ-TC-04). */
    val isDebt: Boolean get() = this == CREDIT_CARD || this == LOAN
}

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
    /** Null until stored. */
    val createdAt: Instant? = null,
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

/** Sums of what moved through an account, all positive (detailed-design §6.9.5). */
data class AccountFlows(
    val income: Money = Money.ZERO,
    val expense: Money = Money.ZERO,
    val transfersOut: Money = Money.ZERO,
    val transfersIn: Money = Money.ZERO,
)

data class AccountBalance(
    val account: Account,
    val flows: AccountFlows,
    /** Non-deleted movements where the account is the source or the destination. */
    val transactions: Int = 0,
) {
    val balance: Money get() = BalanceCalculator.balance(account, flows)
}

/** What removing an account touches (SEQ-12, REQ-CTA-05). */
data class AccountImpact(
    val transactions: Int,
    val balance: Money,
    val firstAt: Instant?,
    val lastAt: Instant?,
)

enum class RemoveMode { DEACTIVATE, MOVE, DELETE_ALL }

data class PeriodTotals(
    val income: Money = Money.ZERO,
    val expense: Money = Money.ZERO,
)

/** Expenses or income of one category in a period, without its subcategories. */
data class CategoryTotal(
    val categoryId: UUID,
    val total: Money,
    val count: Int,
)

/** Why a ledger write was refused; the screens turn it into a message. */
enum class LedgerError {
    NAME_REQUIRED,
    NAME_TOO_LONG,
    NAME_TAKEN,
    TYPE_NOT_AVAILABLE,
    TARGET_REQUIRED,
    TARGET_INVALID,
    CONFIRMATION_MISMATCH,
    ONE_LEVEL_ONLY,
    KIND_MISMATCH,
    SYSTEM_CATEGORY_IN_USE,
}
