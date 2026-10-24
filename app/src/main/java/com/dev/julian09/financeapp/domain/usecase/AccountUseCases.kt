package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.RemoveMode
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

private const val ACCOUNT_NAME_MAX = 60

/** Accounts created by CreateAccount; a card goes through CreateCard (S7) and loans arrive in S10. */
val AVAILABLE_ACCOUNT_TYPES = listOf(AccountType.CASH, AccountType.SAVINGS)

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
            // S12-H03: the opening balance arrives as a «Saldo inicial» movement
            repository.moveOpeningBalances()
            scheduler.requestSync()
            return null
        }
    }

/** S12-DAT02, S15-DAT03: on every start, asset accounts that still keep their opening balance as a field move it to a movement. */
class MoveOpeningBalances
    @Inject
    constructor(
        private val repository: AccountRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke() {
            val moved = repository.moveOpeningBalances()
            // S15-DAT03: and «Deudas» for the installments of loans
            val ensured = repository.ensureLoanCategory()
            if (moved || ensured) scheduler.requestSync()
        }
    }

/** REQ-CTA-02, 07: name, icon, color and "include in total"; the type and the opening balance never change (S12-H02). */
class UpdateAccount
    @Inject
    constructor(
        private val repository: AccountRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(
            id: UUID,
            name: String,
            includeInTotal: Boolean,
            icon: String?,
            color: String?,
        ): LedgerError? {
            nameError(name)?.let { return it }
            val current =
                repository
                    .observeBalances()
                    .first()
                    .first { it.account.id == id }
                    .account
            repository.update(current.copy(name = name.trim(), includeInTotal = includeInTotal, icon = icon, color = color))
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

/** S15-DOM07: marks or unmarks a card or loan in the custom net balance («Elegir deudas»). */
class SetIncludeInNet
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(
            accountId: UUID,
            include: Boolean,
        ) {
            val account =
                accounts
                    .observeBalances()
                    .first()
                    .firstOrNull { it.account.id == accountId }
                    ?.account ?: return
            if (account.includeInNet == include) return
            accounts.update(account.copy(includeInNet = include))
            scheduler.requestSync()
        }
    }
