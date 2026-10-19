package com.dev.julian09.financeapp.data.repository

import androidx.room.withTransaction
import com.dev.julian09.financeapp.data.local.FinanceDatabase
import com.dev.julian09.financeapp.data.local.dao.AccountDao
import com.dev.julian09.financeapp.data.local.dao.CategoryDao
import com.dev.julian09.financeapp.data.local.dao.TagDao
import com.dev.julian09.financeapp.data.local.dao.TransactionDao
import com.dev.julian09.financeapp.data.local.entity.AccountEntity
import com.dev.julian09.financeapp.data.local.entity.CategoryEntity
import com.dev.julian09.financeapp.data.local.entity.ChangeLogEntity
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.local.entity.TagEntity
import com.dev.julian09.financeapp.data.local.entity.TransactionEntity
import com.dev.julian09.financeapp.data.sync.transactionData
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountFlows
import com.dev.julian09.financeapp.domain.model.AccountImpact
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.CategoryTotal
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.PeriodTotals
import com.dev.julian09.financeapp.domain.model.SearchTotals
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionFilter
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.TagRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class AccountRepositoryImpl
    @Inject
    constructor(
        private val db: FinanceDatabase,
        private val dao: AccountDao,
        private val transactions: TransactionDao,
        private val clock: Clock,
    ) : AccountRepository {
        override fun observeActive(): Flow<List<Account>> = dao.observeActive().map { rows -> rows.map { it.toDomain() } }

        override fun observeBalances(before: Instant?): Flow<List<AccountBalance>> =
            dao.observeBalances(before ?: END_OF_TIME).map { rows ->
                rows.map {
                    AccountBalance(
                        it.account.toDomain(),
                        AccountFlows(Money(it.income), Money(it.expense), Money(it.transfersOut), Money(it.transfersIn)),
                        it.transactions,
                    )
                }
            }

        override suspend fun create(account: Account) {
            val now = clock.now()
            dao.upsert(listOf(account.toEntity(createdAt = now, updatedAt = now)))
        }

        override suspend fun update(account: Account) {
            val stored = dao.get(account.id) ?: return
            dao.upsert(listOf(account.toEntity(createdAt = stored.createdAt, updatedAt = clock.now())))
        }

        override suspend fun setActive(
            id: UUID,
            active: Boolean,
        ) {
            val now = clock.now()
            dao.setActive(id, active, if (active) null else now, now)
        }

        override suspend fun impact(id: UUID): AccountImpact {
            val row = transactions.accountImpact(id)
            val balance = observeBalances().first().first { it.account.id == id }.balance
            return AccountImpact(row.count, balance, row.firstAt, row.lastAt)
        }

        override suspend fun moveAndDelete(
            id: UUID,
            targetId: UUID,
        ) = db.withTransaction {
            val now = clock.now()
            transactions.deleteTransfersBetween(id, targetId, now)
            transactions.moveSource(id, targetId, now)
            transactions.moveDestination(id, targetId, now)
            dao.markDeleted(id, now)
        }

        override suspend fun deleteWithTransactions(id: UUID) =
            db.withTransaction {
                val now = clock.now()
                transactions.deleteForAccount(id, now)
                dao.markDeleted(id, now)
            }

        private companion object {
            val END_OF_TIME: Instant = Instant.ofEpochMilli(Long.MAX_VALUE)
        }
    }

class CategoryRepositoryImpl
    @Inject
    constructor(
        private val db: FinanceDatabase,
        private val dao: CategoryDao,
        private val transactions: TransactionDao,
        private val clock: Clock,
    ) : CategoryRepository {
        override fun observeAll(): Flow<List<Category>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

        override fun observeTotals(
            from: Instant,
            to: Instant,
        ): Flow<List<CategoryTotal>> =
            transactions.observeCategoryTotals(from, to).map { rows ->
                rows.map { CategoryTotal(it.categoryId, Money(it.total), it.count) }
            }

        override suspend fun create(category: Category) {
            val now = clock.now()
            dao.upsert(
                listOf(
                    CategoryEntity(
                        id = category.id,
                        parentId = category.parentId,
                        name = category.name,
                        kind = category.kind.name,
                        isBase = category.isBase,
                        systemKey = category.systemKey,
                        icon = category.icon,
                        sortOrder = category.sortOrder,
                        createdAt = now,
                        updatedAt = now,
                    ),
                ),
            )
        }

        override suspend fun rename(
            id: UUID,
            name: String,
        ) = dao.rename(id, name, clock.now())

        override suspend fun transactionCount(id: UUID): Int = transactions.countInCategory(id)

        override suspend fun delete(
            id: UUID,
            targetId: UUID?,
        ) = db.withTransaction {
            val now = clock.now()
            if (targetId != null) transactions.reassignCategory(id, targetId, now)
            dao.deleteWithChildren(id, now)
        }
    }

/** REQ-HIS-01: one change_log row per write, with the synced data before and after, in the caller's transaction. */
class ChangeLogWriter
    @Inject
    constructor(
        private val db: FinanceDatabase,
    ) {
        suspend fun log(
            action: String,
            before: TransactionEntity?,
            after: TransactionEntity?,
            now: Instant,
        ) {
            val transactionId = (after ?: before)!!.id
            db.changeLog().upsert(
                listOf(
                    ChangeLogEntity(
                        id = UUID.randomUUID(),
                        transactionId = transactionId,
                        action = action,
                        beforeValue = before?.let { transactionData(it).toString() },
                        afterValue = after?.let { transactionData(it).toString() },
                        changedAt = now,
                        createdAt = now,
                        updatedAt = now,
                    ),
                ),
            )
        }
    }

class TagRepositoryImpl
    @Inject
    constructor(
        private val dao: TagDao,
        private val clock: Clock,
    ) : TagRepository {
        override fun observeAll(): Flow<List<Tag>> = dao.observeAll().map { rows -> rows.map { Tag(it.id, it.name, it.pinned) } }

        override suspend fun create(tag: Tag) {
            val now = clock.now()
            dao.upsert(listOf(TagEntity(id = tag.id, name = tag.name, pinned = tag.pinned, createdAt = now, updatedAt = now)))
        }
    }

class TransactionRepositoryImpl
    @Inject
    constructor(
        private val db: FinanceDatabase,
        private val dao: TransactionDao,
        private val history: ChangeLogWriter,
        private val clock: Clock,
    ) : TransactionRepository {
        override fun observe(id: UUID): Flow<Transaction?> = dao.observe(id).map { it?.toDomain() }

        override suspend fun lastUsedAccountId(): UUID? = dao.lastUsedAccountId()

        override suspend fun insert(transaction: Transaction) =
            db.withTransaction {
                val now = clock.now()
                val row = transaction.toEntity(createdAt = now, updatedAt = now)
                dao.upsert(listOf(row))
                history.log("CREATE", null, row, now)
            }

        override suspend fun update(transaction: Transaction) =
            db.withTransaction {
                val before = dao.get(transaction.id) ?: return@withTransaction
                val now = clock.now()
                val after = transaction.toEntity(createdAt = before.createdAt, updatedAt = now)
                dao.upsert(listOf(after))
                history.log("UPDATE", before, after, now)
            }

        override suspend fun delete(id: UUID) =
            db.withTransaction {
                val before = dao.get(id)?.takeIf { it.deletedAt == null } ?: return@withTransaction
                val now = clock.now()
                dao.upsert(listOf(before.copy(deletedAt = now, updatedAt = now, syncState = SyncState.PENDING, syncError = null)))
                history.log("DELETE", before, null, now)
            }

        override fun observeRecent(limit: Int): Flow<List<Transaction>> =
            dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

        override fun observeSince(from: Instant): Flow<List<Transaction>> =
            dao.observeSince(from).map { rows -> rows.map { it.toDomain() } }

        override fun observeByAccount(accountId: UUID): Flow<List<Transaction>> =
            dao.observeByAccount(accountId).map { rows -> rows.map { it.toDomain() } }

        override fun observeTotals(
            from: Instant,
            to: Instant,
        ): Flow<PeriodTotals> = dao.observeTotals(from, to).map { PeriodTotals(Money(it.income), Money(it.expense)) }

        override fun observeCount(): Flow<Int> = dao.observeCount()

        override fun search(
            filter: TransactionFilter,
            limit: Int,
        ): Flow<List<Transaction>> {
            val f = filter
            val rows =
                dao.search(
                    f.from,
                    f.to,
                    f.types.isEmpty(),
                    f.types.map { it.name },
                    f.categoryIds.isEmpty(),
                    f.categoryIds.toList(),
                    f.accountIds.isEmpty(),
                    f.accountIds.toList(),
                    f.tagIds.isEmpty(),
                    f.tagIds.toList(),
                    like(f.query),
                    limit,
                )
            return rows.map { list -> list.map { it.toDomain() } }
        }

        override fun searchTotals(filter: TransactionFilter): Flow<SearchTotals> {
            val f = filter
            val totals =
                dao.searchTotals(
                    f.from,
                    f.to,
                    f.types.isEmpty(),
                    f.types.map { it.name },
                    f.categoryIds.isEmpty(),
                    f.categoryIds.toList(),
                    f.accountIds.isEmpty(),
                    f.accountIds.toList(),
                    f.tagIds.isEmpty(),
                    f.tagIds.toList(),
                    like(f.query),
                )
            return totals.map { SearchTotals(Money(it.income), Money(it.expense), it.count) }
        }

        override fun observeFirstOccurredAt(): Flow<Instant?> = dao.observeFirstOccurredAt()

        // shortcut: SQLite LIKE ignores case only for ASCII, so "cafe" misses "Café"; normalize text if the PO asks
        private fun like(query: String): String {
            val text = query.trim()
            if (text.isEmpty()) return ""
            return "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
        }
    }

internal fun Transaction.toEntity(
    createdAt: Instant,
    updatedAt: Instant,
) = TransactionEntity(
    id = id,
    type = type.name,
    amount = amount.scaled,
    occurredAt = occurredAt,
    accountId = accountId,
    destinationAccountId = destinationAccountId,
    categoryId = categoryId,
    tagId = tagId,
    note = note,
    installments = installments,
    principal = principal?.scaled,
    interest = interest?.scaled,
    fees = fees?.scaled,
    installmentId = installmentId,
    recurringId = recurringId,
    paymentKind = paymentKind,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun Account.toEntity(
    createdAt: Instant,
    updatedAt: Instant,
) = AccountEntity(
    id = id,
    name = name,
    type = type.name,
    initialBalance = initialBalance.scaled,
    includeInTotal = includeInTotal,
    active = active,
    deactivatedAt = deactivatedAt,
    institution = institution,
    icon = icon,
    color = color,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun AccountEntity.toDomain() =
    Account(
        id,
        name,
        AccountType.valueOf(type),
        Money(initialBalance),
        includeInTotal,
        active,
        deactivatedAt,
        institution,
        icon,
        color,
        sortOrder,
        createdAt,
    )

internal fun CategoryEntity.toDomain() = Category(id, name, CategoryKind.valueOf(kind), parentId, isBase, systemKey, icon, sortOrder)

internal fun TransactionEntity.toDomain() =
    Transaction(
        id = id,
        type = TransactionType.valueOf(type),
        amount = Money(amount),
        occurredAt = occurredAt,
        accountId = accountId,
        destinationAccountId = destinationAccountId,
        categoryId = categoryId,
        tagId = tagId,
        note = note,
        installments = installments,
        principal = principal?.let(::Money),
        interest = interest?.let(::Money),
        fees = fees?.let(::Money),
        installmentId = installmentId,
        recurringId = recurringId,
        paymentKind = paymentKind,
        createdAt = createdAt,
        pendingSync = syncState != SyncState.SYNCED,
    )
