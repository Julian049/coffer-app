package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.local.entity.TransactionDraftEntity
import com.dev.julian09.financeapp.domain.finance.DraftLevel
import com.dev.julian09.financeapp.domain.finance.matchRule
import com.dev.julian09.financeapp.domain.model.DraftRule
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RuleLevel
import com.dev.julian09.financeapp.domain.model.RuleOrigin
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.ApplyDraft
import com.dev.julian09.financeapp.domain.usecase.AutoApplyDrafts
import com.dev.julian09.financeapp.domain.usecase.DeleteDraftRule
import com.dev.julian09.financeapp.domain.usecase.DraftRuleInput
import com.dev.julian09.financeapp.domain.usecase.MerchantSuggestions
import com.dev.julian09.financeapp.domain.usecase.ObserveDraftRules
import com.dev.julian09.financeapp.domain.usecase.ReorderDraftRules
import com.dev.julian09.financeapp.domain.usecase.SaveDraftRule
import com.dev.julian09.financeapp.domain.usecase.SaveRuleResult
import com.dev.julian09.financeapp.domain.usecase.SetLastDigits
import com.dev.julian09.financeapp.domain.usecase.SuggestDraft
import com.dev.julian09.financeapp.domain.usecase.TransactionValidator
import com.dev.julian09.financeapp.domain.usecase.UndoDraft
import com.dev.julian09.financeapp.domain.usecase.UpdateTransaction
import com.dev.julian09.financeapp.ui.FakeDraftPrefs
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import com.dev.julian09.financeapp.ui.draftRecurring
import com.dev.julian09.financeapp.ui.learnRules
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

/** roadmap-v2.5 «Registro automático»: rules learned from the user's decisions, and the user's own rules. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DraftRulesTest {
    private val f = LedgerFixture()
    private val prefs = FakeDraftPrefs()
    private val sync = FakeSyncScheduler()
    private val learn = learnRules(f)
    private val suggest = SuggestDraft(f.accounts, f.transactions, f.mailRules, prefs, f.draftRules)
    private val validator = TransactionValidator(f.accounts, f.categories)
    private val dr = draftRecurring(f, prefs, sync)
    private val apply = ApplyDraft(f.drafts, suggest, validator, sync, dr, learn)
    private val auto = AutoApplyDrafts(f.drafts, suggest, validator, prefs, sync, dr, learn)
    private val undo = UndoDraft(f.transactions, f.drafts, prefs, f.clock, sync, f.recurring, learn)
    private val update = UpdateTransaction(f.transactions, validator, sync)
    private val save = SaveDraftRule(f.draftRules, sync)
    private val observe = ObserveDraftRules(f.draftRules)
    private var hour = 0L

    @After
    fun close() = f.db.close()

    private suspend fun card(digits: String = "2309") = f.account("Tarjeta $digits").also { SetLastDigits(f.accounts, sync)(it, digits) }

    private suspend fun draft(
        merchant: String,
        pesos: Long,
        digits: String = "2309",
        type: String = "EXPENSE",
        source: String = "BANCOLOMBIA",
        at: Instant = Instant.parse("2026-10-01T12:00:00Z").plusSeconds(3600 * ++hour),
    ): UUID {
        val id = UUID.randomUUID()
        f.db.drafts().upsert(
            listOf(TransactionDraftEntity(id, source, type, pesos * 10_000, at, merchant, digits, at, at, syncState = SyncState.SYNCED)),
        )
        return id
    }

    /** Revisar transacción: the form saves what the user chose, the draft goes and the rules learn. */
    private suspend fun reviewDraft(
        id: UUID,
        note: String,
        category: UUID,
        account: UUID,
    ): Transaction {
        val d = f.drafts.get(id)!!
        val t =
            Transaction(
                UUID.randomUUID(),
                d.type,
                d.amount,
                d.occurredAt,
                account,
                categoryId = category,
                note = note,
                sourceMerchant = d.merchant,
                draftId = d.id,
            )
        f.drafts.apply(d.id, t)
        learn(t)
        return t
    }

    private suspend fun review(
        merchant: String,
        pesos: Long,
        note: String,
        category: UUID,
        account: UUID,
        digits: String = "2309",
        type: String = "EXPENSE",
    ) = reviewDraft(draft(merchant, pesos, digits, type), note, category, account)

    private suspend fun rules() = f.draftRules.observeAll().first()

    private fun SaveRuleResult.rule() = (this as SaveRuleResult.Saved).rule

    @Test
    fun threeEqualUbersWithAnyAmountMakeASureRuleThatRegistersTheNext() =
        runBlocking {
            val acc = card()
            val transport = f.category("Transporte")
            review("UBER", 12_000, "Uber", transport, acc)
            review("Uber", 15_500, "Uber", transport, acc)
            review("UBER ", 9_800, "Uber", transport, acc)
            val r = rules().single()
            assertEquals(RuleLevel.SURE, r.level)
            assertEquals(RuleOrigin.LEARNED, r.origin)
            assertNull(r.amount)

            val next = draft("UBER", 21_300)
            assertEquals(1, auto())
            assertNull(f.drafts.get(next))
            val made = f.transactions.byDraft(next).single()
            assertEquals("Uber", made.note)
            assertEquals(transport, made.categoryId)
        }

    @Test
    fun twoEqualMakeAProbableRuleThatOnlyFillsAndAThirdPromotesIt() =
        runBlocking {
            val acc = card()
            val food = f.category("Comida")
            review("RAPPI", 30_000, "Rappi", food, acc)
            review("RAPPI", 42_000, "Rappi", food, acc)
            assertEquals(RuleLevel.PROBABLE, rules().single().level)

            val next = draft("RAPPI", 25_000)
            assertEquals(0, auto())
            val s = suggest(f.drafts.get(next)!!)
            assertEquals("Rappi", s.note)
            assertEquals(food, s.categoryId)
            assertEquals(DraftLevel.PROBABLE, s.level)

            assertTrue(apply(next))
            assertEquals(RuleLevel.SURE, rules().single().level)
        }

    @Test
    fun aRepeatedAmountAmongMixedOutcomesMakesARuleForThatAmountOnly() =
        runBlocking {
            val acc = card()
            val lunch = f.category("Almuerzo")
            review("EXITO", 10_500, "Almuerzo", lunch, acc)
            review("EXITO", 80_000, "Mercado", f.category("Mercado"), acc)
            review("Éxito", 10_500, "Almuerzo", lunch, acc)
            review("EXITO", 45_000, "Aseo", f.category("Aseo"), acc)
            review("EXITO", 10_500, "Almuerzo", lunch, acc)
            val r = rules().single()
            assertEquals(RuleLevel.SURE, r.level)
            assertEquals(Money.ofPesos(10_500), r.amount)

            val other = draft("EXITO", 23_000)
            val same = draft("EXITO", 10_500)
            assertEquals(1, auto())
            assertNotNull(f.drafts.get(other))
            assertNull(f.drafts.get(same))
        }

    @Test
    fun theAccountIsAConditionOnlyWhenTheMovementsShareIt() =
        runBlocking {
            val a = card("1111")
            val b = card("2222")
            val subs = f.category("Suscripciones")
            repeat(3) { review("NETFLIX", 40_000L + it, "Netflix", subs, a, "1111") }
            review("SPOTIFY", 17_000, "Spotify", subs, a, "1111")
            review("SPOTIFY", 17_000, "Spotify", subs, b, "2222")
            review("SPOTIFY", 17_000, "Spotify", subs, a, "1111")
            assertEquals(a, rules().single { it.merchantKey == "netflix" }.accountId)
            assertNull(rules().single { it.merchantKey == "spotify" }.accountId)
        }

    @Test
    fun aNewRuleGoesAboveTheLessSpecificOnesAndManualOrderStays() =
        runBlocking {
            val acc = card()
            val c = f.category("Varios")
            val general = save(DraftRuleInput(merchant = "A", note = "A", categoryId = c)).rule()
            val both =
                save(
                    DraftRuleInput(merchant = "B", amount = Money.ofPesos(1_000), accountId = acc, note = "B", categoryId = c),
                ).rule()
            val one = save(DraftRuleInput(merchant = "C", accountId = acc, note = "C", categoryId = c)).rule()
            assertEquals(listOf(both.id, one.id, general.id), observe().first().sure.map { it.id })

            ReorderDraftRules(f.draftRules, sync)(listOf(general.id, both.id, one.id))
            val d = save(DraftRuleInput(merchant = "D", amount = Money.ofPesos(500), note = "D", categoryId = c)).rule()
            assertEquals(listOf(d.id, general.id, both.id, one.id), observe().first().sure.map { it.id })
            assertEquals(listOf(0, 1, 2, 3), observe().first().sure.map { it.position })
        }

    @Test
    fun aSureRuleBeatsAProbableOneAndThenTheLowerPositionWins() =
        runBlocking {
            val acc = UUID.randomUUID()
            val c = UUID.randomUUID()
            val rules =
                listOf(
                    DraftRule(
                        UUID.randomUUID(),
                        "Uber",
                        "uber",
                        note = "P",
                        categoryId = c,
                        level = RuleLevel.PROBABLE,
                        origin = RuleOrigin.USER,
                        position = 0,
                    ),
                    DraftRule(
                        UUID.randomUUID(),
                        "Uber",
                        "uber",
                        note = "S5",
                        categoryId = c,
                        level = RuleLevel.SURE,
                        origin = RuleOrigin.USER,
                        position = 5,
                    ),
                    DraftRule(
                        UUID.randomUUID(),
                        "Uber",
                        "uber",
                        amount = Money.ofPesos(10_000),
                        note = "S2",
                        categoryId = c,
                        level = RuleLevel.SURE,
                        origin = RuleOrigin.USER,
                        position = 2,
                    ),
                )
            assertEquals("S2", matchRule("UBER", Money.ofPesos(10_000), acc, rules)?.note)
            assertEquals("S5", matchRule("UBER", Money.ofPesos(9_000), acc, rules)?.note)
            assertEquals("P", matchRule("UBER", Money.ofPesos(9_000), acc, rules.map { it.copy(level = RuleLevel.PROBABLE) })?.note)
        }

    @Test
    fun undoingWhatALearnedRuleRegisteredTurnsItOffUntilNewMovementsTeachIt() =
        runBlocking {
            val acc = card()
            val transport = f.category("Transporte")
            repeat(3) { review("UBER", 10_000L + it, "Uber", transport, acc) }
            val next = draft("UBER", 20_000)
            assertEquals(1, auto())
            assertTrue(undo(f.transactions.byDraft(next).single()))
            val off = rules().single()
            assertFalse(off.enabled)
            assertEquals(f.clock.now(), off.learnedSince)

            // The same three earlier movements never bring it back
            f.clock.instant = f.clock.instant.plusSeconds(3600)
            reviewDraft(next, "Uber", transport, acc)
            assertFalse(rules().single().enabled)
            review("UBER", 31_000, "Uber", transport, acc)
            review("UBER", 32_000, "Uber", transport, acc)
            val back = rules().single()
            assertTrue(back.enabled)
            assertEquals(RuleLevel.SURE, back.level)
        }

    @Test
    fun correctingWhatALearnedRuleRegisteredTurnsItOffButNeverAUserRule() =
        runBlocking {
            val acc = card()
            val transport = f.category("Transporte")
            val food = f.category("Comida")
            repeat(3) { review("UBER", 10_000L + it, "Uber", transport, acc) }
            val next = draft("UBER", 20_000)
            assertEquals(1, auto())
            val made = f.transactions.byDraft(next).single()
            val corrected = made.copy(categoryId = food)
            update(corrected)
            learn(corrected, made)
            assertFalse(rules().single { it.origin == RuleOrigin.LEARNED }.enabled)

            val user = save(DraftRuleInput(merchant = "DIDI", note = "Didi", categoryId = transport)).rule()
            val didi = draft("DIDI", 8_000)
            assertEquals(1, auto())
            val byRule = f.transactions.byDraft(didi).single()
            assertEquals("Didi", byRule.note)
            val renamed = byRule.copy(note = "Taxi")
            update(renamed)
            learn(renamed, byRule)
            assertTrue(undo(renamed))
            assertTrue(f.draftRules.get(user.id)!!.enabled)
        }

    @Test
    fun incomesAndTransfersAreNeverRegisteredByRules() =
        runBlocking {
            val acc = card()
            val salary = f.category("Salario", kind = "INCOME")
            repeat(3) { review("Bancolombia", 1_000_000, "", salary, acc, type = "INCOME") }
            assertTrue(rules().isEmpty())
            val income = draft("Bancolombia", 1_000_000, type = "INCOME")
            // learn() alone said SURE before 2.5: now it only suggests
            assertEquals(DraftLevel.PROBABLE, suggest(f.drafts.get(income)!!).level)
            save(DraftRuleInput(merchant = "Bancolombia", note = "Nómina", categoryId = salary))
            assertNotEquals("Nómina", suggest(f.drafts.get(income)!!).note)

            val rappi = f.account("RappiCuenta")
            f.mailRules.save("RAPPICUENTA", null, rappi)
            val at = Instant.parse("2026-10-29T15:36:00Z")
            draft("@rp1077", 500_000, at = at)
            draft("Bancolombia", 500_000, digits = "", type = "INCOME", source = "RAPPICUENTA", at = at.plusSeconds(40))
            save(DraftRuleInput(merchant = "@rp1077", note = "Ahorro", categoryId = salary))
            assertEquals(0, auto())
            // The pending income and the transfer (one draft for both halves) still wait
            assertEquals(
                listOf(TransactionType.INCOME, TransactionType.TRANSFER),
                f.drafts
                    .observeAll()
                    .first()
                    .map { it.type }
                    .sorted(),
            )
        }

    @Test
    fun withTheSwitchOffNothingIsRegisteredButTheDraftIsFilled() =
        runBlocking {
            val acc = card()
            val transport = f.category("Transporte")
            repeat(3) { review("UBER", 10_000L + it, "Uber", transport, acc) }
            prefs.setAutoSave(false)
            val next = draft("UBER", 20_000)
            assertEquals(0, auto())
            val s = suggest(f.drafts.get(next)!!)
            assertEquals("Uber", s.note)
            assertEquals(transport, s.categoryId)
        }

    @Test
    fun saveFindsDuplicatesAndAnEditMakesALearnedRuleTheUsersAndSure() =
        runBlocking {
            val acc = card()
            val food = f.category("Comida")
            val first = save(DraftRuleInput(merchant = "Uber", note = "Uber", categoryId = food)).rule()
            val dup = save(DraftRuleInput(merchant = " ÚBER ", note = "Otro", categoryId = food))
            assertEquals(first.id, (dup as SaveRuleResult.Duplicate).existing.id)
            assertEquals(SaveRuleResult.Invalid, save(DraftRuleInput(merchant = "  ", note = "x", categoryId = food)))

            review("RAPPI", 30_000, "Rappi", food, acc)
            review("RAPPI", 42_000, "Rappi", food, acc)
            val learned = rules().single { it.merchantKey == "rappi" }
            assertEquals(RuleLevel.PROBABLE, learned.level)
            val edited = save(DraftRuleInput(id = learned.id, merchant = "RAPPI", note = "Domicilios", categoryId = food)).rule()
            assertEquals(learned.id, edited.id)
            assertEquals(RuleOrigin.USER, edited.origin)
            assertEquals(RuleLevel.SURE, edited.level)
            assertTrue(observe().first().sure.any { it.id == learned.id })
        }

    @Test
    fun deletingALearnedRuleOnlyTurnsItOff() =
        runBlocking {
            val acc = card()
            val food = f.category("Comida")
            review("RAPPI", 30_000, "Rappi", food, acc)
            review("RAPPI", 42_000, "Rappi", food, acc)
            val learned = observe().first().probable.single()
            val delete = DeleteDraftRule(f.draftRules, f.clock, sync)
            delete(learned.id)
            assertEquals(0, observe().first().let { it.sure.size + it.probable.size })
            assertFalse(f.draftRules.get(learned.id)!!.enabled)
            val user = save(DraftRuleInput(merchant = "DIDI", note = "Didi", categoryId = food)).rule()
            delete(user.id)
            assertNull(f.draftRules.get(user.id))

            // Without an enabled rule, Rappi is offered again, as the emails spell it
            val merchants = MerchantSuggestions(f.transactions, f.drafts, f.draftRules)
            assertEquals(listOf("RAPPI"), merchants())
            assertEquals(listOf("RAPPI"), merchants.merchantAliases("rappi"))
        }
}
