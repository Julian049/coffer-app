package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.io.CsvReader
import com.dev.julian09.financeapp.data.io.CsvWriter
import com.dev.julian09.financeapp.data.local.FixedClock
import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.domain.model.AccountResolution
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.CategoryRef
import com.dev.julian09.financeapp.domain.model.CategoryResolution
import com.dev.julian09.financeapp.domain.model.CsvTable
import com.dev.julian09.financeapp.domain.model.ExportRange
import com.dev.julian09.financeapp.domain.model.ImportMapping
import com.dev.julian09.financeapp.domain.model.ImportPlan
import com.dev.julian09.financeapp.domain.model.ImportSource
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.CsvFiles
import com.dev.julian09.financeapp.domain.usecase.ExportCsv
import com.dev.julian09.financeapp.domain.usecase.NATIVE_COLUMNS
import com.dev.julian09.financeapp.domain.usecase.PreviewImport
import com.dev.julian09.financeapp.domain.usecase.RunImport
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.time.Instant

/** Files kept in memory: what a URI holds and what was written to it. */
class MemoryCsvFiles : CsvFiles {
    val content = mutableMapOf<String, ByteArray>()

    override fun read(uri: String): CsvTable = CsvReader.open(ByteArrayInputStream(content.getValue(uri)))

    override fun write(
        uri: String,
        lines: Sequence<List<String?>>,
    ): Int {
        val all = lines.toList()
        content[uri] = all.joinToString("") { CsvWriter.line(it) }.toByteArray()
        return all.size - 1
    }
}

/** S8-QA01 and S8-QA02 over the real repositories: AT-HU11-1 to 3 (SEQ-13). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImportTest {
    private val f = LedgerFixture(FixedClock(Instant.parse("2026-10-19T15:00:00Z")))
    private val files = MemoryCsvFiles()
    private val sync = FakeSyncScheduler()
    private val preview = PreviewImport(files, f.accounts, f.categories, f.transactions, f.clock)
    private val run = RunImport(files, f.accounts, f.categories, f.tags, f.transactions, sync, f.clock)

    @After
    fun close() = f.db.close()

    private fun sample(): String {
        files.content["cashew"] = javaClass.getResourceAsStream("/import/cashew-sample.csv")!!.readBytes()
        return "cashew"
    }

    private fun plan(
        uri: String,
        accounts: Map<String, AccountResolution>,
        categories: Map<CategoryRef, CategoryResolution>,
        mapping: ImportMapping,
    ) = ImportPlan(ImportSource.CASHEW, uri, "cashew.csv", mapping, accounts, categories)

    @Test
    fun thePreviewListsErrorsDuplicatesAndUnknownNames() =
        runBlocking {
            val p = preview(ImportSource.CASHEW, sample())
            assertEquals(13, p.valid)
            assertEquals(1, p.errorCount)
            // Rows of the same file are not duplicates of each other (coffer-app#72)
            assertEquals(0, p.duplicates)
            assertEquals(2, p.unpaid)
            assertEquals(listOf("Banco", "Tarjeta A", "Cash", "Tarjeta B"), p.unknownAccounts)
            assertEquals(7, p.unknownCategories.size)
            assertTrue(CategoryRef(CategoryKind.EXPENSE, "Finance", "Debts") in p.unknownCategories)
        }

    @Test
    fun aRefundOnACardLowersItsDebt() =
        runBlocking {
            val card = f.account("Tarjeta", type = "CREDIT_CARD")
            val food = f.category("Comida")
            val income = f.category("Reembolsos", kind = "INCOME")
            f.tx("EXPENSE", 100_000, card, category = food)
            f.tx("INCOME", 30_000, card, category = income)
            assertEquals(
                Money.ofPesos(-70_000),
                f.accounts
                    .observeBalances()
                    .first()
                    .single()
                    .balance,
            )
        }

    @Test
    fun knownNamesAreNotAskedAgain() =
        runBlocking {
            f.account("cash")
            f.category("Dining")
            f.category("Debts", parent = f.category("Finance"))
            val p = preview(ImportSource.CASHEW, sample())
            assertTrue("Cash" !in p.unknownAccounts)
            assertTrue(p.unknownCategories.none { it.name == "Dining" || it.name == "Finance" })
        }

    @Test
    fun importingCreatesWhatWasChosenSkipsDuplicatesAndSyncsEverything() =
        runBlocking {
            val uri = sample()
            val p = preview(ImportSource.CASHEW, uri)
            val existing = f.account("Mi banco")
            val dining = f.category("Comida")
            val accounts =
                p.unknownAccounts.associateWith { AccountResolution.Create(AccountType.SAVINGS) } +
                    ("Banco" to AccountResolution.Use(existing))
            val categories =
                p.unknownCategories.associateWith { CategoryResolution.Create } +
                    (CategoryRef(CategoryKind.EXPENSE, "Dining") to CategoryResolution.Use(dining))
            val summary = run(plan(uri, accounts, categories, p.mapping))
            assertEquals(13, summary.imported)
            assertEquals(2, summary.skipped) // the two unpaid ones
            assertEquals(1, summary.errors)

            val all = f.transactions.observeSince(Instant.EPOCH).first()
            assertEquals(13, all.size)
            assertEquals(2, all.count { it.categoryId == dining })
            val transfer = all.single { it.type == TransactionType.TRANSFER && it.amount == Money.ofPesos(30_000) }
            assertEquals(existing, transfer.destinationAccountId)
            assertEquals(Money.ofPesos(30_000), transfer.amount)
            assertTrue(all.all { it.pendingSync })
            assertEquals(
                2,
                f.tags
                    .observeAll()
                    .first()
                    .single { it.name == "Going out" }
                    .let { tag -> all.count { it.tagId == tag.id } },
            )
            val finance = f.categories.observeAll().first()
            assertTrue(finance.any { it.name == "Debts" && it.parentId == finance.single { c -> c.name == "Finance" }.id })
            assertEquals(
                13,
                f.db
                    .changeLog()
                    .pending(100)
                    .size,
            )
            assertTrue(sync.syncRequests > 0)

            // AT-HU11-3: the same file again only finds duplicates
            val again = run(plan(uri, accounts, categories, p.mapping))
            assertEquals(0, again.imported)
            assertEquals(15, again.skipped)
            assertEquals(
                SyncState.PENDING,
                f.db
                    .transactions()
                    .get(all.first().id)!!
                    .syncState,
            )
        }

    @Test
    fun fiveThousandRowsImportInLessThanAMinute() =
        runBlocking {
            val header = "account,amount,title,note,date,category name,budget\n"
            val body =
                (0 until 5_000).joinToString("") { i ->
                    val minute = Instant.parse("2025-01-01T00:00:00Z").plusSeconds(i * 3_600L)
                    "Banco,-${1_000 + i}.0,Compra $i,,${minute.toString().replace("T", " ").removeSuffix("Z")}.000,Dining,Going out\n"
                }
            files.content["big"] = (header + body).toByteArray()
            val p = preview(ImportSource.CASHEW, "big")
            val start = System.nanoTime()
            val summary =
                run(
                    plan(
                        "big",
                        mapOf("Banco" to AccountResolution.Create(AccountType.SAVINGS)),
                        mapOf(CategoryRef(CategoryKind.EXPENSE, "Dining") to CategoryResolution.Create),
                        p.mapping,
                    ),
                )
            val seconds = (System.nanoTime() - start) / 1_000_000_000.0
            assertEquals(5_000, summary.imported)
            assertTrue("took $seconds s", seconds < 60)
        }

    @Test
    fun theExportUsesTheNativeColumnsAndReadsBack() =
        runBlocking {
            val bank = f.account("Banco")
            val cash = f.account("Efectivo")
            val food = f.category("Comida")
            val cafe = f.category("Cafeterías", parent = food)
            f.tx("EXPENSE", 4_500, bank, at = Instant.parse("2026-10-18T13:10:00Z"), category = cafe)
            f.tx("TRANSFER", 50_000, bank, at = Instant.parse("2026-10-18T09:00:00Z"), destination = cash)
            f.tx("EXPENSE", 1_000, bank, at = Instant.parse("2026-09-30T09:00:00Z"), category = food)
            val export = ExportCsv(files, f.accounts, f.categories, f.tags, f.transactions, f.clock)
            val count = export("out", ExportRange(from = Instant.parse("2026-10-01T00:00:00Z")))
            assertEquals(2, count)
            CsvReader.open(ByteArrayInputStream(files.content.getValue("out"))).use { table ->
                assertEquals(NATIVE_COLUMNS, table.headers)
                val rows = table.records.map { it.values }.toList()
                assertEquals(listOf("2026-10-18 09:00", "Transferencia", "50000", "Banco", "Efectivo", "", "", "", ""), rows[0])
                assertEquals(listOf("2026-10-18 13:10", "Gasto", "4500", "Banco", "", "Comida", "Cafeterías", "", ""), rows[1])
            }
        }
}
