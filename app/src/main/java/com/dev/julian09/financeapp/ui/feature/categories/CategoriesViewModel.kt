package com.dev.julian09.financeapp.ui.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.CategoryTotal
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.usecase.CreateCategory
import com.dev.julian09.financeapp.domain.usecase.DeleteCategory
import com.dev.julian09.financeapp.domain.usecase.RenameCategory
import com.dev.julian09.financeapp.domain.usecase.SetLoanPaymentCategory
import com.dev.julian09.financeapp.domain.usecase.deleteTargets
import com.dev.julian09.financeapp.domain.usecase.isPickable
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
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

data class CategoryLine(
    val category: Category,
    val total: Money,
    val count: Int,
)

/** A main category with its subcategories; totals include the subcategories. */
data class CategoryNode(
    val line: CategoryLine,
    val children: List<CategoryLine>,
) {
    val total: Money get() = children.fold(line.total) { sum, it -> sum + it.total }
    val count: Int get() = line.count + children.sumOf { it.count }
}

data class CategoriesState(
    val kind: CategoryKind = CategoryKind.EXPENSE,
    val nodes: List<CategoryNode> = emptyList(),
    /** Main categories per kind, for the tab badges. */
    val counts: Map<CategoryKind, Int> = emptyMap(),
) {
    val monthTotal: Money get() = nodes.fold(Money.ZERO) { sum, it -> sum + it.total }
    val subcategories: Int get() = nodes.sumOf { it.children.size }
}

sealed interface CategorySheet {
    data class New(
        val kind: CategoryKind,
        val name: String = "",
        val parentId: UUID? = null,
        val icon: String = "pets",
        val error: LedgerError? = null,
    ) : CategorySheet

    data class Options(
        val category: Category,
        /** S17-H13: it is the category that receives the principal of loan installments. */
        val usedForLoans: Boolean = false,
    ) : CategorySheet

    /** S13-UI04: Hoja · Elegir ícono for a stored category. */
    data class Icon(
        val category: Category,
    ) : CategorySheet

    /** REQ-CAT-04: turning the exclusion on asks first; turning it off does not. */
    data class Exclude(
        val category: Category,
    ) : CategorySheet

    data class Rename(
        val category: Category,
        val name: String,
        val error: LedgerError? = null,
    ) : CategorySheet

    data class Delete(
        val category: Category,
        val subcategories: Int,
        val movements: Int,
        val targets: List<Category>,
        /** S15-DOM06: the category of loan installments is being deleted, so the target takes over that role. */
        val receivesLoans: Boolean = false,
        val targetId: UUID? = null,
        val error: LedgerError? = null,
    ) : CategorySheet
}

/** Categorías (S4-UI03): totals of the current month by default (PO, 2026-10-17). */
@HiltViewModel
class CategoriesViewModel
    @Inject
    constructor(
        private val repository: CategoryRepository,
        private val settings: SettingsRepository,
        private val createCategory: CreateCategory,
        private val renameCategory: RenameCategory,
        private val deleteCategory: DeleteCategory,
        private val setLoanCategory: SetLoanPaymentCategory,
        clock: Clock,
    ) : ViewModel() {
        private val kind = MutableStateFlow(CategoryKind.EXPENSE)

        val state: StateFlow<CategoriesState?> =
            run {
                val month = YearMonth.from(clock.today())
                val zone = clock.zone()
                val totals =
                    repository.observeTotals(
                        month.atDay(1).atStartOfDay(zone).toInstant(),
                        month
                            .plusMonths(1)
                            .atDay(1)
                            .atStartOfDay(zone)
                            .toInstant(),
                    )
                combine(
                    repository.observeAll(),
                    totals,
                    kind,
                ) { all, sums, selected -> build(all.filter { it.isPickable }, sums, selected) }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
            }

        private val _sheet = MutableStateFlow<CategorySheet?>(null)
        val sheet: StateFlow<CategorySheet?> = _sheet.asStateFlow()

        fun selectKind(value: CategoryKind) {
            kind.value = value
        }

        fun dismiss() {
            _sheet.value = null
        }

        /** «Añadir subcategoría» of a card starts with its parent chosen. */
        fun startNew(parentId: UUID? = null) {
            _sheet.value = CategorySheet.New(kind.value, parentId = parentId)
        }

        fun editNew(transform: (CategorySheet.New) -> CategorySheet.New) {
            val s = _sheet.value as? CategorySheet.New ?: return
            val next = transform(s).copy(error = null)
            // The tab follows the kind, so the "Subcategoría de …" options are the right ones
            kind.value = next.kind
            _sheet.value = next
        }

        fun saveNew() {
            val s = _sheet.value as? CategorySheet.New ?: return
            viewModelScope.launch {
                val error =
                    // Every category has its own icon, subcategories too (S13)
                    createCategory(Category(UUID.randomUUID(), s.name, s.kind, s.parentId, icon = s.icon))
                _sheet.value = if (error == null) null else s.copy(error = error)
            }
        }

        fun openOptions(category: Category) {
            _sheet.value = CategorySheet.Options(category)
            viewModelScope.launch {
                val used = settings.observe().first().loanPaymentCategoryId == category.id
                _sheet.update { if (it is CategorySheet.Options && it.category.id == category.id) it.copy(usedForLoans = used) else it }
            }
        }

        /** S17-H13: «Usar para cuotas de préstamos». */
        fun useForLoans() {
            val options = _sheet.value as? CategorySheet.Options ?: return
            viewModelScope.launch {
                val error = setLoanCategory(options.category.id)
                if (error == null) _sheet.update { (it as? CategorySheet.Options)?.copy(usedForLoans = true) ?: it }
            }
        }

        fun setExcluded(excluded: Boolean) {
            val category = (_sheet.value as? CategorySheet.Options)?.category ?: return
            if (excluded) {
                _sheet.value = CategorySheet.Exclude(category)
            } else {
                viewModelScope.launch { repository.setExcludedFromReports(category.id, false) }
                _sheet.value = CategorySheet.Options(category.copy(excludeFromReports = false))
            }
        }

        fun confirmExclude() {
            val category = (_sheet.value as? CategorySheet.Exclude)?.category ?: return
            viewModelScope.launch { repository.setExcludedFromReports(category.id, true) }
            _sheet.value = null
        }

        fun startIcon() {
            val category = (_sheet.value as? CategorySheet.Options)?.category ?: return
            _sheet.value = CategorySheet.Icon(category)
        }

        fun saveIcon(icon: String) {
            val category = (_sheet.value as? CategorySheet.Icon)?.category ?: return
            viewModelScope.launch { repository.setIcon(category.id, icon) }
            _sheet.value = null
        }

        fun startRename() {
            val category = (_sheet.value as? CategorySheet.Options)?.category ?: return
            _sheet.value = CategorySheet.Rename(category, category.name)
        }

        fun typeName(name: String) = _sheet.update { (it as? CategorySheet.Rename)?.copy(name = name, error = null) ?: it }

        fun saveRename() {
            val s = _sheet.value as? CategorySheet.Rename ?: return
            viewModelScope.launch {
                val error = renameCategory(s.category.id, s.name)
                _sheet.value = if (error == null) null else s.copy(error = error)
            }
        }

        fun startDelete() {
            val category = (_sheet.value as? CategorySheet.Options)?.category ?: return
            viewModelScope.launch {
                val all = repository.observeAll().first()
                val receivesLoans =
                    settings.observe().first().loanPaymentCategoryId.let { loan ->
                        loan != null && all.any { it.id == loan && (it.id == category.id || it.parentId == category.id) }
                    }
                _sheet.value =
                    CategorySheet.Delete(
                        category = category,
                        subcategories = all.count { it.parentId == category.id },
                        movements = repository.transactionCount(category.id),
                        // S17-H13: the category of the installments may land on a subcategory
                        targets = deleteTargets(all, category.id, anyLevel = receivesLoans),
                        receivesLoans = receivesLoans,
                    )
            }
        }

        fun selectTarget(id: UUID) = _sheet.update { (it as? CategorySheet.Delete)?.copy(targetId = id, error = null) ?: it }

        fun confirmDelete() {
            val s = _sheet.value as? CategorySheet.Delete ?: return
            viewModelScope.launch {
                val error = deleteCategory(s.category.id, s.targetId.takeIf { s.movements > 0 || s.receivesLoans })
                _sheet.value = if (error == null) null else s.copy(error = error)
            }
        }

        private fun build(
            all: List<Category>,
            sums: List<CategoryTotal>,
            selected: CategoryKind,
        ): CategoriesState {
            val byId = sums.associateBy { it.categoryId }

            fun line(c: Category) = CategoryLine(c, byId[c.id]?.total ?: Money.ZERO, byId[c.id]?.count ?: 0)
            val mains = all.filter { it.parentId == null }
            val nodes =
                mains
                    .filter { it.kind == selected }
                    .map { main -> CategoryNode(line(main), all.filter { it.parentId == main.id }.map(::line)) }
            return CategoriesState(selected, nodes, mains.groupingBy { it.kind }.eachCount())
        }
    }
