package com.dev.julian09.financeapp.data.local

import com.dev.julian09.financeapp.data.local.entity.AccountEntity
import com.dev.julian09.financeapp.data.local.entity.CategoryEntity
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.local.entity.TransactionEntity
import com.dev.julian09.financeapp.data.repository.AccountRepositoryImpl
import com.dev.julian09.financeapp.data.repository.TransactionRepositoryImpl
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.Money
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LedgerDaoTest {
    private val db = inMemoryDatabase()
    private val t0 = Instant.parse("2026-10-01T10:00:00Z")

    @After
    fun close() = db.close()

    @Test
    fun activeAccountsHideDeactivatedAndDeletedOnes() =
        runBlocking {
            db.accounts().upsert(
                listOf(
                    account("Ahorros", sortOrder = 2),
                    account("Bancolombia", sortOrder = 1),
                    account("Vieja", active = false),
                    account("Borrada", deletedAt = t0),
                ),
            )

            val active = AccountRepositoryImpl(db, db.accounts(), db.transactions(), FixedClock()).observeActive().first()
            assertEquals(listOf("Bancolombia", "Ahorros"), active.map { it.name })
            assertEquals(AccountType.DEBIT, active.first().type)
            assertEquals(Money.ofPesos(1_250_000), active.first().initialBalance)
        }

    @Test
    fun recentTransactionsAreNewestFirstWithoutDeletedOnes() =
        runBlocking {
            val account = UUID.randomUUID()
            val category = UUID.randomUUID()
            db.categories().upsert(listOf(CategoryEntity(id = category, name = "Comida", kind = "EXPENSE", createdAt = t0, updatedAt = t0)))
            db.transactions().upsert(
                listOf(
                    tx(account, category, t0, 28000),
                    tx(account, category, t0.plusSeconds(60), 5000),
                    tx(account, category, t0.plusSeconds(120), 9000, deleted = true),
                ),
            )

            val recent = TransactionRepositoryImpl(db.transactions()).observeRecent(10).first()
            assertEquals(listOf(Money.ofPesos(5000), Money.ofPesos(28000)), recent.map { it.amount })
            assertEquals(1, TransactionRepositoryImpl(db.transactions()).observeRecent(1).first().size)
            assertEquals(3, db.transactions().observeUnsynced().first())
        }

    private fun account(
        name: String,
        sortOrder: Int = 0,
        active: Boolean = true,
        deletedAt: Instant? = null,
    ) = AccountEntity(
        id = UUID.randomUUID(),
        name = name,
        type = "DEBIT",
        initialBalance = Money.ofPesos(1_250_000).scaled,
        active = active,
        sortOrder = sortOrder,
        createdAt = t0,
        updatedAt = t0,
        deletedAt = deletedAt,
    )

    private fun tx(
        account: UUID,
        category: UUID,
        at: Instant,
        pesos: Long,
        deleted: Boolean = false,
    ) = TransactionEntity(
        id = UUID.randomUUID(),
        type = "EXPENSE",
        amount = Money.ofPesos(pesos).scaled,
        occurredAt = at,
        accountId = account,
        categoryId = category,
        createdAt = at,
        updatedAt = at,
        deletedAt = if (deleted) at else null,
        syncState = SyncState.PENDING,
    )
}
