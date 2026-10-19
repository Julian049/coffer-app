package com.dev.julian09.financeapp.data.io

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

/** S8-QA01: RFC 4180, separators, encodings and the round trip with CsvWriter (S8-QA02). */
class CsvReaderTest {
    private fun read(
        text: String,
        charset: java.nio.charset.Charset = Charsets.UTF_8,
    ) = CsvReader.open(ByteArrayInputStream(text.toByteArray(charset))).use { it.headers to it.records.toList() }

    @Test
    fun quotedFieldsKeepCommasQuotesAndLineBreaksAndTellTheirFirstLine() {
        val (headers, records) = read("a,b,c\n1,\"dos, tres\",\"di \"\"hola\"\"\"\n2,\"una\nnota\",x\n\n3,,\n")
        assertEquals(listOf("a", "b", "c"), headers)
        assertEquals(listOf("1", "dos, tres", "di \"hola\""), records[0].values)
        assertEquals(listOf("2", "una\nnota", "x"), records[1].values)
        assertEquals(listOf(2, 3, 6), records.map { it.line })
        assertEquals(listOf("3", "", ""), records[2].values)
    }

    @Test
    fun semicolonsBomAndCarriageReturnsAreUnderstood() {
        val (headers, records) = read("\uFEFFfecha;monto\r\n2026-10-01;1000\r\n")
        assertEquals(listOf("fecha", "monto"), headers)
        assertEquals(listOf("2026-10-01", "1000"), records.single().values)
    }

    @Test
    fun aFileThatIsNotUtf8IsReadAsWindows1252() {
        val (_, records) = read("nota\nCafé en Bogotá\n", charset("windows-1252"))
        assertEquals("Café en Bogotá", records.single().values.single())
    }

    @Test
    fun whatTheWriterWritesTheReaderReadsBack() {
        val rows = listOf(listOf("date", "note"), listOf("2026-10-01 08:00", "con, coma y \"comillas\"\nen dos líneas"), listOf("x", null))
        val text = rows.joinToString("") { CsvWriter.line(it) }
        val (headers, records) = read(text)
        assertEquals(rows[0], headers)
        assertEquals(rows[1], records[0].values)
        assertEquals(listOf("x", ""), records[1].values)
    }
}
