package com.dev.julian09.financeapp.data.io

import android.content.Context
import androidx.core.net.toUri
import com.dev.julian09.financeapp.domain.model.CsvTable
import com.dev.julian09.financeapp.domain.port.CsvFiles
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import javax.inject.Inject

/** CSV through the ContentResolver: the URIs come from the Storage Access Framework (OpenDocument, CreateDocument). */
class ContentCsvFiles
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : CsvFiles {
        override fun read(uri: String): CsvTable {
            val input = context.contentResolver.openInputStream(uri.toUri()) ?: throw FileNotFoundException(uri)
            return CsvReader.open(input)
        }

        override fun write(
            uri: String,
            lines: Sequence<List<String?>>,
        ): Int {
            var rows = -1 // the header is not a row
            val output = context.contentResolver.openOutputStream(uri.toUri(), "wt") ?: throw FileNotFoundException(uri)
            output.bufferedWriter(Charsets.UTF_8).use { writer ->
                lines.forEach {
                    writer.write(CsvWriter.line(it))
                    rows++
                }
            }
            return rows.coerceAtLeast(0)
        }
    }
