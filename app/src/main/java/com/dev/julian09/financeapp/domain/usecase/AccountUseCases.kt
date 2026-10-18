package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RemoveMode
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

private const val ACCOUNT_NAME_MAX = 60

/** Cards and loans arrive in S7 and S10 (S4-UI01). */
val AVAILABLE_ACCOUNT_TYPES = listOf(AccountType.CASH, AccountType.DEBIT, AccountType.SAVINGS)

private fun nameError(name: String): LedgerError? =
    when {
        name.isBlank() -> LedgerError.NAME_REQUIRED
        name.trim().length > ACCOUNT_NAME_MAX -> LedgerError.NAME_TOO_LONG
        else -> null
    }

/** REQ-CTA-01: name, type and initial balance; written PENDING and synced. */
class CreateAccount
    @Inject
    constructor(
        private val repository: AccountRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(account: Account): LedgerError? {
            nameError(account.name)?.let { return it }
            if (account.type !in AVAILABLE_ACCOUNT_TYPES) return LedgerError.TYPE_NOT_AVAILABLE
            val last = repository.observeBalances().first().maxOfOrNull { it.account.sortOrder } ?: -1
            repository.create(account.copy(name = account.name.trim(), sortOrder = last + 1))
            scheduler.requestSync()
            return null
        }
    }

/** REQ-CTA-02, 07: name, initial balance and "include in total"; the type never changes (S4-SRV01). */
class UpdateAccount
    @Inject
    constructor(
        private val repository: AccountRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(
            id: UUID,
            name: String,
            initialBalance: Money,
            includeInTotal: Boolean,
        ): LedgerError? {
            nameError(name)?.let { return it }
            val current =
                repository
                    .observeBalances()
                    .first()
                    .first { it.account.id == id }
                    .account
            repository.update(current.copy(name = name.trim(), initialBalance = initialBalance, includeInTotal = includeInTotal))
            scheduler.requestSync()
            return null
        }
    }

/** REQ-CTA-03 to 05, SEQ-12. */
class RemoveAccount
    @Inject
    constructor(
        private val repository: AccountRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(
            id: UUID,
            mode: RemoveMode,
            targetId: UUID? = null,
            confirmation: String = "",
        ): LedgerError? {
            val accounts = repository.observeBalances().first().map { it.account }
            val account = accounts.first { it.id == id }
            when (mode) {
                RemoveMode.DEACTIVATE -> repository.setActive(id, false)
                RemoveMode.MOVE -> {
                    targetId ?: return LedgerError.TARGET_REQUIRED
                    if (targetId == id || accounts.none { it.id == targetId && it.active }) return LedgerError.TARGET_INVALID
                    repository.moveAndDelete(id, targetId)
                }
                RemoveMode.DELETE_ALL -> {
                    // REQ-CTA-05: the user types the name, ignoring case and outer spaces
                    if (!confirmation.trim().equals(account.name.trim(), ignoreCase = true)) return LedgerError.CONFIRMATION_MISMATCH
                    repository.deleteWithTransactions(id)
                }
            }
            scheduler.requestSync()
            return null
        }
    }

/** REQ-CTA-04: back to Home, selectors and the total. */
class ReactivateAccount
    @Inject
    constructor(
        private val repository: AccountRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(id: UUID) {
            repository.setActive(id, true)
            scheduler.requestSync()
        }
    }
