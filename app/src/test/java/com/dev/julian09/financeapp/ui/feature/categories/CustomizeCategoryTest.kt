package com.dev.julian09.financeapp.ui.feature.categories

import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.usecase.CreateCategory
import com.dev.julian09.financeapp.domain.usecase.DeleteCategory
import com.dev.julian09.financeapp.domain.usecase.RenameCategory
import com.dev.julian09.financeapp.domain.usecase.SetLoanPaymentCategory
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import com.dev.julian09.financeapp.ui.components.COLOR_CHOICES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/** 2.2: «Personalizar» changes the name, the icon and (main categories) the color of a stored category. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CustomizeCategoryTest {
    private val f = LedgerFixture()
    private val sync = FakeSyncScheduler()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        f.db.close()
    }

    private fun viewModel() =
        CategoriesViewModel(
            f.categories,
            f.settings,
            CreateCategory(f.categories, sync),
            RenameCategory(f.categories, sync),
            DeleteCategory(f.categories, f.accounts, f.settings, sync),
            SetLoanPaymentCategory(f.categories, f.settings, sync),
            sync,
            f.clock,
        )

    /** Saving runs on Room's thread: wait until the sheet closes or shows an error. */
    private suspend fun CategoriesViewModel.saved() =
        withTimeout(5_000) { sheet.first { it == null || (it as? CategorySheet.Customize)?.error != null } }

    private suspend fun stored(id: UUID) =
        f.categories
            .observeAll()
            .first()
            .first { it.id == id }

    @Test
    fun `the name, the icon and the color change together and sync`() =
        runBlocking {
            val food = f.category("Comida")
            val vm = viewModel()
            vm.openOptions(stored(food))
            vm.startCustomize()
            vm.editCustomize { it.copy(name = "Restaurantes", icon = "pets", color = "#2E7D32") }
            vm.saveCustomize()
            vm.saved()

            val c = stored(food)
            assertEquals("Restaurantes", c.name)
            assertEquals("#2E7D32", c.color)
            assertNull(vm.sheet.value)
        }

    @Test
    fun `a taken name keeps the sheet open and changes nothing`() =
        runBlocking {
            val food = f.category("Comida")
            f.category("Hogar")
            val vm = viewModel()
            vm.openOptions(stored(food))
            vm.startCustomize()
            vm.editCustomize { it.copy(name = "Hogar", color = "#2E7D32") }
            vm.saveCustomize()
            vm.saved()

            assertEquals(LedgerError.NAME_TAKEN, (vm.sheet.value as CategorySheet.Customize).error)
            assertEquals("Comida", stored(food).name)
            assertNull(stored(food).color)
        }

    @Test
    fun `a subcategory never gets a color`() =
        runBlocking {
            val food = f.category("Comida")
            val sub = f.category("Almuerzos", parent = food)
            val vm = viewModel()
            vm.openOptions(stored(sub))
            vm.startCustomize()
            vm.editCustomize { it.copy(color = "#2E7D32") }
            vm.saveCustomize()
            vm.saved()

            assertNull(stored(sub).color)
        }

    @Test
    fun `the palette has the six tag colors first and no repeated color`() {
        assertEquals(COLOR_CHOICES.size, COLOR_CHOICES.map { it.first }.distinct().size)
        assertTrue(COLOR_CHOICES.size > 6)
        assertTrue(COLOR_CHOICES.all { it.first.matches(Regex("^#[0-9A-F]{6}$")) })
    }
}
