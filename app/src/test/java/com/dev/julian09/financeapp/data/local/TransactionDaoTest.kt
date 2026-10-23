package com.dev.julian09.financeapp.data.local

import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.TransactionFilter
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.SearchTransactions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** S5-QA02: order by occurredAt (AT-HU02-1), combined filters and partial search (REQ-TRX-09, 10). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TransactionDaoTest {
    private val f = LedgerFixture()
    private val search = SearchTransactions(f.transactions, f.categories)
    private val october = TransactionFilter(Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"))

    @After
    fun close() = f.db.close()

    private suspend fun amounts(
        filter: TransactionFilter,
        limit: Int = 100,
    ) = search(filter, limit).first().transactions.map {
        f.db
            .transactions()
            .get(it.id)!!
            .amount / 10_000
    }

    @Test
    fun theOrderIsWhenItHappenedNotWhenItWasRegistered() =
        runBlocking {
            val cash = f.account("Efectivo")
            // Registered first, happened later the same day
            f.tx("EXPENSE", 13, cash, at = Instant.parse("2026-10-07T13:00:00Z"))
            f.tx("EXPENSE", 9, cash, at = Instant.parse("2026-10-07T09:00:00Z"))
            f.tx("EXPENSE", 1, cash, at = Instant.parse("2026-09-30T23:00:00Z"))
            // Newest first, as in Figma and REQ-TRX-12 (PO, 2026-10-18)
            assertEquals(listOf(13L, 9L), amounts(october))
        }

    @Test
    fun filtersCombineAndACategoryIncludesItsSubcategories() =
        runBlocking {
            val cash = f.account("Efectivo")
            val nu = f.account("Nu")
            val food = f.category("Comida")
            val restaurants = f.category("Restaurantes", parent = food)
            val home = f.category("Hogar")
            val at = Instant.parse("2026-10-10T12:00:00Z")
            f.tx("EXPENSE", 1, cash, at = at, category = food)
            f.tx("EXPENSE", 2, nu, at = at, category = restaurants)
            f.tx("EXPENSE", 3, cash, at = at, category = home)
            f.tx("TRANSFER", 4, nu, at = at, destination = cash)

            assertEquals(setOf(1L, 2L), amounts(october.copy(categoryIds = setOf(food))).toSet())
            assertEquals(listOf(2L), amounts(october.copy(categoryIds = setOf(food), accountIds = setOf(nu))))
            // An account matches as origin or destination
            assertEquals(setOf(1L, 3L, 4L), amounts(october.copy(accountIds = setOf(cash))).toSet())
            assertEquals(listOf(4L), amounts(october.copy(types = setOf(TransactionType.TRANSFER))))
            val totals = search(october.copy(types = setOf(TransactionType.EXPENSE, TransactionType.TRANSFER)), 100).first().totals
            assertEquals(Money.ofPesos(6), totals.expense) // the transfer is neither expense nor income
            assertEquals(4, totals.count)
        }

    @Test
    fun theSearchMatchesPartsOfNoteCategoryAccountAndTag() =
        runBlocking {
            val cash = f.account("Billetera")
            val food = f.category("Restaurantes")
            val at = Instant.parse("2026-10-10T12:00:00Z")
            val withNote = f.tx("EXPENSE", 1, cash, at = at)
            f.db.transactions().upsert(
                listOf(
                    f.db
                        .transactions()
                        .get(withNote)!!
                        .copy(note = "Almuerzo 100% pagado"),
                ),
            )
            f.tx("EXPENSE", 2, cash, at = at, category = food)

            assertEquals(listOf(1L), amounts(october.copy(query = "almu")))
            assertEquals(listOf(2L), amounts(october.copy(query = "RESTAU")))
            assertEquals(2, amounts(october.copy(query = "bille")).size)
            // % and _ are literal characters, not wildcards
            assertEquals(listOf(1L), amounts(october.copy(query = "100%")))
            assertTrue(amounts(october.copy(query = "_")).isEmpty())
        }

    @Test
    fun theWindowGrowsUntilEverythingIsLoaded() =
        runBlocking {
            val cash = f.account("Efectivo")
            repeat(5) { f.tx("EXPENSE", it + 1L, cash, at = Instant.parse("2026-10-10T12:00:00Z").plusSeconds(it.toLong())) }
            val first = search(october, 3).first()
            assertEquals(3, first.transactions.size)
            assertTrue(first.hasMore)
            assertFalse(search(october, 6).first().hasMore)
        }

    @Test
    fun theSummaryLeavesOutCategoriesExcludedFromReportsUnlessTheFilterNamesThem() =
        runBlocking {
            val cash = f.account("Efectivo")
            val savings = f.category("Ahorros", excluded = true)
            val fund = f.category("Fondo", parent = savings)
            val opening = f.category("Saldo inicial", "INCOME", systemKey = "OPENING_BALANCE", excluded = true)
            val food = f.category("Comida")
            val at = Instant.parse("2026-10-10T12:00:00Z")
            f.tx("EXPENSE", 10, cash, at = at, category = food)
            f.tx("EXPENSE", 50, cash, at = at, category = savings)
            f.tx("EXPENSE", 7, cash, at = at, category = fund)
            f.tx("INCOME", 1_000, cash, at = at, category = opening)

            val all = search(october, 100).first().totals
            assertEquals(Money.ofPesos(10), all.expense)
            assertEquals(Money.ofPesos(0), all.income)
            // The list and the pagination still see the four rows
            assertEquals(4, all.count)
            // An explicit filter counts the category, and its subcategories through the parent
            assertEquals(Money.ofPesos(57), search(october.copy(categoryIds = setOf(savings)), 100).first().totals.expense)
        }
}
