package com.dev.julian09.financeapp.data.repository

import androidx.room.withTransaction
import com.dev.julian09.financeapp.data.local.FinanceDatabase
import com.dev.julian09.financeapp.data.local.dao.AccountDao
import com.dev.julian09.financeapp.data.local.dao.CardDao
import com.dev.julian09.financeapp.data.local.dao.CategoryDao
import com.dev.julian09.financeapp.data.local.dao.ChangeLogDao
import com.dev.julian09.financeapp.data.local.dao.DraftDao
import com.dev.julian09.financeapp.data.local.dao.LoanDao
import com.dev.julian09.financeapp.data.local.dao.NoteRow
import com.dev.julian09.financeapp.data.local.dao.RecurringDao
import com.dev.julian09.financeapp.data.local.dao.SettingsDao
import com.dev.julian09.financeapp.data.local.dao.SyncMetaDao
import com.dev.julian09.financeapp.data.local.dao.TagDao
import com.dev.julian09.financeapp.data.local.dao.TransactionDao
import com.dev.julian09.financeapp.data.local.entity.AccountEntity
import com.dev.julian09.financeapp.data.local.entity.CategoryEntity
import com.dev.julian09.financeapp.data.local.entity.ChangeLogEntity
import com.dev.julian09.financeapp.data.local.entity.CreditCardDetailsEntity
import com.dev.julian09.financeapp.data.local.entity.DeferredPurchaseEntity
import com.dev.julian09.financeapp.data.local.entity.InstallmentEntity
import com.dev.julian09.financeapp.data.local.entity.LoanDetailsEntity
import com.dev.julian09.financeapp.data.local.entity.RecurringExpenseEntity
import com.dev.julian09.financeapp.data.local.entity.SyncMetaEntity
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.local.entity.TagEntity
import com.dev.julian09.financeapp.data.local.entity.TransactionDraftEntity
import com.dev.julian09.financeapp.data.local.entity.TransactionEntity
import com.dev.julian09.financeapp.data.local.entity.decodeTagIds
import com.dev.julian09.financeapp.data.local.entity.encodeTagIds
import com.dev.julian09.financeapp.data.sync.SyncMeta
import com.dev.julian09.financeapp.data.sync.transactionData
import com.dev.julian09.financeapp.data.sync.transactionFromData
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountFlows
import com.dev.julian09.financeapp.domain.model.AccountImpact
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.Amortization
import com.dev.julian09.financeapp.domain.model.AmountQuery
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.CategoryTotal
import com.dev.julian09.financeapp.domain.model.ChangeAction
import com.dev.julian09.financeapp.domain.model.ChangeEntry
import com.dev.julian09.financeapp.domain.model.CreditCard
import com.dev.julian09.financeapp.domain.model.DeferredPurchase
import com.dev.julian09.financeapp.domain.model.DraftSource
import com.dev.julian09.financeapp.domain.model.Frequency
import com.dev.julian09.financeapp.domain.model.HomeCard
import com.dev.julian09.financeapp.domain.model.Installment
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.IntervalUnit
import com.dev.julian09.financeapp.domain.model.Loan
import com.dev.julian09.financeapp.domain.model.LoanInstallment
import com.dev.julian09.financeapp.domain.model.LoanMode
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.NetBalanceMode
import com.dev.julian09.financeapp.domain.model.NoteSuggestion
import com.dev.julian09.financeapp.domain.model.PeriodTotals
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.model.Recurring
import com.dev.julian09.financeapp.domain.model.SearchTotals
import com.dev.julian09.financeapp.domain.model.Settings
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.model.TagTotal
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionDraft
import com.dev.julian09.financeapp.domain.model.TransactionFilter
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CardRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.ChangeLogRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.DraftRepository
import com.dev.julian09.financeapp.domain.port.LoanRepository
import com.dev.julian09.financeapp.domain.port.RecurringRepository
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.port.TagRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import com.dev.julian09.financeapp.domain.usecase.OPENING_BALANCE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
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
            // The carousel order is not part of the form (M9)
            dao.upsert(listOf(account.toEntity(createdAt = stored.createdAt, updatedAt = clock.now()).copy(sortOrder = stored.sortOrder)))
        }

        override suspend fun moveOpeningBalances(): Boolean {
            val owner = db.syncMeta().get(SyncMeta.OWNER) ?: return false
            val accounts = dao.withOpeningBalance().ifEmpty { return false }
            db.withTransaction {
                val now = clock.now()
                val categoryId = UUID.nameUUIDFromBytes("$owner:$OPENING_BALANCE".toByteArray())
                if (db.categories().get(categoryId) == null) {
                    db.categories().upsert(
                        listOf(
                            CategoryEntity(
                                id = categoryId,
                                name = "Saldo inicial",
                                kind = CategoryKind.INCOME.name,
                                isBase = true,
                                systemKey = OPENING_BALANCE,
                                icon = "savings",
                                sortOrder = Int.MAX_VALUE,
                                createdAt = now,
                                updatedAt = now,
                                excludeFromReports = true,
                            ),
                        ),
                    )
                }
                accounts.forEach { a ->
                    transactions.upsert(
                        listOf(
                            TransactionEntity(
                                id = UUID.nameUUIDFromBytes("$OPENING_BALANCE:${a.id}".toByteArray()),
                                type = TransactionType.INCOME.name,
                                amount = a.initialBalance,
                                occurredAt = a.createdAt,
                                accountId = a.id,
                                categoryId = categoryId,
                                note = "Saldo inicial",
                                createdAt = now,
                                updatedAt = now,
                            ),
                        ),
                    )
                    dao.upsert(listOf(a.copy(initialBalance = 0, updatedAt = now, syncState = SyncState.PENDING, syncError = null)))
                }
            }
            return true
        }

        override suspend fun ensureLoanCategory(): Boolean {
            val owner = db.syncMeta().get(SyncMeta.OWNER) ?: return false
            // The settings row arrives with the first download
            val stored = db.settings().observe().first() ?: return false
            if (stored.loanPaymentCategoryId != null) return false
            val categoryId = UUID.nameUUIDFromBytes("$owner:LOAN_PAYMENTS".toByteArray())
            // Created once: if the user deleted it, the next payment asks for another category instead
            if (db.categories().get(categoryId) != null) return false
            db.withTransaction {
                val now = clock.now()
                db.categories().upsert(
                    listOf(
                        CategoryEntity(
                            id = categoryId,
                            name = "Deudas",
                            kind = CategoryKind.EXPENSE.name,
                            icon = "money",
                            sortOrder = Int.MAX_VALUE - 1,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    ),
                )
                db.settings().upsert(
                    stored.copy(loanPaymentCategoryId = categoryId, updatedAt = now, syncState = SyncState.PENDING, syncError = null),
                )
            }
            return true
        }

        override suspend fun reorder(ids: List<UUID>) =
            db.withTransaction {
                val now = clock.now()
                ids.forEachIndexed { index, id -> dao.setSortOrder(id, index, now) }
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
                        excludeFromReports = category.excludeFromReports,
                        color = category.color,
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

        override suspend fun setIcon(
            id: UUID,
            icon: String,
        ) = dao.setIcon(id, icon, clock.now())

        override suspend fun setExcludedFromReports(
            id: UUID,
            excluded: Boolean,
        ) = dao.setExcludedFromReports(id, excluded, clock.now())

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
        suspend fun logCreated(
            rows: List<TransactionEntity>,
            now: Instant,
        ) = db.changeLog().upsert(
            rows.map {
                ChangeLogEntity(
                    id = UUID.randomUUID(),
                    transactionId = it.id,
                    action = "CREATE",
                    afterValue = transactionData(it).toString(),
                    changedAt = now,
                    createdAt = now,
                    updatedAt = now,
                )
            },
        )

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

class ChangeLogRepositoryImpl
    @Inject
    constructor(
        private val dao: ChangeLogDao,
    ) : ChangeLogRepository {
        override fun observeSince(from: Instant): Flow<List<ChangeEntry>> =
            dao.observeSince(from).map { rows ->
                rows.map {
                    ChangeEntry(
                        id = it.id,
                        transactionId = it.transactionId,
                        action = ChangeAction.valueOf(it.action),
                        before = it.beforeValue?.let { json -> movement(it.transactionId, json) },
                        after = it.afterValue?.let { json -> movement(it.transactionId, json) },
                        changedAt = it.changedAt,
                    )
                }
            }

        override suspend fun purgeBefore(cutoff: Instant) = dao.deleteBefore(cutoff)

        private fun movement(
            id: UUID,
            json: String,
        ) = transactionFromData(id, Json.parseToJsonElement(json).jsonObject).toDomain()
    }

class TagRepositoryImpl
    @Inject
    constructor(
        private val db: FinanceDatabase,
        private val dao: TagDao,
        private val clock: Clock,
    ) : TagRepository {
        override fun observeAll(): Flow<List<Tag>> = dao.observeAll().map { rows -> rows.map { Tag(it.id, it.name, it.pinned, it.color) } }

        override suspend fun create(tag: Tag) {
            val now = clock.now()
            dao.upsert(
                listOf(TagEntity(id = tag.id, name = tag.name, pinned = tag.pinned, color = tag.color, createdAt = now, updatedAt = now)),
            )
        }

        override suspend fun rename(
            id: UUID,
            name: String,
        ) = dao.rename(id, name, clock.now())

        override suspend fun setPinned(
            id: UUID,
            pinned: Boolean,
        ) = dao.setPinned(id, pinned, clock.now())

        override suspend fun setColor(
            id: UUID,
            color: String,
        ) = dao.setColor(id, color, clock.now())

        override suspend fun movementCount(id: UUID): Int = dao.movementCount(id)

        override suspend fun delete(id: UUID) {
            val now = clock.now()
            db.withTransaction {
                dao.untag(id, now)
                dao.delete(id, now)
            }
        }

        override fun observeTotals(
            from: Instant,
            to: Instant,
        ): Flow<List<TagTotal>> =
            dao.observeTotals(from, to).map { rows -> rows.map { TagTotal(it.tagId, Money(it.income), Money(it.expense), it.count) } }
    }

class TransactionRepositoryImpl
    @Inject
    constructor(
        private val db: FinanceDatabase,
        private val dao: TransactionDao,
        private val cards: CardDao,
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

        override suspend fun insertAll(transactions: List<Transaction>) =
            db.withTransaction {
                val now = clock.now()
                val rows = transactions.map { it.toEntity(createdAt = now, updatedAt = now) }
                dao.upsert(rows)
                history.logCreated(rows, now)
            }

        override suspend fun update(transaction: Transaction) =
            db.withTransaction {
                val before = dao.get(transaction.id) ?: return@withTransaction
                val now = clock.now()
                val after = transaction.toEntity(createdAt = before.createdAt, updatedAt = now)
                dao.upsert(listOf(after))
                history.log("UPDATE", before, after, now)
            }

        override suspend fun changePaymentDate(
            id: UUID,
            occurredAt: Instant,
        ): Boolean =
            db.withTransaction {
                val before = dao.get(id)?.takeIf { it.deletedAt == null && it.paymentKind != null } ?: return@withTransaction false
                val now = clock.now()
                val after = before.copy(occurredAt = occurredAt, updatedAt = now, syncState = SyncState.PENDING, syncError = null)
                dao.upsert(listOf(after))
                history.log("UPDATE", before, after, now)
                // A loan payment settled one installment; a card payment, those stamped with its date (payments from before 1.6.0
                // were stamped with the day they were registered and stay as they were)
                val settled =
                    before.installmentId?.let { listOfNotNull(cards.getInstallment(it)) }
                        ?: before.destinationAccountId?.let { cards.installmentsPaidOn(it, before.occurredAt) }.orEmpty()
                cards.upsertInstallments(
                    settled.filter { it.status == "PAID" }.map {
                        it.copy(paidAt = occurredAt, updatedAt = now, syncState = SyncState.PENDING, syncError = null)
                    },
                )
                true
            }

        override suspend fun delete(id: UUID) =
            db.withTransaction {
                val before = dao.get(id)?.takeIf { it.deletedAt == null } ?: return@withTransaction
                val now = clock.now()
                dao.upsert(listOf(before.copy(deletedAt = now, updatedAt = now, syncState = SyncState.PENDING, syncError = null)))
                history.log("DELETE", before, null, now)
                // A deferred card purchase takes its plan with it
                cards.deletePlanOf(id, now)
                cards.deleteDeferredOf(id, now)
            }

        override suspend fun restore(id: UUID): Boolean =
            db.withTransaction {
                val before = dao.get(id)?.takeIf { it.deletedAt != null } ?: return@withTransaction false
                val now = clock.now()
                val after = before.copy(deletedAt = null, updatedAt = now, syncState = SyncState.PENDING, syncError = null)
                dao.upsert(listOf(after))
                history.log("RESTORE", null, after, now)
                true
            }

        override fun observeRecent(limit: Int): Flow<List<Transaction>> =
            dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

        override suspend fun suggestNotes(
            type: TransactionType,
            text: String,
            limit: Int,
        ): List<NoteSuggestion> {
            val pattern = like(text).ifEmpty { return emptyList() }
            // More rows than [limit]: the ranking can bring a later row up
            return rankNotes(text, dao.notes(type.name, pattern, NOTE_CANDIDATES)).take(limit)
        }

        override fun observeSince(from: Instant): Flow<List<Transaction>> =
            dao.observeSince(from).map { rows -> rows.map { it.toDomain() } }

        override fun observeByAccount(accountId: UUID): Flow<List<Transaction>> =
            dao.observeByAccount(accountId).map { rows -> rows.map { it.toDomain() } }

        override fun observeBetween(
            from: Instant,
            to: Instant,
        ): Flow<List<Transaction>> = dao.observeBetween(from, to).map { rows -> rows.map { it.toDomain() } }

        override fun observeDeferredPurchases(): Flow<List<Transaction>> =
            dao.observeDeferredPurchases().map { rows -> rows.map { it.toDomain() } }

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
                    amountOf(f.query),
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
                    amountOf(f.query),
                )
            return totals.map { SearchTotals(Money(it.income), Money(it.expense), it.count) }
        }

        override fun observeFirstOccurredAt(): Flow<Instant?> = dao.observeFirstOccurredAt()

        override fun observeRecurringPayments(
            from: Instant,
            to: Instant,
        ): Flow<List<Transaction>> = dao.observeRecurringPayments(from, to).map { rows -> rows.map { it.toDomain() } }

        // shortcut: SQLite LIKE ignores case only for ASCII, so "cafe" misses "Café"; normalize text if the PO asks

        /** S15-DAT02: an amount typed in the search also matches the movements of exactly that value; no amount never matches. */
        private fun amountOf(query: String): Long = AmountQuery.parse(query)?.scaled ?: -1L

        private fun like(query: String): String {
            val text = query.trim()
            if (text.isEmpty()) return ""
            return "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
        }
    }

class RecurringRepositoryImpl
    @Inject
    constructor(
        private val db: FinanceDatabase,
        private val dao: RecurringDao,
        private val transactions: TransactionDao,
        private val history: ChangeLogWriter,
        private val clock: Clock,
    ) : RecurringRepository {
        override fun observeAll(): Flow<List<Recurring>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

        override fun observe(id: UUID): Flow<Recurring?> = dao.observe(id).map { it?.toDomain() }

        override suspend fun create(recurring: Recurring) {
            val now = clock.now()
            dao.upsert(listOf(recurring.toEntity(createdAt = now, updatedAt = now)))
        }

        override suspend fun update(recurring: Recurring) {
            val stored = dao.get(recurring.id) ?: return
            dao.upsert(listOf(recurring.toEntity(createdAt = stored.createdAt, updatedAt = clock.now())))
        }

        override suspend fun pay(
            recurring: Recurring,
            payment: Transaction,
        ) = db.withTransaction {
            val stored = dao.get(recurring.id) ?: return@withTransaction
            val now = clock.now()
            val row = payment.toEntity(createdAt = now, updatedAt = now)
            transactions.upsert(listOf(row))
            history.log("CREATE", null, row, now)
            dao.upsert(listOf(recurring.toEntity(createdAt = stored.createdAt, updatedAt = now)))
        }

        override suspend fun markNotified(
            id: UUID,
            on: LocalDate,
        ) = dao.markNotified(id, on, clock.now())
    }

class CardRepositoryImpl
    @Inject
    constructor(
        private val db: FinanceDatabase,
        private val dao: CardDao,
        private val accounts: AccountDao,
        private val transactions: TransactionDao,
        private val meta: SyncMetaDao,
        private val history: ChangeLogWriter,
        private val clock: Clock,
    ) : CardRepository {
        override fun observeCards(): Flow<List<CreditCard>> = dao.observeCards().map { rows -> rows.map { it.toDomain() } }

        override fun observeDeferred(): Flow<List<DeferredPurchase>> = dao.observeDeferred().map { rows -> rows.map { it.toDomain() } }

        override fun observeInstallments(): Flow<List<Installment>> = dao.observeInstallments().map { rows -> rows.map { it.toDomain() } }

        override fun observeCardTransactions(): Flow<List<Transaction>> =
            dao.observeCardTransactions().map { rows -> rows.map { it.toDomain() } }

        override suspend fun create(
            account: Account,
            card: CreditCard,
        ) = db.withTransaction {
            val now = clock.now()
            accounts.upsert(listOf(account.toEntity(createdAt = now, updatedAt = now)))
            dao.upsertCards(listOf(card.toEntity(createdAt = now, updatedAt = now)))
        }

        override suspend fun update(
            account: Account,
            card: CreditCard,
        ) = db.withTransaction {
            val stored = accounts.get(account.id) ?: return@withTransaction
            val now = clock.now()
            accounts.upsert(listOf(account.toEntity(createdAt = stored.createdAt, updatedAt = now)))
            dao.upsertCards(listOf(card.toEntity(createdAt = dao.getCard(card.accountId)?.createdAt ?: now, updatedAt = now)))
        }

        override suspend fun insertDeferred(
            purchase: Transaction,
            deferred: DeferredPurchase,
            installments: List<Installment>,
        ) = db.withTransaction {
            val now = clock.now()
            val row = purchase.toEntity(createdAt = now, updatedAt = now)
            transactions.upsert(listOf(row))
            history.log("CREATE", null, row, now)
            dao.upsertDeferred(listOf(deferred.toEntity(createdAt = now, updatedAt = now)))
            dao.upsertInstallments(installments.map { it.toEntity(createdAt = now, updatedAt = now) })
        }

        override suspend fun pay(
            payment: Transaction,
            installments: List<Installment>,
        ) = db.withTransaction {
            val now = clock.now()
            val row = payment.toEntity(createdAt = now, updatedAt = now)
            transactions.upsert(listOf(row))
            history.log("CREATE", null, row, now)
            dao.upsertInstallments(
                installments.map { i -> i.toEntity(createdAt = dao.getInstallment(i.id)?.createdAt ?: now, updatedAt = now) },
            )
        }

        override suspend fun lastNotifiedOn(cardId: UUID): LocalDate? = meta.get(notifiedKey(cardId))?.let(LocalDate::parse)

        override suspend fun markNotified(
            cardId: UUID,
            on: LocalDate,
        ) = meta.put(SyncMetaEntity(notifiedKey(cardId), on.toString()))

        private fun notifiedKey(cardId: UUID) = "card_notified:$cardId"
    }

internal fun CreditCard.toEntity(
    createdAt: Instant,
    updatedAt: Instant,
) = CreditCardDetailsEntity(
    id = accountId,
    creditLimit = creditLimit.scaled,
    cutOffDay = cutOffDay,
    paymentDay = paymentDay,
    rate = rate.toPlainString(),
    rateType = rateType.name,
    firstInstallmentInterestFree = firstInstallmentInterestFree,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun CreditCardDetailsEntity.toDomain() =
    CreditCard(id, Money(creditLimit), cutOffDay, paymentDay, BigDecimal(rate), RateType.valueOf(rateType), firstInstallmentInterestFree)

internal fun DeferredPurchase.toEntity(
    createdAt: Instant,
    updatedAt: Instant,
) = DeferredPurchaseEntity(
    id = id,
    cardAccountId = cardAccountId,
    purchaseTransactionId = purchaseTransactionId,
    amount = amount.scaled,
    installmentsCount = installmentsCount,
    rate = rate.toPlainString(),
    rateType = rateType.name,
    firstInstallmentInterestFree = firstInstallmentInterestFree,
    firstDueDate = firstDueDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun DeferredPurchaseEntity.toDomain() =
    DeferredPurchase(
        id,
        cardAccountId,
        purchaseTransactionId,
        Money(amount),
        installmentsCount,
        BigDecimal(rate),
        RateType.valueOf(rateType),
        firstInstallmentInterestFree,
        firstDueDate,
    )

internal fun Installment.toEntity(
    createdAt: Instant,
    updatedAt: Instant,
) = InstallmentEntity(
    id = id,
    deferredPurchaseId = deferredPurchaseId,
    number = number,
    dueDate = dueDate,
    principal = principal.scaled,
    interest = interest.scaled,
    otherCosts = otherCosts.scaled,
    status = status.name,
    paidAt = paidAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InstallmentEntity.toDomain() =
    Installment(
        id = id,
        deferredPurchaseId = deferredPurchaseId!!,
        number = number,
        dueDate = dueDate,
        principal = Money(principal),
        interest = Money(interest),
        otherCosts = Money(otherCosts),
        status = InstallmentStatus.valueOf(status),
        paidAt = paidAt,
    )

class LoanRepositoryImpl
    @Inject
    constructor(
        private val db: FinanceDatabase,
        private val dao: LoanDao,
        private val cards: CardDao,
        private val accounts: AccountDao,
        private val transactions: TransactionDao,
        private val history: ChangeLogWriter,
        private val clock: Clock,
    ) : LoanRepository {
        override fun observeLoans(): Flow<List<Loan>> = dao.observeLoans().map { rows -> rows.map { it.toDomain() } }

        override fun observeInstallments(): Flow<List<LoanInstallment>> =
            dao.observeInstallments().map { rows -> rows.map { it.toLoanInstallment() } }

        override fun observePaymentsSince(from: Instant): Flow<List<Transaction>> =
            dao.observePaymentsSince(from).map { rows -> rows.map { it.toDomain() } }

        override suspend fun create(
            account: Account,
            loan: Loan,
            installments: List<LoanInstallment>,
            disbursement: Transaction?,
        ) = db.withTransaction {
            val now = clock.now()
            accounts.upsert(listOf(account.toEntity(createdAt = now, updatedAt = now)))
            dao.upsertLoans(listOf(loan.toEntity(createdAt = now, updatedAt = now)))
            cards.upsertInstallments(installments.map { it.toEntity(createdAt = now, updatedAt = now) })
            if (disbursement != null) {
                val row = disbursement.toEntity(createdAt = now, updatedAt = now)
                transactions.upsert(listOf(row))
                history.log("CREATE", null, row, now)
            }
        }

        override suspend fun update(
            loan: Loan,
            installments: List<LoanInstallment>,
        ) = db.withTransaction {
            val now = clock.now()
            dao.upsertLoans(listOf(loan.toEntity(createdAt = dao.getLoan(loan.accountId)?.createdAt ?: now, updatedAt = now)))
            cards.upsertInstallments(installments.map { it.toEntity(createdAt = now, updatedAt = now) })
        }

        override suspend fun pay(
            payment: Transaction,
            installments: List<LoanInstallment>,
        ) = db.withTransaction {
            val now = clock.now()
            val row = payment.toEntity(createdAt = now, updatedAt = now)
            transactions.upsert(listOf(row))
            history.log("CREATE", null, row, now)
            cards.upsertInstallments(
                installments.map { i -> i.toEntity(createdAt = cards.getInstallment(i.id)?.createdAt ?: now, updatedAt = now) },
            )
        }
    }

internal fun Loan.toEntity(
    createdAt: Instant,
    updatedAt: Instant,
) = LoanDetailsEntity(
    id = accountId,
    mode = mode.name,
    disbursementAccountId = disbursementAccountId,
    principal = principal?.scaled,
    installmentsCount = installmentsCount,
    rate = rate?.toPlainString(),
    rateType = rateType?.name,
    firstDueDate = firstDueDate,
    paymentDay = paymentDay,
    monthlyPayment = monthlyPayment?.scaled,
    lifeInsuranceMonthly = lifeInsurance.scaled,
    otherCostsMonthly = otherCosts.scaled,
    amortization = amortization.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun LoanDetailsEntity.toDomain() =
    Loan(
        accountId = id,
        mode = LoanMode.valueOf(mode),
        paymentDay = paymentDay,
        disbursementAccountId = disbursementAccountId,
        principal = principal?.let(::Money),
        installmentsCount = installmentsCount,
        rate = rate?.let(::BigDecimal),
        rateType = rateType?.let(RateType::valueOf),
        firstDueDate = firstDueDate,
        monthlyPayment = monthlyPayment?.let(::Money),
        lifeInsurance = Money(lifeInsuranceMonthly),
        otherCosts = Money(otherCostsMonthly),
        amortization = Amortization.valueOf(amortization),
    )

internal fun LoanInstallment.toEntity(
    createdAt: Instant,
    updatedAt: Instant,
) = InstallmentEntity(
    id = id,
    loanAccountId = loanId,
    number = number,
    dueDate = dueDate,
    principal = principal.scaled,
    interest = interest.scaled,
    insurance = insurance.scaled,
    otherCosts = otherCosts.scaled,
    status = status.name,
    paidAt = paidAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InstallmentEntity.toLoanInstallment() =
    LoanInstallment(
        id = id,
        loanId = loanAccountId!!,
        number = number,
        dueDate = dueDate,
        principal = Money(principal),
        interest = Money(interest),
        insurance = Money(insurance),
        otherCosts = Money(otherCosts),
        status = InstallmentStatus.valueOf(status),
        paidAt = paidAt,
    )

/** user_settings; homeCards is stored as the contract's JSON array. */
class SettingsRepositoryImpl
    @Inject
    constructor(
        private val dao: SettingsDao,
        private val clock: Clock,
    ) : SettingsRepository {
        override fun observe(): Flow<Settings> =
            dao.observe().map { row ->
                row?.let {
                    Settings(
                        it.reminderLeadDays,
                        it.notificationsEnabled,
                        it.homeBalanceView,
                        it.amountDecimals,
                        homeCards(it.homeCards),
                        netBalanceModeOf(it.netBalanceMode),
                        it.loanPaymentCategoryId,
                        it.notifyDrafts,
                    )
                } ?: Settings()
            }

        override suspend fun update(settings: Settings) {
            val stored = dao.observe().first() ?: return
            dao.upsert(
                stored.copy(
                    reminderLeadDays = settings.reminderLeadDays,
                    notificationsEnabled = settings.notificationsEnabled,
                    homeBalanceView = settings.homeBalanceView,
                    amountDecimals = settings.amountDecimals,
                    homeCards = homeCardsJson(settings.homeCards),
                    netBalanceMode = settings.netBalanceMode.name,
                    loanPaymentCategoryId = settings.loanPaymentCategoryId,
                    notifyDrafts = settings.notifyDrafts,
                    updatedAt = clock.now(),
                    syncState = SyncState.PENDING,
                    syncError = null,
                ),
            )
        }
    }

/** An unknown mode reads as PERIOD; TOTAL_DEBT, from before 1.6.0, is CUSTOM. */
internal fun netBalanceModeOf(name: String): NetBalanceMode =
    when (name) {
        "CUSTOM", "TOTAL_DEBT" -> NetBalanceMode.CUSTOM
        else -> NetBalanceMode.PERIOD
    }

internal fun Recurring.toEntity(
    createdAt: Instant,
    updatedAt: Instant,
) = RecurringExpenseEntity(
    id = id,
    name = name,
    estimatedAmount = estimatedAmount.scaled,
    lastPaidAmount = lastPaidAmount?.scaled,
    accountId = accountId,
    categoryId = categoryId,
    startDate = startDate,
    frequency = frequency.name,
    intervalCount = intervalCount,
    intervalUnit = intervalUnit?.name,
    nextDueDate = nextDueDate,
    lastNotifiedOn = lastNotifiedOn,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun RecurringExpenseEntity.toDomain() =
    Recurring(
        id = id,
        name = name,
        estimatedAmount = Money(estimatedAmount),
        accountId = accountId,
        categoryId = categoryId,
        startDate = startDate,
        frequency = Frequency.valueOf(frequency),
        nextDueDate = nextDueDate,
        intervalCount = intervalCount,
        intervalUnit = intervalUnit?.let(IntervalUnit::valueOf),
        lastPaidAmount = lastPaidAmount?.let(::Money),
        lastNotifiedOn = lastNotifiedOn,
        active = active,
    )

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
    tagIds = encodeTagIds(tagIds),
    note = note,
    installments = installments,
    principal = principal?.scaled,
    interest = interest?.scaled,
    fees = fees?.scaled,
    installmentId = installmentId,
    recurringId = recurringId,
    paymentKind = paymentKind,
    concept = concept,
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
    includeInNet = includeInNet,
    lastDigits = lastDigits?.takeIf { it.length == 4 },
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
        includeInNet,
        lastDigits,
    )

internal fun CategoryEntity.toDomain() =
    Category(id, name, CategoryKind.valueOf(kind), parentId, isBase, systemKey, icon, sortOrder, excludeFromReports, color)

internal fun TransactionEntity.toDomain() =
    Transaction(
        id = id,
        type = TransactionType.valueOf(type),
        amount = Money(amount),
        occurredAt = occurredAt,
        accountId = accountId,
        destinationAccountId = destinationAccountId,
        categoryId = categoryId,
        tagIds = decodeTagIds(tagIds),
        note = note,
        installments = installments,
        principal = principal?.let(::Money),
        interest = interest?.let(::Money),
        fees = fees?.let(::Money),
        installmentId = installmentId,
        recurringId = recurringId,
        paymentKind = paymentKind,
        concept = concept,
        createdAt = createdAt,
        pendingSync = syncState != SyncState.SYNCED,
    )

@Serializable
private data class HomeCardJson(
    val key: String,
    val visible: Boolean,
)

private val cardsJson = Json { ignoreUnknownKeys = true }

/** An unreadable value falls back to the default order rather than breaking Home. */
internal fun homeCards(text: String): List<HomeCard> =
    runCatching { cardsJson.decodeFromString<List<HomeCardJson>>(text) }
        .getOrDefault(emptyList())
        .map { HomeCard(it.key, it.visible) }

internal fun homeCardsJson(cards: List<HomeCard>): String = cardsJson.encodeToString(cards.map { HomeCardJson(it.key, it.visible) })

private const val NOTE_CANDIDATES = 100

/**
 * S15-DOM01: exact, then prefix, then start of a word, then contains; ties by use and then by date (the rows come latest
 * first). Notes that differ only in case are one suggestion, the most recent.
 */
internal fun rankNotes(
    text: String,
    rows: List<NoteRow>,
): List<NoteSuggestion> {
    val query = text.trim().lowercase()

    fun rank(note: String): Int {
        val n = note.lowercase()
        return when {
            n == query -> 0
            n.startsWith(query) -> 1
            n.split(' ', '-', '/').any { it.startsWith(query) } -> 2
            else -> 3
        }
    }
    return rows
        .groupBy { it.note.lowercase() }
        .values
        .map { group -> group.first() to group.sumOf { it.uses } }
        .sortedWith(compareBy({ rank(it.first.note) }, { -it.second }))
        .map { (row, _) -> NoteSuggestion(row.note, row.categoryId, row.accountId) }
}

class DraftRepositoryImpl
    @Inject
    constructor(
        private val db: FinanceDatabase,
        private val dao: DraftDao,
        private val transactions: TransactionRepository,
        private val clock: Clock,
    ) : DraftRepository {
        override fun observeAll(): Flow<List<TransactionDraft>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

        override suspend fun get(id: UUID): TransactionDraft? = dao.get(id)?.toDomain()

        override suspend fun count(): Int = dao.count()

        override suspend fun apply(
            draftId: UUID,
            transaction: Transaction,
        ) = db.withTransaction {
            transactions.insert(transaction)
            dao.delete(draftId, clock.now())
        }

        override suspend fun discard(id: UUID) = dao.delete(id, clock.now())
    }

internal fun TransactionDraftEntity.toDomain() =
    TransactionDraft(
        id = id,
        source = DraftSource.valueOf(source),
        type = TransactionType.valueOf(type),
        amount = Money(amount),
        occurredAt = occurredAt,
        merchant = merchant,
        lastDigits = lastDigits,
    )
