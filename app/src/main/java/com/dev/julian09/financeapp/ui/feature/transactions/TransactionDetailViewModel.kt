package com.dev.julian09.financeapp.ui.feature.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.TagRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import com.dev.julian09.financeapp.domain.usecase.DeleteTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TransactionDetail(
    val transaction: Transaction,
    val category: Category?,
    /** "Comida > Cafetería". */
    val categoryLabel: String?,
    val accountName: String,
    val destinationName: String?,
    val tagName: String?,
)

/** Detalle de transacción (S5-UI04): what it was, and edit or delete it. */
@HiltViewModel
class TransactionDetailViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        transactions: TransactionRepository,
        accounts: AccountRepository,
        categories: CategoryRepository,
        tags: TagRepository,
        private val delete: DeleteTransaction,
    ) : ViewModel() {
        private val id: UUID = UUID.fromString(savedState.get<String>("id"))

        /** Null while loading and once deleted. */
        val detail: StateFlow<TransactionDetail?> =
            combine(
                transactions.observe(id),
                accounts.observeBalances(),
                categories.observeAll(),
                tags.observeAll(),
            ) { t, accs, cats, tgs ->
                t ?: return@combine null
                val names = accs.associate { it.account.id to it.account.name }
                val category = cats.firstOrNull { it.id == t.categoryId }
                val parent = category?.parentId?.let { p -> cats.firstOrNull { it.id == p } }
                TransactionDetail(
                    transaction = t,
                    category = category ?: parent,
                    categoryLabel = category?.name,
                    accountName = names[t.accountId] ?: "Cuenta",
                    destinationName = t.destinationAccountId?.let { names[it] ?: "Cuenta" },
                    tagName = tgs.firstOrNull { it.id == t.tagId }?.name,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private val _deleted = MutableStateFlow(false)
        val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

        fun confirmDelete() {
            viewModelScope.launch {
                delete(id)
                _deleted.value = true
            }
        }
    }
