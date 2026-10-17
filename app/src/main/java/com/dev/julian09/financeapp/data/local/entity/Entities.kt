package com.dev.julian09.financeapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
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
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDING,
    val syncError: String? = null,
)

/** Local-only key/value: pull_cursor, last_sync_at, last_sync_result, legacy_migrated. */
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)
