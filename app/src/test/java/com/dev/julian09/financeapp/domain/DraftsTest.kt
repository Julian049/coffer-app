package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.local.entity.TransactionDraftEntity
import com.dev.julian09.financeapp.domain.model.MailProviders
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.ApplyDraft
import com.dev.julian09.financeapp.domain.usecase.DiscardDraft
import com.dev.julian09.financeapp.domain.usecase.SetLastDigits
import com.dev.julian09.financeapp.domain.usecase.SuggestDraft
import com.dev.julian09.financeapp.domain.usecase.TransactionValidator
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

/** S19-S20 (REQ-COR-04 to 08): a draft counts nowhere until applied, then exactly by its amount. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DraftsTest {
    private val f = LedgerFixture()
    private val sync = FakeSyncScheduler()
    private val suggest = SuggestDraft(f.accounts, f.transactions, f.mailRules)
    private val apply = ApplyDraft(f.drafts, suggest, TransactionValidator(f.accounts, f.categories), sync)

    @After
    fun close() = f.db.close()

    private suspend fun draft(
        merchant: String?,
        digits: String?,
        pesos: Long = 22_980,
        source: String = "BANCOLOMBIA",
        type: String = "EXPENSE",
        at: Instant = Instant.parse("2026-10-26T00:16:04Z"),
    ): UUID {
        val id = UUID.randomUUID()
        f.db.drafts().upsert(
            listOf(
                TransactionDraftEntity(
                    id,
                    source,
                    type,
                    pesos * 10_000,
                    at,
                    merchant,
                    digits,
                    at,
                    at,
                    syncState = SyncState.SYNCED,
                ),
            ),
        )
        return id
    }

    private suspend fun balance(account: UUID) =
        f.accounts
            .observeBalances()
            .first()
            .single { it.account.id == account }
            .balance

    @Test
    fun aDraftWithAKnownCardAndMerchantIsAppliedInOneTap() =
        runBlocking {
            val card = f.account("Efectivo", 500_000, type = "CASH")
            SetLastDigits(f.accounts, sync)(card, "5378")
            val market = f.category("Mercado")
            f.transactions.insert(
                Transaction(
                    UUID.randomUUID(),
                    TransactionType.EXPENSE,
                    Money.ofPesos(10_000),
                    f.clock.now(),
                    card,
                    categoryId = market,
                    note = "Tiendas D1",
                ),
            )
            val id = draft("Tiendas D1", "5378")
            val before = balance(card)

            assertEquals(before, balance(card))
            assertTrue(apply(id))
            assertEquals(before - Money.ofPesos(22_980), balance(card))
            val made =
                f.transactions
                    .observeRecent(10)
                    .first()
                    .single { it.amount == Money.ofPesos(22_980) }
            assertEquals(market, made.categoryId)
            assertEquals("Tiendas D1", made.note)
            assertNull(f.drafts.get(id))
            // The deletion travels to the server on the next push
            assertEquals(
                SyncState.PENDING,
                f.db
                    .drafts()
                    .pending(10)
                    .single()
                    .syncState,
            )
        }

    @Test
    fun withoutMatchingDigitsOrAKnownMerchantItOpensTheReview() =
        runBlocking {
            f.account("Bancolombia", 100_000)
            val unknownCard = draft("Tiendas D1", "0000")
            assertFalse(apply(unknownCard))
            assertEquals(1, f.drafts.count())
            val suggestion = suggest(f.drafts.get(unknownCard)!!)
            assertNull(suggestion.accountId)
            assertNull(suggestion.categoryId)

            DiscardDraft(f.drafts, sync)(unknownCard)
            assertEquals(0, f.drafts.count())
        }

    @Test
    fun aMailRuleWinsOverTheDigitsAndDigitsOnlyMatchTheProvidersAccountType() =
        runBlocking {
            val savings = f.account("Ahorros", 100_000)
            SetLastDigits(f.accounts, sync)(savings, "6858")
            val visa = f.account("Visa", type = "CREDIT_CARD")
            val rappi = f.account("RappiCuenta", 0)
            // T.Cred *6858 is a card: the savings account ending the same is not it
            val card = draft("EXITO", "6858", source = "BANCOLOMBIA_TC")
            assertNull(suggest(f.drafts.get(card)!!).accountId)
            f.mailRules.save("BANCOLOMBIA_TC", "6858", visa)
            assertEquals(visa, suggest(f.drafts.get(card)!!).accountId)
            // A provider without digits only has its rule
            val received = draft("Bancolombia", null, 50_000, source = "RAPPICUENTA", type = "INCOME")
            assertNull(suggest(f.drafts.get(received)!!).accountId)
            f.mailRules.save("RAPPICUENTA", null, rappi)
            assertEquals(rappi, suggest(f.drafts.get(received)!!).accountId)
            // Saving again replaces the provider's rule; removing it brings the digits back
            f.mailRules.save("RAPPICUENTA", null, savings)
            assertEquals(
                listOf(savings),
                f.mailRules
                    .observeAll()
                    .first()
                    .filter { it.source == "RAPPICUENTA" }
                    .map { it.accountId },
            )
            f.mailRules.remove("BANCOLOMBIA_TC")
            assertNull(suggest(f.drafts.get(card)!!).accountId)
        }

    @Test
    fun aTransferBetweenOwnAccountsIsOneDraftAndPaymentsToOthersAreNot() =
        runBlocking {
            val bancolombia = f.account("Bancolombia", 2_000_000)
            SetLastDigits(f.accounts, sync)(bancolombia, "2309")
            val rappi = f.account("RappiCuenta", 0)
            f.mailRules.save("RAPPICUENTA", null, rappi)
            val at = Instant.parse("2026-10-29T15:36:00Z")
            val out = draft("@rp1077", "2309", 1_000_000, at = at)
            draft("Bancolombia", null, 1_000_000, source = "RAPPICUENTA", type = "INCOME", at = at.plusSeconds(40))
            // Two payments of the same amount to other people, a minute apart, stay two expenses
            draft("Jorge", "2309", 50_000, at = at.plusSeconds(600))
            draft("Ana", "2309", 50_000, at = at.plusSeconds(630))

            val listed = f.drafts.observeAll().first()
            assertEquals(3, listed.size)
            val transfer = listed.single { it.type == TransactionType.TRANSFER }
            assertEquals(out, transfer.id)
            assertEquals("Bancolombia → RappiCuenta", "${transfer.sourceName} → ${transfer.destinationSource?.let(MailProviders::name)}")

            val before = balance(bancolombia)
            assertTrue(apply(out))
            assertEquals(before - Money.ofPesos(1_000_000), balance(bancolombia))
            assertEquals(Money.ofPesos(1_000_000), balance(rappi))
            assertEquals(2, f.drafts.count())
        }

    @Test
    fun anUnknownSourceShowsItsNameInsteadOfFailing() =
        runBlocking {
            val id = draft("Tienda", null, source = "NUEVO_BANCO")
            assertEquals("Nuevo banco", f.drafts.get(id)!!.sourceName)
            assertEquals("Uber", MailProviders.name("UBER"))
        }
}
