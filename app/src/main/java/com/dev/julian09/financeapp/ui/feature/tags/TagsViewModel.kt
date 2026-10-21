package com.dev.julian09.financeapp.ui.feature.tags

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.model.TagTotal
import com.dev.julian09.financeapp.domain.model.TransactionFilter
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.TagRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import com.dev.julian09.financeapp.domain.usecase.CreateTag
import com.dev.julian09.financeapp.domain.usecase.DeleteTag
import com.dev.julian09.financeapp.domain.usecase.PinTag
import com.dev.julian09.financeapp.domain.usecase.RenameTag
import com.dev.julian09.financeapp.ui.feature.history.HistoryDay
import com.dev.julian09.financeapp.ui.feature.history.buildDays
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/** A tag with what moved under it this month (list and Home). */
data class TagLine(
    val tag: Tag,
    val total: TagTotal?,
) {
    val net: Money get() = total?.net ?: Money.ZERO
    val count: Int get() = total?.count ?: 0
}

/** Name and pin of the "Nueva etiqueta" or "Editar etiqueta" sheet. */
data class TagSheet(
    val editing: Tag? = null,
    val name: String = editing?.name.orEmpty(),
    val pinned: Boolean = editing?.pinned ?: false,
    val error: LedgerError? = null,
)

internal fun monthBounds(
    month: YearMonth,
    zone: ZoneId,
) = month.atDay(1).atStartOfDay(zone).toInstant() to
    month
        .plusMonths(1)
        .atDay(1)
        .atStartOfDay(zone)
        .toInstant()

/** The tags with their totals of [month]; pinned first, like the list query (REQ-ETQ-04). */
internal fun tagLines(
    tags: TagRepository,
    month: YearMonth,
    zone: ZoneId,
): Flow<List<TagLine>> {
    val (from, to) = monthBounds(month, zone)
    return combine(tags.observeAll(), tags.observeTotals(from, to)) { all, totals ->
        val byTag = totals.associateBy { it.tagId }
        all.map { TagLine(it, byTag[it.id]) }
    }
}

/** Etiquetas (S9-UI02): every tag with its month total and the pin. */
@HiltViewModel
class TagsViewModel
    @Inject
    constructor(
        tags: TagRepository,
        private val createTag: CreateTag,
        private val pinTag: PinTag,
        clock: Clock,
    ) : ViewModel() {
        val lines: StateFlow<List<TagLine>?> =
            tagLines(tags, YearMonth.from(clock.today()), clock.zone()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private val _sheet = MutableStateFlow<TagSheet?>(null)
        val sheet: StateFlow<TagSheet?> = _sheet.asStateFlow()

        fun startNew() {
            _sheet.value = TagSheet()
        }

        fun dismiss() {
            _sheet.value = null
        }

        fun edit(transform: (TagSheet) -> TagSheet) = _sheet.update { it?.let(transform)?.copy(error = null) }

        fun save() {
            val s = _sheet.value ?: return
            viewModelScope.launch {
                val error = createTag(Tag(UUID.randomUUID(), s.name, s.pinned))
                _sheet.value = if (error == null) null else s.copy(error = error)
            }
        }

        fun togglePin(tag: Tag) {
            viewModelScope.launch { pinTag(tag.id, !tag.pinned) }
        }
    }

enum class TagRange { THIS_MONTH, LAST_MONTH, RANGE }

data class TagDetailState(
    val tag: Tag,
    val range: TagRange,
    val from: LocalDate,
    val to: LocalDate,
    val income: Money,
    val expense: Money,
    val count: Int,
    /** Expenses per main category, largest first (REQ-ETQ-03). */
    val byCategory: List<Pair<Category, Money>>,
    val days: List<HistoryDay>,
) {
    val net: Money get() = income - expense
}

private data class Window(
    val range: TagRange,
    val from: LocalDate,
    val to: LocalDate,
)

/** Detalle de etiqueta (S9-UI02): this month, last month or a range; by category and its movements. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TagDetailViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        private val tags: TagRepository,
        transactions: TransactionRepository,
        categories: CategoryRepository,
        accounts: AccountRepository,
        private val renameTag: RenameTag,
        private val deleteTag: DeleteTag,
        private val pinTag: PinTag,
        clock: Clock,
    ) : ViewModel() {
        private val id: UUID = UUID.fromString(savedState.get<String>("id"))
        private val today = clock.today()
        private val zone = clock.zone()
        private val window = MutableStateFlow(windowOf(TagRange.THIS_MONTH))

        /** Null while loading or once the tag is gone. */
        val state: StateFlow<TagDetailState?> =
            window
                .flatMapLatest { w ->
                    val filter =
                        TransactionFilter(
                            from = w.from.atStartOfDay(zone).toInstant(),
                            to =
                                w.to
                                    .plusDays(1)
                                    .atStartOfDay(zone)
                                    .toInstant(),
                            tagIds = setOf(id),
                        )
                    combine(
                        tags.observeAll(),
                        transactions.search(filter, MOVEMENTS),
                        categories.observeAll(),
                        accounts.observeBalances(),
                    ) { all, rows, cats, accs ->
                        val tag = all.firstOrNull { it.id == id } ?: return@combine null
                        val moved = rows.filter { it.type != TransactionType.TRANSFER }
                        val byId = cats.associateBy { it.id }
                        val byCategory =
                            moved
                                .filter { it.type == TransactionType.EXPENSE }
                                .groupBy { t -> t.categoryId?.let(byId::get)?.let { c -> c.parentId?.let(byId::get) ?: c } }
                                .mapNotNull { (main, list) -> main?.let { it to list.fold(Money.ZERO) { s, t -> s + t.amount } } }
                                .sortedByDescending { it.second.scaled }
                        TagDetailState(
                            tag = tag,
                            range = w.range,
                            from = w.from,
                            to = w.to,
                            income = moved.filter { it.type == TransactionType.INCOME }.fold(Money.ZERO) { s, t -> s + t.amount },
                            expense = moved.filter { it.type == TransactionType.EXPENSE }.fold(Money.ZERO) { s, t -> s + t.amount },
                            count = rows.size,
                            byCategory = byCategory,
                            days = buildDays(rows, accs.associate { it.account.id to it.account.name }, cats, zone),
                        )
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private val _sheet = MutableStateFlow<TagSheet?>(null)
        val sheet: StateFlow<TagSheet?> = _sheet.asStateFlow()

        private fun windowOf(range: TagRange): Window {
            val month = YearMonth.from(today)
            return when (range) {
                TagRange.THIS_MONTH -> Window(range, month.atDay(1), month.atEndOfMonth())
                TagRange.LAST_MONTH -> month.minusMonths(1).let { Window(range, it.atDay(1), it.atEndOfMonth()) }
                TagRange.RANGE -> window.value.copy(range = range)
            }
        }

        fun selectRange(range: TagRange) {
            window.value = windowOf(range)
        }

        fun setFrom(date: LocalDate) = window.update { Window(TagRange.RANGE, minOf(date, it.to), maxOf(date, it.to)) }

        fun setTo(date: LocalDate) = window.update { Window(TagRange.RANGE, minOf(it.from, date), maxOf(it.from, date)) }

        fun togglePin() {
            val tag = state.value?.tag ?: return
            viewModelScope.launch { pinTag(tag.id, !tag.pinned) }
        }

        fun startEdit() {
            _sheet.value = state.value?.tag?.let { TagSheet(editing = it) }
        }

        fun dismiss() {
            _sheet.value = null
        }

        fun edit(transform: (TagSheet) -> TagSheet) = _sheet.update { it?.let(transform)?.copy(error = null) }

        fun save() {
            val s = _sheet.value ?: return
            viewModelScope.launch {
                val error = renameTag(id, s.name)
                _sheet.value = if (error == null) null else s.copy(error = error)
            }
        }

        fun delete(onDeleted: () -> Unit) {
            viewModelScope.launch {
                _sheet.value = null
                deleteTag(id)
                onDeleted()
            }
        }

        private companion object {
            /** A tag groups a trip or a few outings; more than this is not shown in its detail. */
            const val MOVEMENTS = 500
        }
    }
