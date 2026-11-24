package com.dev.julian09.financeapp.ui.feature.mail

import androidx.lifecycle.SavedStateHandle
import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.model.DraftRule
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RuleLevel
import com.dev.julian09.financeapp.domain.model.RuleOrigin
import com.dev.julian09.financeapp.domain.usecase.DeleteDraftRule
import com.dev.julian09.financeapp.domain.usecase.MerchantSuggestions
import com.dev.julian09.financeapp.domain.usecase.ObserveDraftRules
import com.dev.julian09.financeapp.domain.usecase.ReorderDraftRules
import com.dev.julian09.financeapp.domain.usecase.SaveDraftRule
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/** 2.5-04: Registro automático, Nueva regla, Ya existe and Editar regla over the real repositories. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DraftRuleScreensTest {
    private val f = LedgerFixture()
    private val sync = FakeSyncScheduler()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        f.db.close()
    }

    private fun rule(
        merchant: String,
        category: UUID,
        position: Int,
        level: RuleLevel = RuleLevel.SURE,
        origin: RuleOrigin = RuleOrigin.USER,
        amount: Money? = null,
    ) = DraftRule(UUID.randomUUID(), merchant, merchant.lowercase(), amount, null, merchant, category, level, origin, position = position)

    private fun list() =
        DraftRulesViewModel(ObserveDraftRules(f.draftRules), f.accounts, f.categories, ReorderDraftRules(f.draftRules, sync))

    private fun form(id: UUID? = null) =
        DraftRuleFormViewModel(
            SavedStateHandle(listOfNotNull(id?.let { "id" to it.toString() }).toMap()),
            f.accounts,
            f.categories,
            f.draftRules,
            MerchantSuggestions(f.transactions, f.drafts, f.draftRules),
            SaveDraftRule(f.draftRules, sync),
            DeleteDraftRule(f.draftRules, f.clock, sync),
            f.clock,
        )

    @Test
    fun theSummaryCountsSureAndProbableRules() {
        assertEquals("Coffer las crea cuando un movimiento se repite", draftRulesSummary(0, 0))
        assertEquals("1 regla · entra directo al historial", draftRulesSummary(1, 0))
        assertEquals("4 reglas · entran directo al historial", draftRulesSummary(4, 0))
        assertEquals("6 reglas · 4 directo, 2 por revisar", draftRulesSummary(4, 2))
    }

    @Test
    fun theListGroupsByLevelAndADragSavesTheNewOrder() =
        runBlocking {
            val food = f.category("Restaurantes")
            val uber = rule("Uber", food, 0)
            val corral = rule("El Corral", food, 1)
            val rappi = rule("Rappi", food, 2, RuleLevel.PROBABLE, RuleOrigin.LEARNED)
            f.draftRules.saveAll(listOf(uber, corral, rappi))
            val vm = list()
            val state = withTimeout(5_000) { vm.state.first { it.loaded && it.sure.size == 2 } }
            assertEquals(listOf("Uber", "El Corral"), state.sure.map { it.rule.merchant })
            assertEquals(listOf("Rappi"), state.probable.map { it.rule.merchant })
            assertEquals(
                "Restaurantes",
                state.sure
                    .first()
                    .category
                    ?.name,
            )

            vm.move(1, 0)

            assertEquals(
                listOf("El Corral", "Uber"),
                vm.state.value.sure
                    .map { it.rule.merchant },
            )
            val stored = withTimeout(5_000) { f.draftRules.observeAll().first { it.first().id == corral.id } }
            assertEquals(listOf(corral.id, uber.id, rappi.id), stored.map { it.id })
        }

    @Test
    fun aNewRuleSavesOnlyWhenComplete() =
        runBlocking {
            val food = f.category("Restaurantes")
            val vm = form()
            withTimeout(5_000) { vm.state.first { it.loaded } }
            vm.merchant("Uber")
            vm.note("Viaje en Uber")
            assertFalse(vm.state.value.canSave)
            vm.category(food)
            vm.exactAmount(true)
            assertFalse("Exacto needs an amount", vm.state.value.canSave)
            vm.amount("$ 9.800")
            assertTrue(vm.state.value.canSave)

            vm.saveRule()

            withTimeout(5_000) { vm.state.first { it.done } }
            val saved =
                f.draftRules
                    .observeAll()
                    .first()
                    .single()
            assertEquals(Money.ofPesos(9_800), saved.amount)
            assertEquals(RuleLevel.SURE, saved.level)
            assertEquals(RuleOrigin.USER, saved.origin)
        }

    @Test
    fun aRepeatedRuleShowsTheOtherOneUntilTheConditionsChange() =
        runBlocking {
            val food = f.category("Restaurantes")
            val lunch = rule("El Corral", food, 0).copy(note = "Almuerzo del trabajo")
            f.draftRules.saveAll(listOf(lunch))
            val vm = form()
            withTimeout(5_000) { vm.state.first { it.loaded } }
            vm.merchant("EL CORRAL")
            vm.note("Otra")
            vm.category(food)

            vm.saveRule()

            val shown = withTimeout(5_000) { vm.state.first { it.duplicate != null } }
            assertEquals("Almuerzo del trabajo", shown.duplicate?.note)
            assertFalse(shown.canSave)
            assertFalse(shown.done)

            vm.exactAmount(true)
            assertNull(vm.state.value.duplicate)
        }

    @Test
    fun savingAProbableRuleMakesItTheUsersAndSure() =
        runBlocking {
            val food = f.category("Domicilios")
            val rappi = rule("Rappi", food, 0, RuleLevel.PROBABLE, RuleOrigin.LEARNED)
            f.draftRules.saveAll(listOf(rappi))
            val vm = form(rappi.id)
            val state = withTimeout(5_000) { vm.state.first { it.loaded } }
            assertTrue(state.turnsSure)
            assertTrue(state.learnedNotice!!.startsWith("La creó Coffer"))
            assertEquals("Rappi", state.merchant)

            vm.saveRule()

            withTimeout(5_000) { vm.state.first { it.done } }
            val saved = f.draftRules.get(rappi.id)!!
            assertEquals(RuleLevel.SURE, saved.level)
            assertEquals(RuleOrigin.USER, saved.origin)
        }

    @Test
    fun deletingAUsersRuleRemovesIt() =
        runBlocking {
            val food = f.category("Restaurantes")
            val uber = rule("Uber", food, 0)
            f.draftRules.saveAll(listOf(uber))
            val vm = form(uber.id)
            withTimeout(5_000) { vm.state.first { it.loaded } }

            vm.requestDelete()
            assertTrue(vm.state.value.confirmingDelete)
            vm.confirmDelete()

            withTimeout(5_000) { vm.state.first { it.done } }
            assertNull(f.draftRules.get(uber.id))
        }

    @Test
    fun theAliasesNameOnlyWhatIsNotTyped() {
        assertNull(aliasesLine("Uber", listOf("Uber")))
        assertEquals("En tus correos aparece como «UBER *TRIP».", aliasesLine("Uber", listOf("UBER *TRIP", "Uber")))
        assertEquals("En tus correos aparece como «A», «B» y «C».", aliasesLine("x", listOf("A", "B", "C")))
    }
}
