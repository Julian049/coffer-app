package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.finance.DraftLevel
import com.dev.julian09.financeapp.domain.finance.learn
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionDraft
import com.dev.julian09.financeapp.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.util.UUID

/** roadmap-v2.4 §3: unknown, probable and sure, from the movements the user made out of drafts. */
class DraftSuggesterTest {
    private val account = UUID.randomUUID()
    private val food = UUID.randomUUID()
    private val leisure = UUID.randomUUID()

    private fun draft(
        merchant: String? = "Éxito Calle 80",
        amount: Long = 50_000,
        type: TransactionType = TransactionType.EXPENSE,
    ) = TransactionDraft(UUID.randomUUID(), "BANCOLOMBIA", type, Money.of(amount.toBigDecimal()), Instant.now(), merchant, "1234")

    private fun past(
        note: String? = "Mercado",
        category: UUID = food,
        amount: Long = 50_000,
        merchant: String? = "EXITO CALLE 80",
        type: TransactionType = TransactionType.EXPENSE,
    ) = Transaction(
        UUID.randomUUID(),
        type,
        Money.of(amount.toBigDecimal()),
        Instant.now(),
        account,
        categoryId = category,
        note = note,
        sourceMerchant = merchant,
        draftId = UUID.randomUUID(),
    )

    @Test
    fun withoutHistoryItStaysUnknown() {
        assertEquals(DraftLevel.UNKNOWN, learn(draft(), emptyList()).level)
        assertEquals(DraftLevel.UNKNOWN, learn(draft(), listOf(past(merchant = "Rappi"))).level)
    }

    @Test
    fun aTransferIsNeverLearned() {
        assertEquals(DraftLevel.UNKNOWN, learn(draft(type = TransactionType.TRANSFER), List(3) { past() }).level)
    }

    @Test
    fun threeEqualResultsAtTheSameAmountAreSure() {
        val learned = learn(draft(), List(3) { past() })
        assertEquals(DraftLevel.SURE, learned.level)
        assertEquals("Mercado", learned.note)
        assertEquals(food, learned.categoryId)
    }

    @Test
    fun twoEqualResultsAreOnlyProbable() {
        assertEquals(DraftLevel.PROBABLE, learn(draft(), List(2) { past() }).level)
    }

    @Test
    fun anotherAmountIsProbableAndTakesTheLatestTitleNotTheEmails() {
        val learned = learn(draft(amount = 72_000), List(3) { past() })
        assertEquals(DraftLevel.PROBABLE, learned.level)
        assertEquals("Mercado", learned.note)
    }

    @Test
    fun mixedResultsAreProbableAndTheMostRecentWins() {
        val learned = learn(draft(), listOf(past("Cine", leisure), past(), past()))
        assertEquals(DraftLevel.PROBABLE, learned.level)
        assertEquals("Cine", learned.note)
        assertEquals(leisure, learned.categoryId)
    }

    @Test
    fun theMerchantIgnoresCaseAccentsAndSpaces() {
        assertEquals(DraftLevel.SURE, learn(draft("  exito   CALLE 80 "), List(3) { past(merchant = "Éxito Calle 80") }).level)
    }

    @Test
    fun anIncomeIsKnownByAccountAndTypeAndKeepsAnEmptyTitle() {
        val learned =
            learn(
                draft(merchant = null, type = TransactionType.INCOME),
                List(3) { past(null, merchant = null, type = TransactionType.INCOME) },
            )
        assertEquals(DraftLevel.SURE, learned.level)
        assertNull(learned.note)
        assertEquals(food, learned.categoryId)
    }
}
