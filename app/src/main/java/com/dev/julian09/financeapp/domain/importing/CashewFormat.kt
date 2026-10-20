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
 * is how Cashew users group expenses (SRS §1.2): it becomes the tag. Cashew also exports what is not paid yet
 * (pending loans, future subscriptions) with an "amount unpaid"; only the paid part counts in its balances, so only
 * that part is imported (coffer-app#72).
 */
object CashewFormat {
    private val COLUMNS =
        mapOf(
            ImportField.DATE to "date",
            ImportField.AMOUNT to "amount",
            ImportField.AMOUNT_UNPAID to "amount unpaid",
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
            // Transfer halves waiting for their pair, which is not always the next row (coffer-app#72)
            val waiting = mutableListOf<Raw.Row>()
            for (record in records) {
                val raw = read(record, index, zone)
                if (raw is Raw.Row) {
                    // A half more than a minute away from this row will not find its pair any more
                    val stale = waiting.filter { !it.closeTo(raw) }
                    waiting -= stale.toSet()
                    stale.forEach { yield(it.alone()) }
                }
                if (raw is Raw.Row && raw.transferNote != null) {
                    val partner = waiting.firstOrNull { it.pairsWith(raw) }
                    if (partner != null) {
                        waiting -= partner
                        yield(partner.transferWith(raw))
                    } else {
                        waiting += raw
                    }
                } else {
                    yield(raw.alone())
                }
            }
            waiting.forEach { yield(it.alone()) }
        }

    private sealed interface Raw {
        fun alone(): ParsedRow

        data class Bad(
            val error: RowError,
        ) : Raw {
            override fun alone() = ParsedRow.Invalid(error)
        }

        data class Unpaid(
            val line: Int,
        ) : Raw {
            override fun alone() = ParsedRow.Unpaid(line)
        }

        data class Row(
            val row: ImportRow,
            /** "A → B" of a transfer half. */
            val transferNote: String?,
            val signed: BigDecimal,
        ) : Raw {
            override fun alone(): ParsedRow =
                if (row.category == null) ParsedRow.Invalid(RowError(row.line, RowProblem.CATEGORY)) else ParsedRow.Valid(row)

            fun closeTo(other: Row) = Duration.between(row.occurredAt, other.row.occurredAt).abs().seconds <= PAIR_SECONDS

            fun pairsWith(other: Row) =
                transferNote == other.transferNote &&
                    signed.signum() != other.signed.signum() &&
                    signed.abs().compareTo(other.signed.abs()) == 0 &&
                    closeTo(other)

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
        val total =
            field(ImportField.AMOUNT)?.toBigDecimalOrNull()?.takeIf { it.signum() != 0 && it.abs() < MAX_AMOUNT }
                ?: return Raw.Bad(RowError(line, RowProblem.AMOUNT))
        // Only the paid part: "amount unpaid" has the same sign as the amount
        val signed = total - (field(ImportField.AMOUNT_UNPAID)?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        if (signed.signum() == 0 || signed.signum() != total.signum()) return Raw.Unpaid(line)
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
