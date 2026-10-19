package com.dev.julian09.financeapp.domain.port

import com.dev.julian09.financeapp.domain.model.CsvTable
import com.dev.julian09.financeapp.domain.model.ImportPlan
import com.dev.julian09.financeapp.domain.model.ImportSummary
import kotlinx.coroutines.flow.Flow

/** Files the user picked with the Storage Access Framework, as CSV in streaming (RNF-04). [uri] is the content URI. */
interface CsvFiles {
    fun read(uri: String): CsvTable

    /** Writes [lines] (the header first) and returns how many data rows went out. */
    fun write(
        uri: String,
        lines: Sequence<List<String?>>,
    ): Int
}

/** Where an import stands: running with its progress, or done with its summary. */
data class ImportStatus(
    val running: Boolean = false,
    val processed: Int = 0,
    /** The last import that finished, as Importar y exportar shows it. */
    val last: ImportSummary? = null,
)

/** SEQ-13: the import runs in the background (ImportWorker), never on the screen. */
interface ImportScheduler {
    suspend fun start(plan: ImportPlan)

    fun observe(): Flow<ImportStatus>
}
