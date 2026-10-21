package com.dev.julian09.financeapp.ui.feature.changes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.ChangeAction
import com.dev.julian09.financeapp.domain.model.ChangeEntry
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.usecase.ChangeLine
import com.dev.julian09.financeapp.domain.usecase.ObserveChangeHistory
import com.dev.julian09.financeapp.domain.usecase.RestoreTransaction
import com.dev.julian09.financeapp.domain.usecase.RevertEdit
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.feature.history.typeLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/** One row of Historial de cambios, with its texts already built. */
data class ChangeRow(
    val id: UUID,
    val action: ChangeAction,
    val title: String,
    val detail: String,
    val canRestore: Boolean = false,
    val canRevert: Boolean = false,
)

data class ChangeDay(
    val date: LocalDate,
    val rows: List<ChangeRow>,
)

/** Historial de cambios (S11-UI01): REQ-HIS-02, 03. */
@HiltViewModel
class ChangeHistoryViewModel
    @Inject
    constructor(
        observe: ObserveChangeHistory,
        accounts: AccountRepository,
        categories: CategoryRepository,
        private val restore: RestoreTransaction,
        private val revert: RevertEdit,
        private val clock: Clock,
    ) : ViewModel() {
        private val lines: StateFlow<List<ChangeLine>> = observe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        val days: StateFlow<List<ChangeDay>?> =
            combine(observe(), accounts.observeBalances(), categories.observeAll()) { list, balances, cats ->
                changeDays(list, balances.associate { it.account.id to it.account.name }, cats, clock.zone())
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private val _failed = MutableStateFlow(false)

        /** A restore or a revert could not run: the account or the category is gone. */
        val failed: StateFlow<Boolean> = _failed.asStateFlow()

        fun undo(id: UUID) {
            val line = lines.value.firstOrNull { it.entry.id == id } ?: return
            viewModelScope.launch {
                val errors =
                    when {
                        line.canRestore -> restore(line.entry)
                        line.canRevert -> revert(line.entry)
                        else -> emptyMap()
                    }
                _failed.value = errors.isNotEmpty()
            }
        }

        fun dismissFailure() {
            _failed.value = false
        }
    }

private val TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-CO"))

/** Newest first by local day: what happened to which movement, and what changed. */
fun changeDays(
    lines: List<ChangeLine>,
    accountNames: Map<UUID, String>,
    categories: List<Category>,
    zone: ZoneId = ZoneId.systemDefault(),
): List<ChangeDay> {
    val byId = categories.associateBy { it.id }

    fun name(t: Transaction) = t.note ?: t.categoryId?.let(byId::get)?.name ?: typeLabel(t.type)

    fun amount(t: Transaction) =
        formatMoney(
            t.amount,
            when (t.type) {
                TransactionType.INCOME -> MoneyKind.Income
                TransactionType.EXPENSE -> MoneyKind.Expense
                TransactionType.TRANSFER -> MoneyKind.Transfer
            },
        )

    fun account(id: UUID?) = id?.let { accountNames[it] } ?: "Cuenta"

    fun changes(
        before: Transaction,
        after: Transaction,
    ) = buildList {
        if (before.amount != after.amount) add("Monto: ${formatMoney(before.amount)} → ${formatMoney(after.amount)}")
        if (before.accountId != after.accountId) add("Cuenta: ${account(before.accountId)} → ${account(after.accountId)}")
        if (before.categoryId != after.categoryId) {
            add("Categoría: ${before.categoryId?.let(byId::get)?.name ?: "—"} → ${after.categoryId?.let(byId::get)?.name ?: "—"}")
        }
        if (before.occurredAt != after.occurredAt) add("Fecha")
        if (before.note != after.note) add("Nota")
        if (before.tagId != after.tagId) add("Etiqueta")
    }.ifEmpty { listOf("Sin cambios visibles") }

    return lines
        .groupBy {
            it.entry.changedAt
                .atZone(zone)
                .toLocalDate()
        }.map { (date, dayLines) ->
            ChangeDay(
                date,
                dayLines.map { line ->
                    val e: ChangeEntry = line.entry
                    val movement = (e.after ?: e.before)!!
                    // "10:42 a. m." never splits across lines
                    val time = TIME.format(e.changedAt.atZone(zone)).lowercase().replace(' ', '\u00A0')
                    val verb =
                        when (e.action) {
                            ChangeAction.CREATE -> "Creaste"
                            ChangeAction.UPDATE -> "Editaste"
                            ChangeAction.DELETE -> "Eliminaste"
                            ChangeAction.RESTORE -> "Restauraste"
                        }
                    val what =
                        if (e.action == ChangeAction.UPDATE && e.before != null && e.after != null) {
                            changes(e.before, e.after)
                        } else {
                            listOf(amount(movement), account(movement.accountId))
                        }
                    ChangeRow(
                        id = e.id,
                        action = e.action,
                        title = "$verb «${name(movement)}»",
                        detail = (what + time).joinToString(" · "),
                        canRestore = line.canRestore,
                        canRevert = line.canRevert,
                    )
                },
            )
        }
}
