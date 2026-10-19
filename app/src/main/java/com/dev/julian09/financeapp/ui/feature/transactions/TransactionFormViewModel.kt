package com.dev.julian09.financeapp.ui.feature.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.TagRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import com.dev.julian09.financeapp.domain.usecase.CreateTag
import com.dev.julian09.financeapp.domain.usecase.RegisterTransaction
import com.dev.julian09.financeapp.domain.usecase.TransactionErrors
import com.dev.julian09.financeapp.domain.usecase.UpdateTransaction
import com.dev.julian09.financeapp.ui.feature.accounts.digitsOnly
import com.dev.julian09.financeapp.ui.feature.accounts.wholePesos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class TransactionFormState(
    val editingId: UUID? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    /** Whole pesos as typed, digits only. */
    val amountDigits: String = "",
    val categoryId: UUID? = null,
    val accountId: UUID? = null,
    val destinationId: UUID? = null,
    val occurredAt: Instant,
    val tagId: UUID? = null,
    val note: String = "",
    val errors: TransactionErrors = emptyMap(),
    val tagError: LedgerError? = null,
    /** Bumps on each tag created inline, so the dialog knows to close. */
    val createdTags: Int = 0,
    val saved: Boolean = false,
)

/** What the form chooses from: active accounts with their balance, categories and tags. */
data class TransactionChoices(
    val accounts: List<AccountBalance> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
)

/** Nueva transacción, Nueva transferencia and the edit of S5-H03; the route's type and id decide which. */
@HiltViewModel
class TransactionFormViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        accounts: AccountRepository,
        categories: CategoryRepository,
        private val tags: TagRepository,
        private val transactions: TransactionRepository,
        private val register: RegisterTransaction,
        private val update: UpdateTransaction,
        private val createTag: CreateTag,
        clock: Clock,
    ) : ViewModel() {
        private val _state =
            MutableStateFlow(
                TransactionFormState(
                    editingId = savedState.get<String>("id")?.let(UUID::fromString),
                    type = savedState.get<String>("type")?.let(TransactionType::valueOf) ?: TransactionType.EXPENSE,
                    // REQ-TRX-05: now, editable
                    occurredAt = clock.now(),
                ),
            )
        val state: StateFlow<TransactionFormState> = _state.asStateFlow()

        val choices: StateFlow<TransactionChoices> =
            combine(accounts.observeBalances(), categories.observeAll(), tags.observeAll()) { accs, cats, tgs ->
                TransactionChoices(accs.filter { it.account.active }, cats, tgs)
            }.stateIn(viewModelScope, SharingStarted.Eagerly, TransactionChoices())

        init {
            viewModelScope.launch {
                val id = _state.value.editingId
                if (id != null) {
                    transactions.observe(id).first()?.let { t ->
                        _state.update {
                            it.copy(
                                type = t.type,
                                amountDigits = t.amount.wholePesos(),
                                categoryId = t.categoryId,
                                accountId = t.accountId,
                                destinationId = t.destinationAccountId,
                                occurredAt = t.occurredAt,
                                tagId = t.tagId,
                                note = t.note.orEmpty(),
                            )
                        }
                    }
                } else {
                    // SEQ-04: the last account used, else the first active one
                    val account =
                        transactions.lastUsedAccountId() ?: accounts
                            .observeActive()
                            .first()
                            .firstOrNull()
                            ?.id
                    _state.update { it.copy(accountId = account) }
                }
            }
        }

        private fun edit(transform: (TransactionFormState) -> TransactionFormState) =
            _state.update { transform(it).copy(errors = emptyMap()) }

        /** Switching between expense and income drops a category of the other kind. */
        fun setType(value: TransactionType) = edit { if (it.type == value) it else it.copy(type = value, categoryId = null) }

        fun setAmount(text: String) = edit { it.copy(amountDigits = digitsOnly(text)) }

        fun setCategory(id: UUID) = edit { it.copy(categoryId = id) }

        fun setAccount(id: UUID) = edit { it.copy(accountId = id) }

        fun setDestination(id: UUID) = edit { it.copy(destinationId = id) }

        fun swapAccounts() = edit { it.copy(accountId = it.destinationId, destinationId = it.accountId) }

        fun setOccurredAt(value: Instant) = edit { it.copy(occurredAt = value) }

        /** At most one tag (REQ-ETQ-02): tapping the chosen one clears it. */
        fun toggleTag(id: UUID) = edit { it.copy(tagId = if (it.tagId == id) null else id) }

        fun setNote(value: String) = edit { it.copy(note = value) }

        fun newTag(name: String) {
            viewModelScope.launch {
                val tag = Tag(UUID.randomUUID(), name)
                val error = createTag(tag)
                _state.update {
                    if (error ==
                        null
                    ) {
                        it.copy(tagId = tag.id, tagError = null, createdTags = it.createdTags + 1)
                    } else {
                        it.copy(tagError = error)
                    }
                }
            }
        }

        fun clearTagError() = _state.update { it.copy(tagError = null) }

        fun save() {
            val s = _state.value
            val transaction =
                Transaction(
                    id = s.editingId ?: UUID.randomUUID(),
                    type = s.type,
                    amount = Money.ofPesos(s.amountDigits.ifEmpty { "0" }.toLong()),
                    occurredAt = s.occurredAt,
                    accountId = s.accountId ?: UUID(0, 0),
                    destinationAccountId = s.destinationId,
                    categoryId = s.categoryId,
                    tagId = s.tagId.takeIf { s.type != TransactionType.TRANSFER },
                    note = s.note,
                )
            viewModelScope.launch {
                val errors = if (s.editingId == null) register(transaction) else update(transaction)
                _state.update { if (errors.isEmpty()) it.copy(saved = true) else it.copy(errors = errors) }
            }
        }
    }
