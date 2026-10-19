package com.dev.julian09.financeapp.data.io

import com.dev.julian09.financeapp.domain.importing.CashewFormat
import com.dev.julian09.financeapp.domain.importing.DuplicateDetector
import com.dev.julian09.financeapp.domain.importing.DuplicateKey
import com.dev.julian09.financeapp.domain.model.CsvRecord
import com.dev.julian09.financeapp.domain.model.ImportField
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.ParsedRow
import com.dev.julian09.financeapp.domain.model.RowProblem
import com.dev.julian09.financeapp.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** S8-DOM01 over an anonymized Cashew export with the same shape as the PO's (planning of S8, 2026-10-19). */
class CashewFormatTest {
    private val bogota = ZoneId.of("America/Bogota")

    private fun rows(): List<ParsedRow> =
        CsvReader.open(javaClass.getResourceAsStream("/import/cashew-sample.csv")!!).use { table ->
            CashewFormat.parse(table.headers, table.records, CashewFormat.suggest(table.headers), bogota).toList()
        }

    @Test
    fun cashewColumnsAreSuggestedByName() {
        val headers = listOf("account", "amount", "title", "note", "date", "category name", "subcategory name", "budget")
        val mapping = CashewFormat.suggest(headers)
        assertEquals("category name", mapping[ImportField.CATEGORY])
        assertEquals("budget", mapping[ImportField.TAG])
        assertEquals("date", mapping[ImportField.DATE])
    }

    @Test
    fun theTwoHalvesOfATransferBecomeOneTransfer() {
        val transfers = rows().filterIsInstance<ParsedRow.Valid>().map { it.row }.filter { it.type == TransactionType.TRANSFER }
        val t = transfers.single()
        assertEquals("Tarjeta A", t.account)
        assertEquals("Banco", t.destination)
        assertEquals(Money.ofPesos(30_000), t.amount)
        assertNull(t.category)
        assertEquals(Instant.parse("2026-10-15T20:20:30Z"), t.occurredAt)
    }

    @Test
    fun rowsKeepTheirSignTitleNoteBudgetAndSubcategory() {
        val valid = rows().filterIsInstance<ParsedRow.Valid>().map { it.row }
        assertEquals(10, valid.size)
        val lunch = valid.first { it.note?.startsWith("Lunch") == true }
        assertEquals("Lunch · con, coma", lunch.note)
        assertEquals("Going out", lunch.tag)
        assertEquals(TransactionType.EXPENSE, lunch.type)
        val bike = valid.first { it.subcategory != null }
        assertEquals("Finance" to "Debts", bike.category to bike.subcategory)
        assertTrue(valid.any { it.type == TransactionType.INCOME && it.amount == Money.ofPesos(400_000) })
    }

    @Test
    fun aBadDateIsAnErrorOnItsLine() {
        val error = rows().filterIsInstance<ParsedRow.Invalid>().single().error
        assertEquals(RowProblem.DATE, error.problem)
        assertEquals(15, error.line)
    }

    @Test
    fun aLonelyHalfStaysAnOrdinaryMovement() {
        val headers = listOf("account", "amount", "title", "note", "date", "category name")
        val records =
            sequenceOf(
                CsvRecord(
                    2,
                    listOf(
                        "Banco",
                        "5000.0",
                        "Banco Transfer In",
                        "Transferred Balance\nCaja → Banco",
                        "2026-10-01 10:00:00.000",
                        "Balance Correction",
                    ),
                ),
            )
        val row = (CashewFormat.parse(headers, records, CashewFormat.suggest(headers), bogota).single() as ParsedRow.Valid).row
        assertEquals(TransactionType.INCOME, row.type)
        assertEquals("Balance Correction", row.category)
    }

    @Test
    fun theSameMinuteAmountAccountAndTypeIsADuplicate() {
        val at = Instant.parse("2026-10-17T05:34:57Z")
        val detector = DuplicateDetector(listOf(DuplicateKey.of(at, Money.ofPesos(2_500), "cash", TransactionType.EXPENSE)))
        assertTrue(detector.isDuplicate(DuplicateKey.of(at.plusSeconds(2), Money.ofPesos(2_500), "cash", TransactionType.EXPENSE)))
        assertTrue(!detector.isDuplicate(DuplicateKey.of(at, Money.ofPesos(2_500), "cash", TransactionType.INCOME)))
        assertTrue(detector.isDuplicate(DuplicateKey.of(at, Money.ofPesos(2_500), "cash", TransactionType.INCOME)))
    }
}
