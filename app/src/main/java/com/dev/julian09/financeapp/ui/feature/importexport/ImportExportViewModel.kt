package com.dev.julian09.financeapp.ui.feature.importexport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountResolution
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryRef
import com.dev.julian09.financeapp.domain.model.CategoryResolution
import com.dev.julian09.financeapp.domain.model.ExportRange
import com.dev.julian09.financeapp.domain.model.ImportField
import com.dev.julian09.financeapp.domain.model.ImportPlan
import com.dev.julian09.financeapp.domain.model.ImportPreview
import com.dev.julian09.financeapp.domain.model.ImportSource
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.ImportScheduler
import com.dev.julian09.financeapp.domain.port.ImportStatus
import com.dev.julian09.financeapp.domain.usecase.CommitImport
import com.dev.julian09.financeapp.domain.usecase.ExportCsv
import com.dev.julian09.financeapp.domain.usecase.PreviewImport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** IDLE shows the two cards; the rest are the steps of an import (SEQ-13 §13.1). */
enum class ImportStep { IDLE, SOURCE, PICKING, LOADING, MAPPING, REVIEW }

enum class ExportChoice { ALL, THIS_MONTH, LAST_MONTH, CUSTOM }

data class ImportExportState(
    val step: ImportStep = ImportStep.IDLE,
    val source: ImportSource = ImportSource.CASHEW,
    val uri: String? = null,
    val fileName: String = "",
    val preview: ImportPreview? = null,
    val accounts: Map<String, AccountResolution> = emptyMap(),
    val categories: Map<CategoryRef, CategoryResolution> = emptyMap(),
    val skipDuplicates: Boolean = true,
    val export: ExportChoice = ExportChoice.ALL,
    val customFrom: LocalDate? = null,
    val customTo: LocalDate? = null,
    /** A short message after exporting or when a file cannot be read. */
    val message: String? = null,
)

/** What the review step picks from. */
data class ImportCatalog(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val status: ImportStatus = ImportStatus(),
)

/** Importar y exportar (S8-UI01, S8-UI02). */
@HiltViewModel
class ImportExportViewModel
    @Inject
    constructor(
        accounts: AccountRepository,
        categories: CategoryRepository,
        scheduler: ImportScheduler,
        private val preview: PreviewImport,
        private val commit: CommitImport,
        private val export: ExportCsv,
        private val clock: Clock,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ImportExportState())
        val state: StateFlow<ImportExportState> = _state.asStateFlow()

        val catalog: StateFlow<ImportCatalog> =
            combine(accounts.observeActive(), categories.observeAll(), scheduler.observe()) { accs, cats, status ->
                ImportCatalog(accs, cats, status)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ImportCatalog())

        /** "Seleccionar archivo": first where it comes from (PO, 2026-10-19). */
        fun startImport() = _state.update { it.copy(step = ImportStep.SOURCE, message = null) }

        fun chooseSource(source: ImportSource) = _state.update { it.copy(source = source, step = ImportStep.PICKING) }

        fun cancel() = _state.update { ImportExportState(export = it.export, customFrom = it.customFrom, customTo = it.customTo) }

        fun fileChosen(
            uri: String?,
            name: String,
        ) {
            if (uri == null) return cancel()
            _state.update { it.copy(uri = uri, fileName = name, step = ImportStep.LOADING) }
            load(mapping = null, next = ImportStep.MAPPING)
        }

        fun setMapping(
            field: ImportField,
            column: String?,
        ) = _state.update { s -> s.copy(preview = s.preview?.let { it.copy(mapping = it.mapping + (field to column)) }) }

        /** Reads the file again with the mapping and proposes what to do with the unknown names (REQ-IMP-05). */
        fun review() = load(mapping = _state.value.preview?.mapping, next = ImportStep.REVIEW)

        private fun load(
            mapping: Map<ImportField, String?>?,
            next: ImportStep,
        ) {
            val s = _state.value
            val uri = s.uri ?: return
            _state.update { it.copy(step = ImportStep.LOADING) }
            viewModelScope.launch {
                val result = runCatching { withContext(Dispatchers.IO) { preview(s.source, uri, mapping) } }
                _state.update { current ->
                    result.fold(
                        onSuccess = { p ->
                            current.copy(
                                step = next,
                                preview = p,
                                accounts =
                                    p.unknownAccounts.associateWith {
                                        current.accounts[it] ?: AccountResolution.Create(
                                            guessType(it),
                                        )
                                    },
                                categories = p.unknownCategories.associateWith { current.categories[it] ?: CategoryResolution.Create },
                            )
                        },
                        onFailure = {
                            ImportExportState(
                                message = "No se pudo leer el archivo. Revisa que sea el CSV exportado de Cashew.",
                            )
                        },
                    )
                }
            }
        }

        fun setAccount(
            name: String,
            resolution: AccountResolution,
        ) = _state.update { it.copy(accounts = it.accounts + (name to resolution)) }

        fun setCategory(
            ref: CategoryRef,
            resolution: CategoryResolution,
        ) = _state.update { it.copy(categories = it.categories + (ref to resolution)) }

        /** "Crear todas" (REQ-IMP-05). */
        fun createAll() =
            _state.update { s ->
                s.copy(
                    accounts =
                        s.accounts.mapValues { (name, r) ->
                            r as? AccountResolution.Create
                                ?: AccountResolution.Create(guessType(name))
                        },
                    categories = s.categories.mapValues { CategoryResolution.Create },
                )
            }

        fun setSkipDuplicates(value: Boolean) = _state.update { it.copy(skipDuplicates = value) }

        fun confirm() {
            val s = _state.value
            val p = s.preview ?: return
            val uri = s.uri ?: return
            viewModelScope.launch {
                commit(ImportPlan(s.source, uri, s.fileName, p.mapping, s.accounts, s.categories, s.skipDuplicates))
                cancel()
            }
        }

        fun setExport(value: ExportChoice) = _state.update { it.copy(export = value, message = null) }

        fun setCustomRange(
            from: LocalDate,
            to: LocalDate,
        ) = _state.update { it.copy(export = ExportChoice.CUSTOM, customFrom = from, customTo = to) }

        fun exportTo(uri: String?) {
            uri ?: return
            val s = _state.value
            viewModelScope.launch {
                val result = runCatching { withContext(Dispatchers.IO) { export(uri, range(s)) } }
                _state.update {
                    it.copy(
                        message =
                            result.fold(
                                { n -> if (n == 1) "Se exportó 1 movimiento." else "Se exportaron $n movimientos." },
                                { "No se pudo guardar el archivo." },
                            ),
                    )
                }
            }
        }

        private fun range(s: ImportExportState): ExportRange {
            val zone = clock.zone()
            val month = YearMonth.from(clock.today())

            fun start(date: LocalDate) = date.atStartOfDay(zone).toInstant()
            return when (s.export) {
                ExportChoice.ALL -> ExportRange()
                ExportChoice.THIS_MONTH -> ExportRange(start(month.atDay(1)), start(month.plusMonths(1).atDay(1)))
                ExportChoice.LAST_MONTH -> ExportRange(start(month.minusMonths(1).atDay(1)), start(month.atDay(1)))
                ExportChoice.CUSTOM -> ExportRange(s.customFrom?.let(::start), s.customTo?.plusDays(1)?.let(::start))
            }
        }

        fun suggestedExportName() = "coffer-${clock.today()}.csv"

        fun clearMessage() = _state.update { it.copy(message = null) }
    }

/** A first guess the user can change: "Cash" or "Efectivo" is cash, the rest a debit account. */
internal fun guessType(name: String): AccountType =
    if (listOf("cash", "efectivo").any { name.contains(it, ignoreCase = true) }) AccountType.CASH else AccountType.DEBIT
