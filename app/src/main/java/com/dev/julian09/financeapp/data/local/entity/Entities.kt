package com.dev.julian09.financeapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Upload state of a local row: the outbox is the table itself (ADR-0006). */
enum class SyncState { PENDING, SYNCED, FAILED }

/*
 * Mirrors of the server tables (data-model §5.1) without user_id or version, plus the local syncState/syncError.
 * Amounts are Long ×10⁴ (ADR-0004); instants are epoch milliseconds; UUIDs are text.
 */

@Entity(tableName = "settings", indices = [Index("syncState")])
data class SettingsEntity(
    @PrimaryKey val id: UUID,
    val reminderLeadDays: Int,
    val notificationsEnabled: Boolean,
    val homeBalanceView: String,
    val amountDecimals: Int,
    /** JSON array of {key, visible}. */
    val homeCards: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDING,
    val syncError: String? = null,
    /** PERIOD or CUSTOM (V13, Room 7; V14, Room 8 renames TOTAL_DEBT to CUSTOM). */
    @ColumnInfo(defaultValue = "PERIOD")
    val netBalanceMode: String = "PERIOD",
    /** The expense category that receives the principal of loan installments (V14, Room 8); null until the first one. */
    @ColumnInfo(defaultValue = "NULL")
    val loanPaymentCategoryId: UUID? = null,
)

@Entity(tableName = "accounts", indices = [Index("syncState")])
data class AccountEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    val type: String,
    val initialBalance: Long,
    val includeInTotal: Boolean = true,
    val active: Boolean = true,
    val deactivatedAt: Instant? = null,
    val institution: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDING,
    val syncError: String? = null,
    /** Whether the debt is subtracted in the custom net balance (V14, Room 8). */
    @ColumnInfo(defaultValue = "1")
    val includeInNet: Boolean = true,
)

@Entity(tableName = "categories", indices = [Index("syncState"), Index("parentId")])
data class CategoryEntity(
    @PrimaryKey val id: UUID,
    val parentId: UUID? = null,
    val name: String,
    val kind: String,
    @ColumnInfo(name = "isBase") val isBase: Boolean = false,
    val systemKey: String? = null,
    val icon: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDING,
    val syncError: String? = null,
    /** V11 / Room 5 (REQ-CAT-04). */
    @ColumnInfo(defaultValue = "0") val excludeFromReports: Boolean = false,
)

@Entity(tableName = "tags", indices = [Index("syncState")])
data class TagEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    val pinned: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDING,
    val syncError: String? = null,
    /** `#RRGGBB` from the 6-color palette (V15, Room 9); null shows the first one. */
    @ColumnInfo(defaultValue = "NULL") val color: String? = null,
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["occurredAt"], orders = [Index.Order.DESC]),
        Index(value = ["accountId", "occurredAt"]),
        Index("categoryId"),
        Index("tagId"),
        Index("syncState"),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: UUID,
    val type: String,
    val amount: Long,
    val occurredAt: Instant,
    val accountId: UUID,
    val destinationAccountId: UUID? = null,
    val categoryId: UUID? = null,
    val tagId: UUID? = null,
    val note: String? = null,
    val installments: Int = 1,
    val principal: Long? = null,
    val interest: Long? = null,
    val fees: Long? = null,
    val installmentId: UUID? = null,
    val recurringId: UUID? = null,
    val paymentKind: String? = null,
    /** A transfer's note, apart from its title [note] (V13, Room 7). */
    val concept: String? = null,
    /** Every tag as `,id1,id2,` or empty (ADR-0015, Room 9); [tagId] is the pre-2.0 column, kept only so old rows migrate. */
    @ColumnInfo(defaultValue = "") val tagIds: String = "",
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDING,
    val syncError: String? = null,
)

/** A scheduled expense (V7); the domain computes its dates (RecurrenceCalculator). */
@Entity(tableName = "recurring_expenses", indices = [Index("syncState"), Index("nextDueDate")])
data class RecurringExpenseEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    val estimatedAmount: Long,
    val lastPaidAmount: Long? = null,
    val accountId: UUID,
    val categoryId: UUID,
    val startDate: LocalDate,
    /** WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, YEARLY or CUSTOM. */
    val frequency: String,
    val intervalCount: Int? = null,
    /** DAY, WEEK or MONTH, only with CUSTOM. */
    val intervalUnit: String? = null,
    val nextDueDate: LocalDate,
    val lastNotifiedOn: LocalDate? = null,
    val active: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDING,
    val syncError: String? = null,
)

/** Terms of a CREDIT_CARD account (V8); [id] is the account's id. [rate] is the decimal fraction as text ("0.020000"). */
@Entity(tableName = "credit_card_details", indices = [Index("syncState")])
data class CreditCardDetailsEntity(
    @PrimaryKey val id: UUID,
    val creditLimit: Long,
    val cutOffDay: Int,
    val paymentDay: Int,
    val rate: String,
    /** EA or MV. */
    val rateType: String,
    val firstInstallmentInterestFree: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDING,
    val syncError: String? = null,
)

/** Terms of a LOAN account (V8, V12); [id] is the account's id. Detailed loans fill the plan fields, simple ones [monthlyPayment]. */
@Entity(tableName = "loan_details", indices = [Index("syncState")])
data class LoanDetailsEntity(
    @PrimaryKey val id: UUID,
    /** DETAILED or SIMPLE. */
    val mode: String,
    val disbursementAccountId: UUID? = null,
    val principal: Long? = null,
    val installmentsCount: Int? = null,
    val rate: String? = null,
    val rateType: String? = null,
    val firstDueDate: LocalDate? = null,
    val paymentDay: Int,
    val monthlyPayment: Long? = null,
    val lifeInsuranceMonthly: Long = 0,
    val otherCostsMonthly: Long = 0,
    /** FIXED_PAYMENT or FIXED_PRINCIPAL (V12). */
    @ColumnInfo(defaultValue = "FIXED_PRINCIPAL") val amortization: String = "FIXED_PRINCIPAL",
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDING,
    val syncError: String? = null,
)

/** A card purchase in 2 or more installments (V8); the plan is in installments. */
@Entity(tableName = "deferred_purchases", indices = [Index("syncState"), Index("cardAccountId"), Index("purchaseTransactionId")])
data class DeferredPurchaseEntity(
    @PrimaryKey val id: UUID,
    val cardAccountId: UUID,
    val purchaseTransactionId: UUID,
    val amount: Long,
    val installmentsCount: Int,
    val rate: String,
    val rateType: String,
    val firstInstallmentInterestFree: Boolean,
    val firstDueDate: LocalDate,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDING,
    val syncError: String? = null,
)

/** One installment of a deferred purchase or, from S10, of a loan (exactly one owner, V8 ck_inst_owner). */
@Entity(tableName = "installments", indices = [Index("syncState"), Index("deferredPurchaseId"), Index("dueDate")])
data class InstallmentEntity(
    @PrimaryKey val id: UUID,
    val deferredPurchaseId: UUID? = null,
    val loanAccountId: UUID? = null,
    val number: Int,
    val dueDate: LocalDate,
    val principal: Long,
    val interest: Long = 0,
    val insurance: Long = 0,
    val otherCosts: Long = 0,
    /** SCHEDULED, PAID or CANCELLED. */
    val status: String = "SCHEDULED",
    val paidAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDING,
    val syncError: String? = null,
)

/** History of a transaction (REQ-HIS-01): before and after are its synced data as JSON, null when it did not exist. */
@Entity(tableName = "change_log", indices = [Index("syncState"), Index("transactionId")])
data class ChangeLogEntity(
    @PrimaryKey val id: UUID,
    val transactionId: UUID,
    /** CREATE, UPDATE, DELETE or RESTORE (S11). */
    val action: String,
    val beforeValue: String? = null,
    val afterValue: String? = null,
    val changedAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDING,
    val syncError: String? = null,
)

/** Local-only key/value: pull_cursor, last_sync_at, last_sync_result, legacy_migrated, card_notified:<id>. */
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

/** ADR-0015: the tags of a movement as stored in [TransactionEntity.tagIds], so `LIKE '%,id,%'` finds one. */
internal fun encodeTagIds(ids: List<UUID>): String = if (ids.isEmpty()) "" else ids.distinct().joinToString(",", ",", ",")

internal fun decodeTagIds(text: String): List<UUID> = text.split(',').filter { it.isNotEmpty() }.map(UUID::fromString)
