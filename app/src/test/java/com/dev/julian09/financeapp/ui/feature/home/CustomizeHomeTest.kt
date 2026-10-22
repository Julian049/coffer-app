package com.dev.julian09.financeapp.ui.feature.home

import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.data.local.entity.SettingsEntity
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.domain.model.HomeCard
import com.dev.julian09.financeapp.domain.model.HomeSection
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.homeLayout
import com.dev.julian09.financeapp.domain.usecase.CustomizeHome
import com.dev.julian09.financeapp.domain.usecase.UpdateSettings
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import com.dev.julian09.financeapp.ui.components.formatMoney
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** S11-QA04: Personalizar inicio (REQ-INI-04), the account order (M9) and the decimals (RNF-13). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CustomizeHomeTest {
    private val f = LedgerFixture()
    private val sync = FakeSyncScheduler()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        f.db.close()
    }

    @Test
    fun anEmptyLayoutKeepsHomeAsItWasAndUnknownCardsAreDropped() {
        assertEquals(HomeSection.entries.map { HomeCard(it.name, true) }, homeLayout(emptyList()))
        val stored = listOf(HomeCard("RECENT", false), HomeCard("WEATHER", true), HomeCard("BALANCE", true))
        val layout = homeLayout(stored)
        assertEquals(listOf("RECENT", "BALANCE"), layout.take(2).map { it.key })
        assertEquals(HomeSection.entries.size, layout.size)
        assertTrue(layout.drop(2).all { it.visible })
    }

    @Test
    fun eachChangeSavesItselfWithTheCardsAndTheOrderOfTheAccounts() =
        runBlocking {
            f.db.settings().upsert(SettingsEntity(UUID.randomUUID(), 5, true, "AVAILABLE", 0, "[]", Instant.EPOCH, Instant.EPOCH))
            val cash = f.account("Efectivo")
            val bank = f.account("Bancolombia")
            val vm = CustomizeHomeViewModel(f.settings, f.accounts, CustomizeHome(UpdateSettings(f.settings, sync), f.accounts))
            assertEquals(
                listOf(bank, cash),
                vm.state
                    .first { it.loaded }
                    .accounts
                    .map { it.id },
            )

            vm.setVisible("PINNED_TAGS", false)
            vm.moveCard(3, 0)
            vm.moveAccount(1, 0)

            // No Guardar: the last change reaches the database on its own
            val cards =
                withTimeout(5_000) {
                    f.settings
                        .observe()
                        .first { s -> s.homeCards.firstOrNull()?.key == "PENDING" }
                        .homeCards
                }
            assertEquals(false, cards.single { it.key == "PINNED_TAGS" }.visible)
            assertEquals(
                listOf(cash, bank),
                withTimeout(5_000) {
                    f.accounts
                        .observeActive()
                        .first { list -> list.map { it.id } == listOf(cash, bank) }
                        .map { it.id }
                },
            )
            assertEquals(
                SyncState.PENDING,
                f.db
                    .accounts()
                    .get(cash)!!
                    .syncState,
            )
        }

    @Test
    fun amountsShowTwoDecimalsWhenAsked() {
        val amount = Money.of(BigDecimal("1250000.5"))
        assertEquals("$1.250.001", formatMoney(amount, decimals = 0))
        assertEquals("$1.250.000,50", formatMoney(amount, decimals = 2))
    }
}
