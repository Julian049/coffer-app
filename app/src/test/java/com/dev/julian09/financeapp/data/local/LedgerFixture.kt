package com.dev.julian09.financeapp.data.local

import com.dev.julian09.financeapp.data.local.entity.AccountEntity
import com.dev.julian09.financeapp.data.local.entity.CategoryEntity
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.local.entity.TransactionEntity
import com.dev.julian09.financeapp.data.repository.AccountRepositoryImpl
import com.dev.julian09.financeapp.data.repository.CategoryRepositoryImpl
import com.dev.julian09.financeapp.data.repository.TransactionRepositoryImpl
import java.time.Instant
import java.util.UUID

/** Real repositories over an in-memory database (Robolectric), with rows written as already SYNCED. */
class LedgerFixture(
    val clock: FixedClock = FixedClock(),
) {
    val db = inMemoryDatabase()
    val accounts = AccountRepositoryImpl(db, db.accounts(), db.transactions(), clock)
    val categories = CategoryRepositoryImpl(db, db.categories(), db.transactions(), clock)
    val transactions = TransactionRepositoryImpl(db.transactions())
    private val past: Instant = Instant.parse("2026-01-01T00:00:00Z")

    suspend fun account(
        name: String,
        pesos: Long = 0,
        type: String = "DEBIT",
        active: Boolean = true,
        include: Boolean = true,
        createdAt: Instant = past,
    ): UUID {
        val id = UUID.randomUUID()
        db.accounts().upsert(
            listOf(
                AccountEntity(
                    id = id,
                    name = name,
                    type = type,
                    initialBalance = pesos * 10_000,
                    includeInTotal = include,
                    active = active,
                    createdAt = createdAt,
                    updatedAt = past,
                    syncState = SyncState.SYNCED,
                ),
            ),
        )
        return id
    }

    suspend fun category(
        name: String,
        kind: String = "EXPENSE",
        parent: UUID? = null,
        systemKey: String? = null,
    ): UUID {
        val id = UUID.randomUUID()
        db.categories().upsert(
            listOf(
                CategoryEntity(
                    id = id,
                    parentId = parent,
                    name = name,
                    kind = kind,
                    systemKey = systemKey,
                    createdAt = past,
                    updatedAt = past,
                    syncState = SyncState.SYNCED,
                ),
            ),
        )
        return id
    }

    suspend fun tx(
        type: String,
        pesos: Long,
        account: UUID,
        at: Instant = clock.now(),
        category: UUID? = null,
        destination: UUID? = null,
    ): UUID {
        val id = UUID.randomUUID()
        db.transactions().upsert(
            listOf(
                TransactionEntity(
                    id = id,
                    type = type,
                    amount = pesos * 10_000,
                    occurredAt = at,
                    accountId = account,
                    destinationAccountId = destination,
                    categoryId = category,
                    createdAt = past,
                    updatedAt = past,
                    syncState = SyncState.SYNCED,
                ),
            ),
        )
        return id
    }
}
