package com.dev.julian09.financeapp.ui.feature.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.CardOverview
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.TagRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import com.dev.julian09.financeapp.domain.usecase.CreateTag
import com.dev.julian09.financeapp.domain.usecase.DeferCardPurchase
import com.dev.julian09.financeapp.domain.usecase.ObserveCards
import com.dev.julian09.financeapp.domain.usecase.RegisterTransaction
import com.dev.julian09.financeapp.domain.usecase.TransactionErrors
import com.dev.julian09.financeapp.domain.usecase.UpdateTransaction
import com.dev.julian09.financeapp.ui.feature.accounts.digitsOnly
import com.dev.julian09.financeapp.ui.feature.accounts.wholePesos
import com.dev.julian09.financeapp.ui.feature.cards.parsePercent
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
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** REQ-TC-05: from 1 to 72 installments (deferred_purchases ck_dp_count). */
const val MAX_INSTALLMENTS = 72

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
    /** A card purchase (S7-UI02): 1 by default (RNF-15). */
    val installments: Int = 1,
    /** Null: the card's own rate and type (REQ-TC-06). */
    val rateText: String? = null,
    val rateType: RateType? = null,
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
    /** Credit cards by account id, with their available credit. */
    val cards: Map<UUID, CardOverview> = emptyMap(),
) {
    /** "Condiciones del crédito" show for a new expense on a card. */
    fun cardOf(state: TransactionFormState): CardOverview? =
        cards[state.accountId]?.takeIf { state.type == TransactionType.EXPENSE && state.editingId == null }
}

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
        private val deferCardPurchase: DeferCardPurchase,
        observeCards: ObserveCards,
        clock: Clock,
    ) : ViewModel() {
        /** The stored movement being edited: what the form does not show is kept as it was. */
        private var original: Transaction? = null

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
            combine(accounts.observeBalances(), categories.observeAll(), tags.observeAll(), observeCards()) { accs, cats, tgs, cards ->
                TransactionChoices(accs.filter { it.account.active }, cats, tgs, cards.associateBy { it.card.accountId })
            }.stateIn(viewModelScope, SharingStarted.Eagerly, TransactionChoices())

        init {
            viewModelScope.launch {
                val id = _state.value.editingId
                if (id != null) {
                    transactions.observe(id).first()?.let { t ->
                        original = t
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

        /** Another account starts the card conditions over: 1 installment and the card's rate. */
        fun setAccount(id: UUID) = edit { it.copy(accountId = id, installments = 1, rateText = null, rateType = null) }

        fun setInstallments(count: Int) = edit { it.copy(installments = count.coerceIn(1, MAX_INSTALLMENTS)) }

        fun setRate(text: String) = edit { it.copy(rateText = text.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(8)) }

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
            val kept = original
            val base =
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
            // An edit keeps what the form does not show, like the recurring expense a payment came from
            val transaction =
                kept?.let {
                    base.copy(installments = it.installments, recurringId = it.recurringId, installmentId = it.installmentId)
                } ?: base
            val card = choices.value.cardOf(s)
            viewModelScope.launch {
                val errors =
                    when {
                        card != null ->
                            deferCardPurchase(
                                transaction,
                                s.installments,
                                // Text that is not a number is a negative rate, which the use case refuses with its message
                                if (s.rateText == null) card.card.rate else parsePercent(s.rateText) ?: BigDecimal(-1),
                                s.rateType ?: card.card.rateType,
                            )
                        s.editingId == null -> register(transaction)
                        else -> update(transaction)
                    }
                _state.update { if (errors.isEmpty()) it.copy(saved = true) else it.copy(errors = errors) }
            }
        }
    }
