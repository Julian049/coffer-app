package com.dev.julian09.financeapp.data.local

import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.sync.SyncTables
import com.dev.julian09.financeapp.domain.model.DraftRule
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RuleLevel
import com.dev.julian09.financeapp.domain.model.RuleOrigin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

/** The draft rules: stored by position, deleted as tombstones, and the same after a trip through the sync wire. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DraftRuleRepositoryTest {
    private val f = LedgerFixture()

    @After
    fun close() = f.db.close()

    private fun rule(
        merchant: String,
        position: Int,
    ) = DraftRule(
        id = UUID.randomUUID(),
        merchant = merchant,
        merchantKey = merchant.lowercase(),
        amount = Money.ofPesos(12_000),
        accountId = UUID.randomUUID(),
        note = "Almuerzo",
        categoryId = UUID.randomUUID(),
        level = RuleLevel.PROBABLE,
        origin = RuleOrigin.LEARNED,
        learnedSince = Instant.parse("2026-11-01T00:00:00Z"),
        position = position,
    )

    @Test
    fun theRulesComeByPositionAndADeletedOneDisappears() =
        runBlocking {
            val second = rule("Rappi", 1)
            val first = rule("Uber", 0)
            f.draftRules.saveAll(listOf(second, first))

            assertEquals(
                listOf("Uber", "Rappi"),
                f.draftRules
                    .observeAll()
                    .first()
                    .map { it.merchant },
            )
            assertEquals(first.copy(updatedAt = f.clock.now()), f.draftRules.get(first.id))

            f.draftRules.delete(first.id)
            assertEquals(
                listOf("Rappi"),
                f.draftRules
                    .observeAll()
                    .first()
                    .map { it.merchant },
            )
            assertNull(f.draftRules.get(first.id))
            assertEquals(
                1,
                f.db
                    .draftRules()
                    .pending(10)
                    .count { it.id == first.id && it.deletedAt != null },
            )
        }

    @Test
    fun aRuleTravelsAsTheContractsDraftRuleData() =
        runBlocking {
            val any = rule("Uber", 3).copy(amount = null, accountId = null, learnedSince = null, level = RuleLevel.SURE)
            val exact = rule("Rappi", 0)
            f.draftRules.saveAll(listOf(any, exact))
            val table = SyncTables(f.db).draftRules

            val change = table.encode(f.db.draftRules().byId(exact.id)!!)
            assertEquals("draftRule", change.entity)
            assertEquals("\"12000.0000\"", change.data["amount"].toString())
            assertEquals("\"PROBABLE\"", change.data["level"].toString())
            assertEquals("true", change.data["enabled"].toString())

            f.db.draftRules().pending(10).forEach {
                assertEquals(it.copy(syncState = SyncState.SYNCED), table.decode(table.encode(it)))
            }
        }
}
