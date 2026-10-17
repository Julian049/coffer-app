package com.dev.julian09.financeapp.data.sync

import com.dev.julian09.financeapp.data.local.FinanceDatabase
import com.dev.julian09.financeapp.data.local.dao.SyncInfo
import com.dev.julian09.financeapp.data.local.entity.AccountEntity
import com.dev.julian09.financeapp.data.local.entity.CategoryEntity
import com.dev.julian09.financeapp.data.local.entity.SettingsEntity
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.local.entity.TagEntity
import com.dev.julian09.financeapp.data.local.entity.TransactionEntity
import com.dev.julian09.financeapp.data.remote.dto.ChangeDto
import com.dev.julian09.financeapp.domain.model.Money
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One synced table: how its rows become changes (EntityCodec) and the DAO calls the outbox and the applier need.
 * The order of [all] is the dependency order: what others reference is sent first.
 */
class SyncTable<E>(
    val entity: String,
    val pending: suspend (Int) -> List<E>,
    val syncInfo: suspend (UUID) -> SyncInfo?,
    val upsert: suspend (E) -> Unit,
    val markSynced: suspend (UUID, Instant) -> Int,
    val markFailed: suspend (UUID, Instant, String) -> Int,
    val idOf: (E) -> UUID,
    val updatedAtOf: (E) -> Instant,
    val encode: (E) -> ChangeDto,
    /** Pulled change → row already marked SYNCED. */
    val decode: (ChangeDto) -> E,
)

@Singleton
class SyncTables
    @Inject
    constructor(
        db: FinanceDatabase,
    ) {
        val settings =
            SyncTable(
                entity = "settings",
                pending = { db.settings().pending(it) },
                syncInfo = { db.settings().syncInfo(it) },
                upsert = { db.settings().upsert(it) },
                markSynced = { id, at -> db.settings().markSynced(id, at) },
                markFailed = { id, at, e -> db.settings().markFailed(id, at, e) },
                idOf = SettingsEntity::id,
                updatedAtOf = SettingsEntity::updatedAt,
                encode = { s ->
                    envelope(
                        "settings",
                        s.id,
                        s.createdAt,
                        s.updatedAt,
                        s.deletedAt,
                        buildJsonObject {
                            put("reminderLeadDays", s.reminderLeadDays)
                            put("notificationsEnabled", s.notificationsEnabled)
                            put("homeBalanceView", s.homeBalanceView)
                            put("amountDecimals", s.amountDecimals)
                            put("homeCards", homeCards(s.homeCards))
                        },
                    )
                },
                decode = { c ->
                    val d = c.data
                    SettingsEntity(
                        id = UUID.fromString(c.id),
                        reminderLeadDays = d.int("reminderLeadDays"),
                        notificationsEnabled = d.bool("notificationsEnabled"),
                        homeBalanceView = d.text("homeBalanceView")!!,
                        amountDecimals = d.int("amountDecimals"),
                        homeCards = d["homeCards"]?.jsonArray?.toString() ?: "[]",
                        createdAt = Instant.parse(c.createdAt),
                        updatedAt = Instant.parse(c.updatedAt),
                        deletedAt = c.deletedAt?.let(Instant::parse),
                        syncState = SyncState.SYNCED,
                    )
                },
            )

        val accounts =
            SyncTable(
                entity = "account",
                pending = { db.accounts().pending(it) },
                syncInfo = { db.accounts().syncInfo(it) },
                upsert = { db.accounts().upsert(listOf(it)) },
                markSynced = { id, at -> db.accounts().markSynced(id, at) },
                markFailed = { id, at, e -> db.accounts().markFailed(id, at, e) },
                idOf = AccountEntity::id,
                updatedAtOf = AccountEntity::updatedAt,
                encode = { a ->
                    envelope(
                        "account",
                        a.id,
                        a.createdAt,
                        a.updatedAt,
                        a.deletedAt,
                        buildJsonObject {
                            put("name", a.name)
                            put("type", a.type)
                            put("initialBalance", amount(a.initialBalance))
                            put("includeInTotal", a.includeInTotal)
                            put("active", a.active)
                            put("deactivatedAt", a.deactivatedAt?.toString())
                            put("institution", a.institution)
                            put("icon", a.icon)
                            put("color", a.color)
                            put("sortOrder", a.sortOrder)
                        },
                    )
                },
                decode = { c ->
                    val d = c.data
                    AccountEntity(
                        id = UUID.fromString(c.id),
                        name = d.text("name")!!,
                        type = d.text("type")!!,
                        initialBalance = scaled(d.text("initialBalance")!!),
                        includeInTotal = d.bool("includeInTotal"),
                        active = d.bool("active"),
                        deactivatedAt = d.text("deactivatedAt")?.let(Instant::parse),
                        institution = d.text("institution"),
                        icon = d.text("icon"),
                        color = d.text("color"),
                        sortOrder = d.int("sortOrder"),
                        createdAt = Instant.parse(c.createdAt),
                        updatedAt = Instant.parse(c.updatedAt),
                        deletedAt = c.deletedAt?.let(Instant::parse),
                        syncState = SyncState.SYNCED,
                    )
                },
            )

        val categories =
            SyncTable(
                entity = "category",
                pending = { db.categories().pending(it) },
                syncInfo = { db.categories().syncInfo(it) },
                upsert = { db.categories().upsert(listOf(it)) },
                markSynced = { id, at -> db.categories().markSynced(id, at) },
                markFailed = { id, at, e -> db.categories().markFailed(id, at, e) },
                idOf = CategoryEntity::id,
                updatedAtOf = CategoryEntity::updatedAt,
                encode = { k ->
                    envelope(
                        "category",
                        k.id,
                        k.createdAt,
                        k.updatedAt,
                        k.deletedAt,
                        buildJsonObject {
                            put("parentId", k.parentId?.toString())
                            put("name", k.name)
                            put("kind", k.kind)
                            put("isBase", k.isBase)
                            put("systemKey", k.systemKey)
                            put("icon", k.icon)
                            put("sortOrder", k.sortOrder)
                        },
                    )
                },
                decode = { c ->
                    val d = c.data
                    CategoryEntity(
                        id = UUID.fromString(c.id),
                        parentId = d.text("parentId")?.let(UUID::fromString),
                        name = d.text("name")!!,
                        kind = d.text("kind")!!,
                        isBase = d.bool("isBase"),
                        systemKey = d.text("systemKey"),
                        icon = d.text("icon"),
                        sortOrder = d.int("sortOrder"),
                        createdAt = Instant.parse(c.createdAt),
                        updatedAt = Instant.parse(c.updatedAt),
                        deletedAt = c.deletedAt?.let(Instant::parse),
                        syncState = SyncState.SYNCED,
                    )
                },
            )

        val tags =
            SyncTable(
                entity = "tag",
                pending = { db.tags().pending(it) },
                syncInfo = { db.tags().syncInfo(it) },
                upsert = { db.tags().upsert(listOf(it)) },
                markSynced = { id, at -> db.tags().markSynced(id, at) },
                markFailed = { id, at, e -> db.tags().markFailed(id, at, e) },
                idOf = TagEntity::id,
                updatedAtOf = TagEntity::updatedAt,
                encode = { t ->
                    envelope(
                        "tag",
                        t.id,
                        t.createdAt,
                        t.updatedAt,
                        t.deletedAt,
                        buildJsonObject {
                            put("name", t.name)
                            put("pinned", t.pinned)
                        },
                    )
                },
                decode = { c ->
                    TagEntity(
                        id = UUID.fromString(c.id),
                        name = c.data.text("name")!!,
                        pinned = c.data.bool("pinned"),
                        createdAt = Instant.parse(c.createdAt),
                        updatedAt = Instant.parse(c.updatedAt),
                        deletedAt = c.deletedAt?.let(Instant::parse),
                        syncState = SyncState.SYNCED,
                    )
                },
            )

        val transactions =
            SyncTable(
                entity = "transaction",
                pending = { db.transactions().pending(it) },
                syncInfo = { db.transactions().syncInfo(it) },
                upsert = { db.transactions().upsert(listOf(it)) },
                markSynced = { id, at -> db.transactions().markSynced(id, at) },
                markFailed = { id, at, e -> db.transactions().markFailed(id, at, e) },
                idOf = TransactionEntity::id,
                updatedAtOf = TransactionEntity::updatedAt,
                encode = { t ->
                    envelope(
                        "transaction",
                        t.id,
                        t.createdAt,
                        t.updatedAt,
                        t.deletedAt,
                        buildJsonObject {
                            put("type", t.type)
                            put("amount", amount(t.amount))
                            put("occurredAt", t.occurredAt.toString())
                            put("accountId", t.accountId.toString())
                            put("destinationAccountId", t.destinationAccountId?.toString())
                            put("categoryId", t.categoryId?.toString())
                            put("tagId", t.tagId?.toString())
                            put("note", t.note)
                            put("installments", t.installments)
                            put("principal", t.principal?.let(::amount))
                            put("interest", t.interest?.let(::amount))
                            put("fees", t.fees?.let(::amount))
                            put("installmentId", t.installmentId?.toString())
                            put("recurringId", t.recurringId?.toString())
                            put("paymentKind", t.paymentKind)
                        },
                    )
                },
                decode = { c ->
                    val d = c.data
                    TransactionEntity(
                        id = UUID.fromString(c.id),
                        type = d.text("type")!!,
                        amount = scaled(d.text("amount")!!),
                        occurredAt = Instant.parse(d.text("occurredAt")!!),
                        accountId = UUID.fromString(d.text("accountId")!!),
                        destinationAccountId = d.text("destinationAccountId")?.let(UUID::fromString),
                        categoryId = d.text("categoryId")?.let(UUID::fromString),
                        tagId = d.text("tagId")?.let(UUID::fromString),
                        note = d.text("note"),
                        installments = d.int("installments"),
                        principal = d.text("principal")?.let(::scaled),
                        interest = d.text("interest")?.let(::scaled),
                        fees = d.text("fees")?.let(::scaled),
                        installmentId = d.text("installmentId")?.let(UUID::fromString),
                        recurringId = d.text("recurringId")?.let(UUID::fromString),
                        paymentKind = d.text("paymentKind"),
                        createdAt = Instant.parse(c.createdAt),
                        updatedAt = Instant.parse(c.updatedAt),
                        deletedAt = c.deletedAt?.let(Instant::parse),
                        syncState = SyncState.SYNCED,
                    )
                },
            )

        /** Dependency order: settings, accounts, categories, tags, transactions. */
        val all: List<SyncTable<*>> = listOf(settings, accounts, categories, tags, transactions)

        val byEntity: Map<String, SyncTable<*>> = all.associateBy { it.entity }
    }

private fun envelope(
    entity: String,
    id: UUID,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?,
    data: JsonObject,
) = ChangeDto(entity, id.toString(), createdAt.toString(), updatedAt.toString(), deletedAt?.toString(), data)

/** Long ×10⁴ ↔ the contract's Amount string ("28000.0000"), never through a Double (ADR-0004). */
internal fun amount(scaled: Long): String = Money(scaled).toBigDecimal().toPlainString()

internal fun scaled(amount: String): Long = Money.of(BigDecimal(amount)).scaled

private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

private fun JsonObject.bool(key: String): Boolean = this[key]?.jsonPrimitive?.boolean ?: false

private fun JsonObject.int(key: String): Int = this[key]?.jsonPrimitive?.int ?: 0

private fun homeCards(text: String): JsonElement =
    runCatching { Json.parseToJsonElement(text) }.getOrNull() as? JsonArray ?: JsonArray(emptyList())
