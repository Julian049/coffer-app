package com.dev.julian09.financeapp.ui.feature.tags

import androidx.lifecycle.SavedStateHandle
import com.dev.julian09.financeapp.data.local.FixedClock
import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.usecase.CreateTag
import com.dev.julian09.financeapp.domain.usecase.DeleteTag
import com.dev.julian09.financeapp.domain.usecase.ObserveHomeDashboard
import com.dev.julian09.financeapp.domain.usecase.ObservePendingPayments
import com.dev.julian09.financeapp.domain.usecase.PinTag
import com.dev.julian09.financeapp.domain.usecase.RenameTag
import com.dev.julian09.financeapp.domain.usecase.UpdateSettings
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import com.dev.julian09.financeapp.ui.feature.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

/** S9-QA02: AT-HU10-1 and 2 (REQ-ETQ-01 to 04). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TagSummaryTest {
    private val f = LedgerFixture(FixedClock(Instant.parse("2026-10-16T12:00:00Z")))
    private val sync = FakeSyncScheduler()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        f.db.close()
    }

    private fun day(date: String) = Instant.parse("${date}T15:00:00Z")

    private fun detail(id: UUID) =
        TagDetailViewModel(
            SavedStateHandle(mapOf("id" to id.toString())),
            f.tags,
            f.transactions,
            f.categories,
            f.accounts,
            RenameTag(f.tags, sync),
            DeleteTag(f.tags, sync),
            PinTag(f.tags, sync),
            f.clock,
        )

    @Test
    fun aTagShowsTheTotalAndTheDetailOfItsMovementsAcrossCategories() =
        runBlocking {
            val visa = f.account("Visa")
            val trip = UUID.randomUUID()
            assertNull(CreateTag(f.tags, sync)(Tag(trip, "#Viaje Cartagena")))
            val lodging = f.category("Alojamiento")
            val transport = f.category("Transporte")
            val food = f.category("Comida")
            f.tx("EXPENSE", 680_000, visa, day("2026-10-04"), lodging, tag = trip)
            f.tx("EXPENSE", 300_000, visa, day("2026-10-02"), transport, tag = trip)
            f.tx("EXPENSE", 96_000, visa, day("2026-10-05"), food, tag = trip)
            f.tx("EXPENSE", 50_000, visa, day("2026-10-05"), food)
            f.tx("EXPENSE", 70_000, visa, day("2026-09-28"), food, tag = trip)

            val s = detail(trip).state.filterNotNull().first()
            assertEquals("Viaje Cartagena", s.tag.name)
            assertEquals(Money.ofPesos(-1_076_000), s.net)
            assertEquals(3, s.count)
            assertEquals(listOf("Alojamiento", "Transporte", "Comida"), s.byCategory.map { it.first.name })

            val vm = detail(trip)
            vm.state.filterNotNull().first()
            vm.selectRange(TagRange.LAST_MONTH)
            assertEquals(
                Money.ofPesos(-70_000),
                vm.state
                    .filterNotNull()
                    .first { it.range == TagRange.LAST_MONTH }
                    .net,
            )
            assertEquals(LedgerError.NAME_TAKEN, CreateTag(f.tags, sync)(Tag(UUID.randomUUID(), "viaje cartagena")))
        }

    @Test
    fun aPinnedTagShowsItsMonthTotalOnHome() =
        runBlocking {
            val cash = f.account("Efectivo", 500_000)
            val outings = UUID.randomUUID()
            CreateTag(f.tags, sync)(Tag(outings, "Salidas"))
            f.tx("EXPENSE", 182_000, cash, day("2026-10-10"), f.category("Entretenimiento"), tag = outings)
            PinTag(f.tags, sync)(outings, true)

            val home =
                HomeViewModel(
                    ObserveHomeDashboard(
                        f.accounts,
                        f.transactions,
                        ObservePendingPayments(f.recurring, f.settings, f.clock),
                        f.cardDues,
                        f.loanDues,
                        f.clock,
                    ),
                    f.categories,
                    f.tags,
                    f.clock,
                    f.settings,
                    UpdateSettings(f.settings, FakeSyncScheduler()),
                ).state.filterNotNull().first()
            val line = home.pinned.single()
            assertEquals("Salidas", line.tag.name)
            assertEquals(Money.ofPesos(-182_000), line.net)
            assertEquals(1, line.count)
        }

    @Test
    fun deletingATagLeavesItsMovementsWithoutATag() =
        runBlocking {
            val cash = f.account("Efectivo")
            val work = UUID.randomUUID()
            CreateTag(f.tags, sync)(Tag(work, "Trabajo"))
            val expense = f.tx("EXPENSE", 54_000, cash, day("2026-10-10"), f.category("Comida"), tag = work)
            DeleteTag(f.tags, sync)(work)
            assertTrue(
                f.tags
                    .observeAll()
                    .first()
                    .isEmpty(),
            )
            val row = f.db.transactions().get(expense)!!
            assertNull(row.tagId)
            assertEquals("PENDING", row.syncState.name)
        }
}
