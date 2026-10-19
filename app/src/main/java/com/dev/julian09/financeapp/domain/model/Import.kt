package com.dev.julian09.financeapp.domain.model

import java.io.Closeable
import java.time.Instant
import java.util.UUID

/** A CSV file read in streaming (RNF-04): its header and its records, never the whole file in memory. */
class CsvTable(
    val headers: List<String>,
    val records: Sequence<CsvRecord>,
    private val onClose: () -> Unit = {},
) : Closeable {
    override fun close() = onClose()
}

/** One record and the file line it starts on (a quoted note can span several lines). */
data class CsvRecord(
    val line: Int,
    val values: List<String>,
)

/** Where a file comes from (PO, 2026-10-19): only Cashew in v1.0.0; Coffer's own format and other apps come later. */
enum class ImportSource { CASHEW, }

/** What a column can feed (REQ-IMP-02). Cashew gives one signed amount: the sign says expense or income. */
enum class ImportField { DATE, AMOUNT, ACCOUNT, CATEGORY, SUBCATEGORY, TAG, TITLE, NOTE }

/** Field → column header, null when no column feeds it. */
typealias ImportMapping = Map<ImportField, String?>

/** A row ready to become a movement, still with names instead of ids. */
data class ImportRow(
    val line: Int,
    val occurredAt: Instant,
    val type: TransactionType,
    val amount: Money,
    val account: String,
    val destination: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val tag: String? = null,
    val note: String? = null,
)

/** Why a row cannot be imported (REQ-IMP-03). */
enum class RowProblem { DATE, AMOUNT, ACCOUNT, CATEGORY }

data class RowError(
    val line: Int,
    val problem: RowProblem,
)

/** A row or the reason it is refused. */
sealed interface ParsedRow {
    data class Valid(
        val row: ImportRow,
    ) : ParsedRow

    data class Invalid(
        val error: RowError,
    ) : ParsedRow
}

/** A category the file names: the main one and, maybe, a subcategory. */
data class CategoryRef(
    val kind: CategoryKind,
    val name: String,
    val subcategory: String? = null,
)

/** REQ-IMP-05: create the account with a type, or use an existing one. */
sealed interface AccountResolution {
    data class Create(
        val type: AccountType,
    ) : AccountResolution

    data class Use(
        val accountId: UUID,
    ) : AccountResolution
}

/** REQ-IMP-05: create the category (and its subcategory), or use an existing one. */
sealed interface CategoryResolution {
    data object Create : CategoryResolution

    data class Use(
        val categoryId: UUID,
    ) : CategoryResolution
}

/** What the user decided before importing; the worker reads the file again with it. */
data class ImportPlan(
    val source: ImportSource,
    val uri: String,
    val fileName: String,
    val mapping: ImportMapping,
    val accounts: Map<String, AccountResolution>,
    val categories: Map<CategoryRef, CategoryResolution>,
    /** REQ-IMP-04: duplicates are skipped by default. */
    val skipDuplicates: Boolean = true,
)

/** The check before importing (REQ-IMP-03 to 05). */
data class ImportPreview(
    val headers: List<String>,
    val mapping: ImportMapping,
    /** The first valid rows. */
    val sample: List<ImportRow>,
    val valid: Int,
    /** The first errors and how many there are in all. */
    val errors: List<RowError>,
    val errorCount: Int,
    val duplicates: Int,
    val unknownAccounts: List<String>,
    val unknownCategories: List<CategoryRef>,
)

/** REQ-IMP-06. */
data class ImportSummary(
    val fileName: String,
    val at: Instant,
    val imported: Int,
    val skipped: Int,
    val errors: Int,
)

/** Exportar a CSV (REQ-EXP-01): everything when both ends are null. */
data class ExportRange(
    val from: Instant? = null,
    val to: Instant? = null,
)
