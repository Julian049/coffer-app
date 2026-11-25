package com.dev.julian09.financeapp.ui.feature.mail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.finance.normalizeMerchant
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.DraftRule
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RuleLevel
import com.dev.julian09.financeapp.domain.model.RuleOrigin
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.DraftRuleRepository
import com.dev.julian09.financeapp.domain.usecase.DeleteDraftRule
import com.dev.julian09.financeapp.domain.usecase.DraftRuleInput
import com.dev.julian09.financeapp.domain.usecase.MerchantSuggestions
import com.dev.julian09.financeapp.domain.usecase.SaveDraftRule
import com.dev.julian09.financeapp.domain.usecase.SaveRuleResult
import com.dev.julian09.financeapp.domain.usecase.isPickable
import com.dev.julian09.financeapp.ui.feature.accounts.digitsOnly
import com.dev.julian09.financeapp.ui.feature.accounts.wholePesos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class DraftRuleFormState(
    val loaded: Boolean = false,
    /** The stored rule when editing; null = Nueva regla. */
    val editing: DraftRule? = null,
    val merchant: String = "",
    /** Monto «Exacto» (else «Cualquiera») and its whole pesos as digits. */
    val exactAmount: Boolean = false,
    val amountDigits: String = "",
    /** Cuenta «Una cuenta» (else «Cualquiera»). */
    val oneAccount: Boolean = false,
    val accountId: UUID? = null,
    val note: String = "",
    val categoryId: UUID? = null,
    /** Every spelling of the merchant seen in the emails (changes-ui-2.5 §3). */
    val aliases: List<String> = emptyList(),
    /** Recent merchants without a rule; only in Nueva regla. */
    val suggestions: List<String> = emptyList(),
    /** Nueva regla · Ya existe (`259:676`): the rule with the same conditions. */
    val duplicate: DraftRule? = null,
    val confirmingDelete: Boolean = false,
    /** Saved or deleted: the screen goes back. */
    val done: Boolean = false,
) {
    val canSave: Boolean
        get() =
            duplicate == null &&
                merchant.isNotBlank() &&
                note.isNotBlank() &&
                categoryId != null &&
                (!exactAmount || amountDigits.isNotEmpty()) &&
                (!oneAccount || accountId != null)

    /** A probable rule turns sure (and the user's) when saved (PO, 2026-11-23). */
    val turnsSure: Boolean get() = editing?.level == RuleLevel.PROBABLE

    /** Why Coffer made it, on top of Editar regla (`259:807`, `261:676`); null for the user's rules. */
    val learnedNotice: String?
        get() =
            editing?.takeIf { it.origin == RuleOrigin.LEARNED }?.let {
                if (it.level == RuleLevel.SURE) {
                    "La creó Coffer porque registraste este movimiento igual 3 veces seguidas."
                } else {
                    "La creó Coffer porque registraste este movimiento igual 2 veces. Por eso queda por revisar."
                }
            }
}

/** What the rule form chooses from: active accounts that take movements and expense categories by use. */
data class DraftRuleChoices(
    val accounts: List<AccountBalance> = emptyList(),
    val categories: List<Category> = emptyList(),
    val usage: Map<UUID, Int> = emptyMap(),
)

/** Nueva regla (`258:676`) and Editar regla (`259:807`, `261:676`); the route's id decides which. */
@HiltViewModel
class DraftRuleFormViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        accounts: AccountRepository,
        categories: CategoryRepository,
        private val rules: DraftRuleRepository,
        private val merchants: MerchantSuggestions,
        private val save: SaveDraftRule,
        private val delete: DeleteDraftRule,
        clock: Clock,
    ) : ViewModel() {
        private val _state = MutableStateFlow(DraftRuleFormState())
        val state: StateFlow<DraftRuleFormState> = _state.asStateFlow()

        val choices: StateFlow<DraftRuleChoices> =
            combine(
                accounts.observeBalances(),
                categories.observeAll(),
                categories.observeTotals(Instant.EPOCH, clock.now().plus(Duration.ofDays(1))),
            ) { accs, cats, totals ->
                DraftRuleChoices(
                    accs.filter { it.account.active && it.account.type.isPickable },
                    // Incomes stay out of the rules for now (PO, 2.5)
                    cats.filter { it.isPickable && it.kind == CategoryKind.EXPENSE },
                    totals.associate { it.categoryId to it.count },
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, DraftRuleChoices())

        private var aliasJob: Job? = null

        init {
            viewModelScope.launch {
                val rule = savedState.get<String>("id")?.let { rules.get(UUID.fromString(it)) }
                _state.value =
                    if (rule == null) {
                        DraftRuleFormState(loaded = true, suggestions = merchants())
                    } else {
                        DraftRuleFormState(
                            loaded = true,
                            editing = rule,
                            merchant = rule.merchant,
                            exactAmount = rule.amount != null,
                            amountDigits = rule.amount?.wholePesos().orEmpty(),
                            oneAccount = rule.accountId != null,
                            accountId = rule.accountId,
                            note = rule.note,
                            categoryId = rule.categoryId,
                        )
                    }
                refreshAliases()
            }
        }

        fun merchant(value: String) {
            _state.update { it.copy(merchant = value, duplicate = null) }
            refreshAliases()
        }

        fun exactAmount(on: Boolean) = _state.update { it.copy(exactAmount = on, duplicate = null) }

        fun amount(text: String) = _state.update { it.copy(amountDigits = digitsOnly(text), duplicate = null) }

        fun oneAccount(on: Boolean) = _state.update { it.copy(oneAccount = on, duplicate = null) }

        fun account(id: UUID) = _state.update { it.copy(accountId = id, duplicate = null) }

        fun note(value: String) = _state.update { it.copy(note = value) }

        fun category(id: UUID) = _state.update { it.copy(categoryId = id) }

        fun requestDelete() = _state.update { it.copy(confirmingDelete = true) }

        fun dismissDelete() = _state.update { it.copy(confirmingDelete = false) }

        fun confirmDelete() {
            val id = _state.value.editing?.id ?: return
            _state.update { it.copy(confirmingDelete = false) }
            viewModelScope.launch {
                delete(id)
                _state.update { it.copy(done = true) }
            }
        }

        fun saveRule() {
            val s = _state.value
            val category = s.categoryId ?: return
            if (!s.canSave) return
            val input =
                DraftRuleInput(
                    id = s.editing?.id,
                    merchant = s.merchant,
                    amount = if (s.exactAmount) Money.ofPesos(s.amountDigits.toLong()) else null,
                    accountId = if (s.oneAccount) s.accountId else null,
                    note = s.note,
                    categoryId = category,
                )
            viewModelScope.launch {
                when (val result = save(input)) {
                    is SaveRuleResult.Saved -> _state.update { it.copy(done = true) }
                    is SaveRuleResult.Duplicate -> _state.update { it.copy(duplicate = result.existing) }
                    SaveRuleResult.Invalid -> Unit
                }
            }
        }

        private fun refreshAliases() {
            val key = normalizeMerchant(_state.value.merchant)
            aliasJob?.cancel()
            aliasJob =
                viewModelScope.launch {
                    val aliases = if (key.isEmpty()) emptyList() else merchants.merchantAliases(key)
                    _state.update { it.copy(aliases = aliases) }
                }
        }
    }

/** «En tus correos aparece como «A», «B» y «C».»; null when there is nothing new to say. */
internal fun aliasesLine(
    typed: String,
    aliases: List<String>,
): String? {
    val shown = aliases.filter { it != typed.trim() }.map { "«$it»" }
    if (shown.isEmpty()) return null
    val list = if (shown.size == 1) shown[0] else shown.dropLast(1).joinToString(", ") + " y " + shown.last()
    return "En tus correos aparece como $list."
}
