package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

private const val CATEGORY_NAME_MAX = 50

/** The category the app files card interest under (REQ-TC-09). */
const val FINANCIAL_CHARGES = "FINANCIAL_CHARGES"

/** The category of the «Saldo inicial» movements (S12-H03): excluded from reports, never offered in a picker. */
const val OPENING_BALANCE = "OPENING_BALANCE"

val Category.isPickable: Boolean get() = systemKey != OPENING_BALANCE

/** Any other category, ignoring case: categories are flat outside Categorías and Reportes, so names are unique (S13). */
private fun nameError(
    name: String,
    siblings: List<Category>,
    except: UUID? = null,
): LedgerError? =
    when {
        name.isBlank() -> LedgerError.NAME_REQUIRED
        name.trim().length > CATEGORY_NAME_MAX -> LedgerError.NAME_TOO_LONG
        siblings.any { it.id != except && it.name.equals(name.trim(), ignoreCase = true) } -> LedgerError.NAME_TAKEN
        else -> null
    }

private fun List<Category>.siblingsOf(category: Category) = filter { it.kind == category.kind && it.parentId == category.parentId }

/** Where the movements of a deleted category can go: same kind, outside what is deleted; a main category only to main ones. */
fun deleteTargets(
    categories: List<Category>,
    id: UUID,
): List<Category> {
    val deleted = categories.first { it.id == id }
    return categories.filter {
        it.kind == deleted.kind && it.id != id && it.parentId != id && (deleted.parentId != null || it.parentId == null)
    }
}

/** REQ-CAT-02: one level of subcategories with the kind of their parent. */
class CreateCategory
    @Inject
    constructor(
        private val repository: CategoryRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(category: Category): LedgerError? {
            val all = repository.observeAll().first()
            category.parentId?.let { parentId ->
                val parent = all.firstOrNull { it.id == parentId } ?: return LedgerError.TARGET_INVALID
                if (parent.parentId != null) return LedgerError.ONE_LEVEL_ONLY
                if (parent.kind != category.kind) return LedgerError.KIND_MISMATCH
            }
            nameError(category.name, all)?.let { return it }
            val last = all.siblingsOf(category).maxOfOrNull { it.sortOrder } ?: -1
            repository.create(category.copy(name = category.name.trim(), sortOrder = last + 1))
            scheduler.requestSync()
            return null
        }
    }

class RenameCategory
    @Inject
    constructor(
        private val repository: CategoryRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(
            id: UUID,
            name: String,
        ): LedgerError? {
            val all = repository.observeAll().first()
            val category = all.first { it.id == id }
            nameError(name, all, except = id)?.let { return it }
            repository.rename(id, name.trim())
            scheduler.requestSync()
            return null
        }
    }

/**
 * REQ-CAT-02, 03: subcategories go with their parent, and all their movements move to [targetId] itself.
 * "Intereses y cargos financieros" stays while a card or a loan could need it (PO, 2026-10-17).
 */
class DeleteCategory
    @Inject
    constructor(
        private val repository: CategoryRepository,
        private val accounts: AccountRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(
            id: UUID,
            targetId: UUID?,
        ): LedgerError? {
            val all = repository.observeAll().first()
            val subtree = all.filter { it.id == id || it.parentId == id }
            if (subtree.any { it.systemKey == FINANCIAL_CHARGES } && hasDebtAccounts()) return LedgerError.SYSTEM_CATEGORY_IN_USE
            if (targetId == null) {
                if (repository.transactionCount(id) > 0) return LedgerError.TARGET_REQUIRED
            } else if (deleteTargets(all, id).none { it.id == targetId }) {
                return LedgerError.TARGET_INVALID
            }
            repository.delete(id, targetId)
            scheduler.requestSync()
            return null
        }

        private suspend fun hasDebtAccounts() = accounts.observeBalances().first().any { it.account.type.isDebt }
    }
