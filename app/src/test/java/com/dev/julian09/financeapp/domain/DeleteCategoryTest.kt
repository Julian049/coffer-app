package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.usecase.CreateCategory
import com.dev.julian09.financeapp.domain.usecase.DeleteCategory
import com.dev.julian09.financeapp.domain.usecase.FINANCIAL_CHARGES
import com.dev.julian09.financeapp.domain.usecase.RenameCategory
import com.dev.julian09.financeapp.domain.usecase.deleteTargets
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/** REQ-CAT-02, 03 and the PO decisions of 2026-10-17. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeleteCategoryTest {
    private val f = LedgerFixture()
    private val scheduler = FakeSyncScheduler()
    private val delete = DeleteCategory(f.categories, f.accounts, f.settings, scheduler)

    @After
    fun close() = f.db.close()

    private suspend fun names() =
        f.categories
            .observeAll()
            .first()
            .map { it.name }
            .toSet()

    @Test
    fun aParentGoesWithItsSubcategoriesAndAllMovementsMoveToTheTargetItself() =
        runBlocking {
            val account = f.account("Débito")
            val food = f.category("Comida")
            val restaurants = f.category("Restaurantes", parent = food)
            val home = f.category("Hogar")
            val rent = f.category("Arriendo", parent = home)
            f.tx("EXPENSE", 10_000, account, category = food)
            f.tx("EXPENSE", 20_000, account, category = restaurants)

            assertEquals(2, f.categories.transactionCount(food))
            assertEquals(LedgerError.TARGET_REQUIRED, delete(food, null))
            // Only main categories of the same kind when a main category is deleted
            assertEquals(listOf("Hogar"), deleteTargets(f.categories.observeAll().first(), food).map { it.name })
            assertEquals(LedgerError.TARGET_INVALID, delete(food, rent))

            assertNull(delete(food, home))
            assertEquals(setOf("Hogar", "Arriendo"), names())
            val moved = f.transactions.observeRecent(10).first()
            assertEquals(listOf(home, home), moved.map { it.categoryId })
            assertEquals(1, scheduler.syncRequests)
        }

    @Test
    fun withoutMovementsNoTargetIsNeeded() =
        runBlocking {
            val food = f.category("Comida")
            f.category("Restaurantes", parent = food)
            assertNull(delete(food, null))
            assertEquals(emptySet<String>(), names())
        }

    @Test
    fun financialChargesStaysWhileACardOrLoanExists() =
        runBlocking {
            val charges = f.category("Intereses y cargos financieros", systemKey = FINANCIAL_CHARGES)
            val loan = f.account("Préstamo", type = "LOAN")
            assertEquals(LedgerError.SYSTEM_CATEGORY_IN_USE, delete(charges, null))

            f.db.accounts().markDeleted(loan, f.clock.now())
            assertNull(delete(charges, null))
        }

    @Test
    fun createAndRenameKeepOneLevelTheKindAndUniqueNames() =
        runBlocking {
            val create = CreateCategory(f.categories, scheduler)
            val food = f.category("Comida")
            val restaurants = f.category("Restaurantes", parent = food)
            val salary = f.category("Salario", kind = "INCOME")

            fun sub(
                name: String,
                parent: UUID?,
                kind: CategoryKind = CategoryKind.EXPENSE,
            ) = Category(UUID.randomUUID(), name, kind, parent)

            assertEquals(LedgerError.ONE_LEVEL_ONLY, create(sub("Almuerzos", restaurants)))
            assertEquals(LedgerError.KIND_MISMATCH, create(sub("Bonos", salary)))
            assertEquals(LedgerError.NAME_TAKEN, create(sub(" restaurantes ", food)))
            assertNull(create(sub("Domicilios", food)))

            val rename = RenameCategory(f.categories, scheduler)
            assertEquals(LedgerError.NAME_TAKEN, rename(restaurants, "DOMICILIOS"))
            assertNull(rename(restaurants, "Restaurantes y cafés"))
            assertEquals(setOf("Comida", "Restaurantes y cafés", "Domicilios", "Salario"), names())
        }
}
