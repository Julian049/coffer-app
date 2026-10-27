package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.importing.CashewFormat
import com.dev.julian09.financeapp.domain.importing.DuplicateDetector
import com.dev.julian09.financeapp.domain.importing.DuplicateKey
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountResolution
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.CategoryRef
import com.dev.julian09.financeapp.domain.model.CategoryResolution
import com.dev.julian09.financeapp.domain.model.CsvTable
import com.dev.julian09.financeapp.domain.model.ExportRange
import com.dev.julian09.financeapp.domain.model.ImportMapping
import com.dev.julian09.financeapp.domain.model.ImportPlan
import com.dev.julian09.financeapp.domain.model.ImportPreview
import com.dev.julian09.financeapp.domain.model.ImportRow
import com.dev.julian09.financeapp.domain.model.ImportSource
import com.dev.julian09.financeapp.domain.model.ImportSummary
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.ParsedRow
import com.dev.julian09.financeapp.domain.model.RowError
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.CsvFiles
import com.dev.julian09.financeapp.domain.port.ImportScheduler
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import com.dev.julian09.financeapp.domain.port.TagRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

private const val SAMPLE = 20
private const val ERRORS_SHOWN = 50
private const val BATCH = 500
private const val ACCOUNT_NAME_LIMIT = 60
private const val CATEGORY_NAME_LIMIT = 50
private const val TAG_NAME_LIMIT = 50
private const val NOTE_LIMIT = 500

/** Names match ignoring case and outer spaces, like the server's unique indexes. */
internal fun nameKey(name: String) = name.trim().lowercase()

private fun kindOf(type: TransactionType) = if (type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE

/** The category a row names, its subcategory when it has one; null when it is not in the catalog. */
internal fun findCategory(
    all: List<Category>,
    ref: CategoryRef,
): Category? {
    val main = all.firstOrNull { it.parentId == null && it.kind == ref.kind && nameKey(it.name) == nameKey(ref.name) } ?: return null
    val sub = ref.subcategory ?: return main
    return all.firstOrNull { it.parentId == main.id && nameKey(it.name) == nameKey(sub) }
}

private fun ImportRow.categoryRef(): CategoryRef? = category?.let { CategoryRef(kindOf(type), it, subcategory) }

private fun parse(
    source: ImportSource,
    table: CsvTable,
    mapping: ImportMapping,
    clock: Clock,
): Sequence<ParsedRow> =
    when (source) {
        ImportSource.CASHEW -> CashewFormat.parse(table.headers, table.records, mapping, clock.zone())
    }

/**
 * SEQ-13 §13.1, REQ-IMP-02 to 05: reads the whole file once in streaming and says what would happen. The mapping is
 * the source's suggestion until the user changes it.
 */
class PreviewImport
    @Inject
    constructor(
        private val files: CsvFiles,
        private val accounts: AccountRepository,
        private val categories: CategoryRepository,
        private val transactions: TransactionRepository,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            source: ImportSource,
            uri: String,
            mapping: ImportMapping? = null,
        ): ImportPreview {
            val known = accounts.observeBalances().first().map { it.account }
            val names = known.associate { it.id to nameKey(it.name) }
            val catalog = categories.observeAll().first()
            val stored = transactions.observeSince(Instant.EPOCH).first()
            // Before resolving, a row matches a stored movement through its account's name
            val detector =
                DuplicateDetector(
                    stored.mapNotNull { t ->
                        names[t.accountId]?.let { DuplicateKey.of(t.occurredAt, t.amount, it, t.type) }
                    },
                )
            return files.read(uri).use { table ->
                val used = mapping ?: CashewFormat.suggest(table.headers)
                val sample = mutableListOf<ImportRow>()
                val errors = mutableListOf<RowError>()
                var errorCount = 0
                var valid = 0
                var duplicates = 0
                var unpaid = 0
                val unknownAccounts = linkedSetOf<String>()
                val unknownCategories = linkedSetOf<CategoryRef>()
                val accountNames = known.map { nameKey(it.name) }.toSet()
                for (parsed in parse(source, table, used, clock)) {
                    when (parsed) {
                        is ParsedRow.Unpaid -> unpaid++
                        is ParsedRow.Invalid -> {
                            errorCount++
                            if (errors.size < ERRORS_SHOWN) errors += parsed.error
                        }
                        is ParsedRow.Valid -> {
                            val row = parsed.row
                            valid++
                            if (sample.size < SAMPLE) sample += row
                            listOfNotNull(row.account, row.destination)
                                .filter { nameKey(it) !in accountNames && unknownAccounts.none { u -> nameKey(u) == nameKey(it) } }
                                .forEach { unknownAccounts += it.trim() }
                            row.categoryRef()?.takeIf { findCategory(catalog, it) == null }?.let { ref ->
                                if (unknownCategories.none { sameRef(it, ref) }) unknownCategories += ref
                            }
                            if (detector.isDuplicate(
                                    DuplicateKey.of(row.occurredAt, row.amount, nameKey(row.account), row.type),
                                )
                            ) {
                                duplicates++
                            }
                        }
                    }
                }
                ImportPreview(
                    headers = table.headers,
                    mapping = used,
                    sample = sample,
                    valid = valid,
                    errors = errors,
                    errorCount = errorCount,
                    duplicates = duplicates,
                    unpaid = unpaid,
                    unknownAccounts = unknownAccounts.toList(),
                    unknownCategories = unknownCategories.toList(),
                )
            }
        }
    }

/** SEQ-13: hands the confirmed plan to the background import (RNF-04). */
class CommitImport
    @Inject
    constructor(
        private val scheduler: ImportScheduler,
    ) {
        suspend operator fun invoke(plan: ImportPlan) = scheduler.start(plan)
    }

/**
 * ImportWorker's job (S8-DAT02): creates what the plan says, then reads the file again and writes the movements in
 * batches of 500, all PENDING (they sync like any movement). [onProgress] gets the rows handled so far.
 */
class RunImport
    @Inject
    constructor(
        private val files: CsvFiles,
        private val accounts: AccountRepository,
        private val categories: CategoryRepository,
        private val tags: TagRepository,
        private val transactions: TransactionRepository,
        private val sync: SyncScheduler,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            plan: ImportPlan,
            onProgress: suspend (Int) -> Unit = {},
        ): ImportSummary {
            val accountIds = resolveAccounts(plan)
            val categoryIds = mutableMapOf<CategoryRef, UUID>()
            val tagIds =
                tags
                    .observeAll()
                    .first()
                    .associate { nameKey(it.name) to it.id }
                    .toMutableMap()
            val stored = transactions.observeSince(Instant.EPOCH).first()
            val detector = DuplicateDetector(stored.map { DuplicateKey.of(it.occurredAt, it.amount, it.accountId, it.type) })
            var imported = 0
            var skipped = 0
            var errors = 0
            var handled = 0
            val batch = mutableListOf<Transaction>()

            suspend fun flush() {
                if (batch.isEmpty()) return
                transactions.insertAll(batch.toList())
                imported += batch.size
                batch.clear()
                onProgress(handled)
            }

            files.read(plan.uri).use { table ->
                for (parsed in parse(plan.source, table, plan.mapping, clock)) {
                    handled++
                    if (parsed is ParsedRow.Unpaid) {
                        skipped++
                        continue
                    }
                    val row = (parsed as? ParsedRow.Valid)?.row
                    val account = row?.let { accountIds[nameKey(it.account)] }
                    val destination = row?.destination?.let { accountIds[nameKey(it)] }
                    val category = row?.categoryRef()?.let { categoryId(plan, it, categoryIds) }
                    if (row == null ||
                        account == null ||
                        (row.type == TransactionType.TRANSFER) != (destination != null) ||
                        (row.type != TransactionType.TRANSFER && category == null)
                    ) {
                        errors++
                        continue
                    }
                    if (detector.isDuplicate(DuplicateKey.of(row.occurredAt, row.amount, account, row.type)) && plan.skipDuplicates) {
                        skipped++
                        continue
                    }
                    batch +=
                        Transaction(
                            id = UUID.randomUUID(),
                            type = row.type,
                            amount = row.amount,
                            occurredAt = row.occurredAt,
                            accountId = account,
                            destinationAccountId = destination,
                            categoryId = category,
                            tagIds =
                                row.tag
                                    ?.split(TAG_SEPARATOR)
                                    ?.filter { it.isNotBlank() }
                                    ?.map { tagId(it, tagIds) }
                                    .orEmpty(),
                            note = row.note?.take(NOTE_LIMIT),
                        )
                    if (batch.size == BATCH) flush()
                }
            }
            flush()
            sync.requestSync()
            return ImportSummary(plan.fileName, clock.now(), imported, skipped, errors)
        }

        /** Every account name of the file → its id: existing ones by name, the rest as the plan says. */
        private suspend fun resolveAccounts(plan: ImportPlan): Map<String, UUID> {
            val known = accounts.observeBalances().first().map { it.account }
            val ids = known.associate { nameKey(it.name) to it.id }.toMutableMap()
            var order = known.maxOfOrNull { it.sortOrder } ?: -1
            plan.accounts.forEach { (name, resolution) ->
                ids[nameKey(name)] =
                    when (resolution) {
                        is AccountResolution.Use -> resolution.accountId
                        is AccountResolution.Create ->
                            ids[nameKey(name)] ?: UUID.randomUUID().also { id ->
                                accounts.create(
                                    Account(id, name.trim().take(ACCOUNT_NAME_LIMIT), resolution.type, Money.ZERO, sortOrder = ++order),
                                )
                            }
                    }
            }
            return ids
        }

        private suspend fun categoryId(
            plan: ImportPlan,
            ref: CategoryRef,
            cache: MutableMap<CategoryRef, UUID>,
        ): UUID? {
            cache[ref]?.let { return it }
            val catalog = categories.observeAll().first()
            val id =
                findCategory(catalog, ref)?.id
                    ?: when (
                        val resolution =
                            plan.categories.entries
                                .firstOrNull { (k, _) -> sameRef(k, ref) }
                                ?.value
                    ) {
                        is CategoryResolution.Use -> resolution.categoryId
                        CategoryResolution.Create -> create(catalog, ref)
                        null -> null
                    }
            id?.let { cache[ref] = it }
            return id
        }

        private suspend fun create(
            catalog: List<Category>,
            ref: CategoryRef,
        ): UUID {
            val main =
                findCategory(catalog, ref.copy(subcategory = null))?.id
                    ?: UUID.randomUUID().also { categories.create(Category(it, ref.name.trim().take(CATEGORY_NAME_LIMIT), ref.kind)) }
            val sub = ref.subcategory ?: return main
            return UUID.randomUUID().also {
                categories.create(
                    Category(it, sub.trim().take(CATEGORY_NAME_LIMIT), ref.kind, parentId = main),
                )
            }
        }

        private suspend fun tagId(
            name: String,
            ids: MutableMap<String, UUID>,
        ): UUID =
            ids.getOrPut(nameKey(name)) {
                UUID.randomUUID().also { tags.create(Tag(it, name.trim().removePrefix("#").take(TAG_NAME_LIMIT))) }
            }
    }

/** The native `tag` column holds every tag of the movement (ADR-0015). */
internal const val TAG_SEPARATOR = "|"

internal fun sameRef(
    a: CategoryRef,
    b: CategoryRef,
) = a.kind == b.kind && nameKey(a.name) == nameKey(b.name) && a.subcategory?.let(::nameKey) == b.subcategory?.let(::nameKey)

/** Coffer's own format (detailed-design §6.9.9), column names in English (PO, 2026-10-19). */
val NATIVE_COLUMNS = listOf("date", "type", "amount", "account", "destination_account", "category", "subcategory", "tag", "note", "concept")

private val NATIVE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/** SEQ-13 §13.2, REQ-EXP-01: movements in [range], oldest first, in Coffer's format. Returns how many went out. */
class ExportCsv
    @Inject
    constructor(
        private val files: CsvFiles,
        private val accounts: AccountRepository,
        private val categories: CategoryRepository,
        private val tags: TagRepository,
        private val transactions: TransactionRepository,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            uri: String,
            range: ExportRange,
        ): Int {
            val names = accounts.observeBalances().first().associate { it.account.id to it.account.name }
            val catalog = categories.observeAll().first().associateBy { it.id }
            val tagNames = tags.observeAll().first().associate { it.id to it.name }
            val zone = clock.zone()
            val rows =
                transactions
                    .observeSince(range.from ?: Instant.EPOCH)
                    .first()
                    .filter { t -> range.to?.let { t.occurredAt < it } ?: true }
                    .sortedBy { it.occurredAt }
            val lines =
                sequenceOf<List<String?>>(NATIVE_COLUMNS) +
                    rows.asSequence().map { t ->
                        val category = catalog[t.categoryId]
                        val main = category?.parentId?.let { catalog[it] } ?: category
                        listOf(
                            NATIVE_DATE.format(t.occurredAt.atZone(zone)),
                            when (t.type) {
                                TransactionType.EXPENSE -> "Gasto"
                                TransactionType.INCOME -> "Ingreso"
                                TransactionType.TRANSFER -> "Transferencia"
                            },
                            t.amount
                                .toBigDecimal()
                                .stripTrailingZeros()
                                .toPlainString(),
                            names[t.accountId],
                            t.destinationAccountId?.let { names[it] },
                            main?.name,
                            category?.takeIf { it.parentId != null }?.name,
                            t.tagIds
                                .mapNotNull { tagNames[it] }
                                .takeIf { it.isNotEmpty() }
                                ?.joinToString(TAG_SEPARATOR),
                            t.note,
                            t.concept,
                        )
                    }
            return files.write(uri, lines)
        }
    }
