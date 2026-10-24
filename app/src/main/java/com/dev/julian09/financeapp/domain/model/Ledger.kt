package com.dev.julian09.financeapp.domain.model

import com.dev.julian09.financeapp.domain.finance.BalanceCalculator
import java.time.Instant
import java.util.UUID

enum class AccountType {
    CASH,
    SAVINGS,
    CREDIT_CARD,
    LOAN,
    ;

    /** Credit cards and loans hold a debt: their balance shows negative (REQ-TC-04). */
    val isDebt: Boolean get() = this == CREDIT_CARD || this == LOAN

    /** S15-DOM02: loans are only seen in Gestión de cuentas; movements, transfers and Home never offer one. */
    val isPickable: Boolean get() = this != LOAN
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
    /** S15-H07: a card or loan whose debt the custom net balance subtracts. */
    val includeInNet: Boolean = true,
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
    /** REQ-CAT-04: set on parents; subcategories follow their parent ([excludedFromReports]). */
    val excludeFromReports: Boolean = false,
)

/** REQ-CAT-04: a subcategory follows its parent; a parent (or an orphan) its own flag. */
fun Category.excludedFromReports(byId: Map<UUID, Category>): Boolean = parentId?.let { byId[it]?.excludeFromReports } ?: excludeFromReports

data class Tag(
    val id: UUID,
    val name: String,
    val pinned: Boolean = false,
)

data class TagTotal(
    val tagId: UUID,
    val income: Money,
    val expense: Money,
    val count: Int,
) {
    val net: Money get() = income - expense
}

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
    /** A transfer's note or concept, apart from its title [note] (S12-H01). */
    val concept: String? = null,
    /** When it was registered; null until stored. */
    val createdAt: Instant? = null,
    /** Not yet in the cloud (shown in the detail). */
    val pendingSync: Boolean = false,
)

/** M5: a note to reuse in the form, with the category it had last time. */
data class NoteSuggestion(
    val note: String,
    val categoryId: UUID?,
    /** Where it was paid last time, shown next to the category (S12-UI03). */
    val accountId: UUID? = null,
)

/** A card purchase in installments or a payment is not edited: it is deleted and registered again (S7). */
fun Transaction.isEditable() = installments <= 1 && paymentKind == null

/** change_log.action (REQ-HIS-01). */
enum class ChangeAction { CREATE, UPDATE, DELETE, RESTORE }

/** One entry of Historial de cambios: the movement before and after, null when it did not exist. */
data class ChangeEntry(
    val id: UUID,
    val transactionId: UUID,
    val action: ChangeAction,
    val before: Transaction?,
    val after: Transaction?,
    val changedAt: Instant,
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
    val netBalanceMode: NetBalanceMode = NetBalanceMode.PERIOD,
    /** S15-H06: where the principal of loan installments counts as an expense; null until the first payment asks. */
    val loanPaymentCategoryId: UUID? = null,
)

/** REQ-CTA-08 (S12-H03): what the net balance subtracts from the available one. */
enum class NetBalanceMode {
    /** What falls due this period: each card's statement and each loan's next installment. */
    PERIOD,

    /** The debts of cards and loans the user marks (Account.includeInNet), as if paid today. */
    CUSTOM,
}

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

/** History filters (REQ-TRX-09, 10): empty sets mean "any"; [query] matches note, category, account and tag. */
data class TransactionFilter(
    val from: Instant,
    val to: Instant,
    val types: Set<TransactionType> = emptySet(),
    val categoryIds: Set<UUID> = emptySet(),
    val accountIds: Set<UUID> = emptySet(),
    val tagIds: Set<UUID> = emptySet(),
    val query: String = "",
)

data class SearchTotals(
    val income: Money = Money.ZERO,
    val expense: Money = Money.ZERO,
    val count: Int = 0,
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
    AMOUNT_REQUIRED,
    ACCOUNT_REQUIRED,
    CATEGORY_REQUIRED,
    SAME_ACCOUNT,
    NOTE_TOO_LONG,
    INTERVAL_INVALID,
    DAY_INVALID,
    RATE_INVALID,
    OVER_LIMIT,
    INSTALLMENTS_INVALID,
    OVER_DEBT,
}
