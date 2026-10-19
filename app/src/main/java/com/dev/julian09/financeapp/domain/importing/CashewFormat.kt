package com.dev.julian09.financeapp.domain.importing

import com.dev.julian09.financeapp.domain.model.CsvRecord
import com.dev.julian09.financeapp.domain.model.ImportField
import com.dev.julian09.financeapp.domain.model.ImportMapping
import com.dev.julian09.financeapp.domain.model.ImportRow
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.ParsedRow
import com.dev.julian09.financeapp.domain.model.RowError
import com.dev.julian09.financeapp.domain.model.RowProblem
import com.dev.julian09.financeapp.domain.model.TransactionType
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * The CSV that Cashew exports (S8-H01, planning of 2026-10-19): `account, amount, …, title, note, date, income, …,
 * category name, subcategory name, …, budget, …`. One signed amount (negative = expense); a transfer is two rows,
 * "<A> Transfer Out" and "<B> Transfer In", with the note "Transferred Balance\nA → B", one second apart. The budget
 * is how Cashew users group expenses (SRS §1.2): it becomes the tag.
 */
object CashewFormat {
    private val COLUMNS =
        mapOf(
            ImportField.DATE to "date",
            ImportField.AMOUNT to "amount",
            ImportField.ACCOUNT to "account",
            ImportField.CATEGORY to "category name",
            ImportField.SUBCATEGORY to "subcategory name",
            ImportField.TAG to "budget",
            ImportField.TITLE to "title",
            ImportField.NOTE to "note",
        )

    /** Rows of the same transfer are written one after the other, a second apart. */
    private const val PAIR_SECONDS = 60L
    private const val ARROW = " → "

    /** The ×10⁴ Long of Money holds up to about 9 × 10¹⁴ pesos. */
    private val MAX_AMOUNT = BigDecimal("1E14")
    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss][.SSS]")

    /** Cashew's columns by name, ignoring case; a field without its column stays unmapped. */
    fun suggest(headers: List<String>): ImportMapping =
        COLUMNS.mapValues { (_, column) -> headers.firstOrNull { it.trim().equals(column, ignoreCase = true) } }

    /** The rows of a file, transfers merged into one row; [zone] reads the dates, which have no offset. */
    fun parse(
        headers: List<String>,
        records: Sequence<CsvRecord>,
        mapping: ImportMapping,
        zone: ZoneId,
    ): Sequence<ParsedRow> =
        sequence {
            val index = mapping.mapValues { (_, column) -> column?.let { headers.indexOf(it) }?.takeIf { it >= 0 } }
            var waiting: Raw.Row? = null
            for (record in records) {
                val raw = read(record, index, zone)
                val half = waiting
                if (half != null) {
                    waiting = null
                    if (raw is Raw.Row && half.pairsWith(raw)) {
                        yield(half.transferWith(raw))
                        continue
                    }
                    yield(half.alone())
                }
                if (raw is Raw.Row && raw.transferNote != null) {
                    waiting = raw
                } else {
                    yield(raw.alone())
                }
            }
            waiting?.let { yield(it.alone()) }
        }

    private sealed interface Raw {
        fun alone(): ParsedRow

        data class Bad(
            val error: RowError,
        ) : Raw {
            override fun alone() = ParsedRow.Invalid(error)
        }

        data class Row(
            val row: ImportRow,
            /** "A → B" of a transfer half. */
            val transferNote: String?,
            val signed: BigDecimal,
        ) : Raw {
            override fun alone(): ParsedRow =
                if (row.category == null) ParsedRow.Invalid(RowError(row.line, RowProblem.CATEGORY)) else ParsedRow.Valid(row)

            fun pairsWith(other: Row) =
                transferNote == other.transferNote &&
                    signed.signum() != other.signed.signum() &&
                    signed.abs().compareTo(other.signed.abs()) == 0 &&
                    Duration.between(row.occurredAt, other.row.occurredAt).abs().seconds <= PAIR_SECONDS

            /** The outgoing half gives the source and the incoming one the destination. */
            fun transferWith(other: Row): ParsedRow {
                val (out, into) = if (signed.signum() < 0) this to other else other to this
                return ParsedRow.Valid(
                    out.row.copy(
                        type = TransactionType.TRANSFER,
                        destination = into.row.account,
                        category = null,
                        subcategory = null,
                        tag = null,
                        note = null,
                    ),
                )
            }
        }
    }

    private fun read(
        record: CsvRecord,
        index: Map<ImportField, Int?>,
        zone: ZoneId,
    ): Raw {
        fun field(f: ImportField) = index[f]?.let { record.values.getOrNull(it) }?.trim()?.ifEmpty { null }

        val line = record.line
        val at = field(ImportField.DATE)?.let { date(it) } ?: return Raw.Bad(RowError(line, RowProblem.DATE))
        val signed =
            field(ImportField.AMOUNT)?.toBigDecimalOrNull()?.takeIf { it.signum() != 0 && it.abs() < MAX_AMOUNT }
                ?: return Raw.Bad(RowError(line, RowProblem.AMOUNT))
        val account = field(ImportField.ACCOUNT) ?: return Raw.Bad(RowError(line, RowProblem.ACCOUNT))
        val note = field(ImportField.NOTE)
        val transfer = note?.lines()?.firstOrNull { ARROW in it }?.trim()
        val title = field(ImportField.TITLE)
        return Raw.Row(
            ImportRow(
                line = line,
                occurredAt = at.atZone(zone).toInstant(),
                type = if (signed.signum() < 0) TransactionType.EXPENSE else TransactionType.INCOME,
                amount = Money.of(signed.abs()),
                account = account,
                category = field(ImportField.CATEGORY),
                subcategory = field(ImportField.SUBCATEGORY),
                tag = field(ImportField.TAG),
                note = listOfNotNull(title, note).joinToString(" · ").ifEmpty { null },
            ),
            transferNote = transfer,
            signed = signed,
        )
    }

    private fun date(text: String): LocalDateTime? =
        try {
            LocalDateTime.parse(text, DATE_TIME)
        } catch (_: DateTimeParseException) {
            runCatching { LocalDate.parse(text).atStartOfDay() }.getOrNull()
        }
}
