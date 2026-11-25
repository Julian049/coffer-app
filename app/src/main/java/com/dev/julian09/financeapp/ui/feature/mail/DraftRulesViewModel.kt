package com.dev.julian09.financeapp.ui.feature.mail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.DraftRule
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.usecase.ObserveDraftRules
import com.dev.julian09.financeapp.domain.usecase.ReorderDraftRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject

/** A rule as Registro automático shows it: with its category and, when it asks for one, its account. */
data class DraftRuleRow(
    val rule: DraftRule,
    val category: Category?,
    val account: Account?,
)

data class DraftRulesState(
    val loaded: Boolean = false,
    val sure: List<DraftRuleRow> = emptyList(),
    val probable: List<DraftRuleRow> = emptyList(),
    /** Every category, so a subcategory without an icon takes its parent's. */
    val categories: List<Category> = emptyList(),
) {
    val summary: String get() = draftRulesSummary(sure.size, probable.size)
}

/** The subtitle of Ajustes → Registro automático (changes-ui-2.5 §1). */
fun draftRulesSummary(
    sure: Int,
    probable: Int,
): String {
    val total = sure + probable
    if (total == 0) return "Coffer las crea cuando un movimiento se repite"
    val rules = if (total == 1) "1 regla" else "$total reglas"
    return when {
        probable == 0 -> "$rules · ${if (total == 1) "entra" else "entran"} directo al historial"
        else -> "$rules · $sure directo, $probable por revisar"
    }
}

/** Registro automático (Figma `257:689`): the sure rules in the order the user drags, then the probable ones. */
@HiltViewModel
class DraftRulesViewModel
    @Inject
    constructor(
        observe: ObserveDraftRules,
        accounts: AccountRepository,
        categories: CategoryRepository,
        private val reorder: ReorderDraftRules,
    ) : ViewModel() {
        // The order while dragging, ahead of the database; null = as stored
        private val dragged = MutableStateFlow<List<UUID>?>(null)
        private val writing = Mutex()

        val state: StateFlow<DraftRulesState> =
            combine(observe(), accounts.observeBalances(), categories.observeAll(), dragged) { list, accs, cats, order ->
                val byAccount = accs.associate { it.account.id to it.account }
                val byCategory = cats.associateBy { it.id }

                fun row(r: DraftRule) = DraftRuleRow(r, byCategory[r.categoryId], r.accountId?.let(byAccount::get))
                val sure = order?.let { ids -> list.sure.sortedBy { ids.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } } }
                DraftRulesState(true, (sure ?: list.sure).map(::row), list.probable.map(::row), cats)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DraftRulesState())

        /** Each step of a drag shows at once; the writes run one at a time and each saves the latest order. */
        fun move(
            from: Int,
            to: Int,
        ) {
            val ids =
                state.value.sure
                    .map { it.rule.id }
                    .toMutableList()
            if (from !in ids.indices || to !in ids.indices) return
            ids.add(to, ids.removeAt(from))
            dragged.value = ids
            viewModelScope.launch(NonCancellable) {
                writing.withLock { dragged.value?.let { reorder(it) } }
            }
        }
    }
