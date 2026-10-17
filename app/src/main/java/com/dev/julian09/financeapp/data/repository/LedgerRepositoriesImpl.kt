package com.dev.julian09.financeapp.data.repository

import com.dev.julian09.financeapp.data.local.dao.AccountDao
import com.dev.julian09.financeapp.data.local.dao.CategoryDao
import com.dev.julian09.financeapp.data.local.dao.TransactionDao
import com.dev.julian09.financeapp.data.local.entity.AccountEntity
import com.dev.julian09.financeapp.data.local.entity.CategoryEntity
import com.dev.julian09.financeapp.data.local.entity.TransactionEntity
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AccountRepositoryImpl
    @Inject
    constructor(
        private val dao: AccountDao,
    ) : AccountRepository {
        override fun observeActive(): Flow<List<Account>> = dao.observeActive().map { rows -> rows.map { it.toDomain() } }
    }

class CategoryRepositoryImpl
    @Inject
    constructor(
        private val dao: CategoryDao,
    ) : CategoryRepository {
        override fun observeAll(): Flow<List<Category>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
    }

class TransactionRepositoryImpl
    @Inject
    constructor(
        private val dao: TransactionDao,
    ) : TransactionRepository {
        override fun observeRecent(limit: Int): Flow<List<Transaction>> =
            dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }
    }

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
    )
