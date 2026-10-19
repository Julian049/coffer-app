package com.dev.julian09.financeapp.data.io

import com.dev.julian09.financeapp.domain.model.CsvRecord
import com.dev.julian09.financeapp.domain.model.CsvTable
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.Reader
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * RFC 4180 CSV in streaming (S8-DAT01): quoted fields with commas, quotes and line breaks. The separator (`,` or `;`)
 * comes from the header line; the encoding is UTF-8 (with or without BOM) unless the first bytes are not valid UTF-8,
 * then Windows-1252, as Excel saves CSV in Spanish.
 */
object CsvReader {
    private const val PROBE = 64 * 1024
    private val WINDOWS_1252: Charset = Charset.forName("windows-1252")

    fun open(input: InputStream): CsvTable {
        val buffered = BufferedInputStream(input, PROBE)
        buffered.mark(PROBE)
        val head = probe(buffered)
        buffered.reset()
        val bom = head.size >= 3 && head[0] == 0xEF.toByte() && head[1] == 0xBB.toByte() && head[2] == 0xBF.toByte()
        if (bom) buffered.skip(3)
        val charset = if (bom || isUtf8(head)) Charsets.UTF_8 else WINDOWS_1252
        val reader = buffered.reader(charset).buffered()
        val parser = Parser(reader, separator = ',')
        val firstLine = String(head, charset).lineSequence().firstOrNull().orEmpty()
        parser.separator = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ';' else ','
        val header = parser.next()?.values?.map { it.trim().trimStart('\uFEFF') } ?: emptyList()
        return CsvTable(header, generateSequence { parser.next() }, onClose = reader::close)
    }

    /** The first bytes, without InputStream.readNBytes (API 33; the app supports 31). */
    private fun probe(input: InputStream): ByteArray {
        val bytes = ByteArray(PROBE)
        var size = 0
        while (size < PROBE) {
            val n = input.read(bytes, size, PROBE - size)
            if (n < 0) break
            size += n
        }
        return bytes.copyOf(size)
    }

    /** Valid UTF-8, allowing a character cut at the end of the probe. */
    private fun isUtf8(bytes: ByteArray): Boolean {
        val decoder = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes, 0, (bytes.size - 3).coerceAtLeast(0)))
            true
        } catch (_: CharacterCodingException) {
            false
        }
    }

    private class Parser(
        private val reader: Reader,
        var separator: Char,
    ) {
        private var line = 1

        fun next(): CsvRecord? {
            var c = reader.read()
            // Blank lines between records are skipped
            while (c == '\n'.code || c == '\r'.code) {
                if (c == '\n'.code) line++
                c = reader.read()
            }
            if (c == -1) return null
            val start = line
            val values = mutableListOf<String>()
            val field = StringBuilder()
            var quoted = false
            while (true) {
                when {
                    c == -1 -> {
                        values += field.toString()
                        return CsvRecord(start, values)
                    }
                    quoted && c == '"'.code -> {
                        val following = reader.read()
                        if (following == '"'.code) {
                            field.append('"')
                        } else {
                            quoted = false
                            c = following
                            continue
                        }
                    }
                    quoted -> {
                        if (c == '\n'.code) line++
                        field.append(c.toChar())
                    }
                    c == '"'.code && field.isEmpty() -> quoted = true
                    c == separator.code -> {
                        values += field.toString()
                        field.clear()
                    }
                    c == '\r'.code -> Unit
                    c == '\n'.code -> {
                        line++
                        values += field.toString()
                        return CsvRecord(start, values)
                    }
                    else -> field.append(c.toChar())
                }
                c = reader.read()
            }
        }
    }
}

/** RFC 4180 output in UTF-8 (S8-DOM02): a field is quoted when it has a comma, a quote or a line break. */
object CsvWriter {
    fun line(values: List<String?>): String =
        values.joinToString(",") { v ->
            val text = v.orEmpty()
            if (text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + text.replace("\"", "\"\"") + "\"" else text
        } + "\r\n"
}
