package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.SplitError
import com.dev.julian09.financeapp.domain.usecase.SplitPart
import com.dev.julian09.financeapp.domain.usecase.SplitTransaction
import com.dev.julian09.financeapp.domain.usecase.TransactionValidator
import com.dev.julian09.financeapp.domain.usecase.halves
import com.dev.julian09.financeapp.ui.FakeDraftPrefs
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
import java.time.Instant
import java.util.UUID

/** roadmap-v2.4 2.4-11 / CAMBIOS-UI-2.4 §6: what is split keeps only the user's part as spending. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SplitTransactionTest {
    private val f = LedgerFixture()
    private val split =
        SplitTransaction(f.transactions, TransactionValidator(f.accounts, f.categories), FakeDraftPrefs(), f.clock, FakeSyncScheduler())

    @After
    fun close() = f.db.close()

    private fun pesos(p: Long) = Money.ofPesos(p)

    private fun expense(
        account: UUID,
        category: UUID,
        total: Long = 60_000,
    ) = Transaction(
        UUID.randomUUID(),
        TransactionType.EXPENSE,
        pesos(total),
        Instant.parse("2026-10-26T20:00:00Z"),
        account,
        categoryId = category,
        note = "Compra ÉXITO Calle 80",
    )

    @Test
    fun halvesAlwaysSumToTheTotal() {
        val (own, other) = halves(Money(10_0001))
        assertEquals(Money(10_0001), own + other)
        assertEquals(pesos(30_000), halves(pesos(60_000)).first)
    }

    @Test
    fun halfOfSixtyThousandCountsThirtyThousandAndMovesTheRestBetweenAccounts() =
        runBlocking {
            val card = f.account("Tarjeta", 100_000)
            val savings = f.account("Ahorros", 0)
            val food = f.category("Mercado")
            val original = expense(card, food)
            f.transactions.insert(original)

            assertNull(split(original, listOf(SplitPart(pesos(30_000)), SplitPart(pesos(30_000), savings))))

            val all = f.transactions.observeRecent(10).first()
            assertEquals(2, all.size)
            assertEquals(listOf(TransactionType.EXPENSE), all.filter { it.categoryId != null }.map { it.type })
            assertEquals(pesos(30_000), all.first { it.type == TransactionType.EXPENSE }.amount)
            val transfer = all.first { it.type == TransactionType.TRANSFER }
            assertEquals(savings, transfer.destinationAccountId)
            assertEquals("Compra ÉXITO Calle 80", transfer.note)
            assertEquals(1, all.map { it.splitGroupId }.distinct().size)
            // The paying account goes down by the whole 60.000; the destination goes up by what came back
            val balances =
                f.accounts
                    .observeBalances()
                    .first()
                    .associate { it.account.id to it.balance }
            assertEquals(pesos(100_000 - 60_000), balances[card])
            assertEquals(pesos(30_000), balances[savings])
        }

    @Test
    fun aPartBackToTheSameAccountOnlyLowersTheExpense() =
        runBlocking {
            val card = f.account("Tarjeta", 0)
            val original = expense(card, f.category("Mercado"))
            f.transactions.insert(original)
            assertNull(split(original, listOf(SplitPart(pesos(20_000)), SplitPart(pesos(40_000), card))))
            assertEquals(
                listOf(TransactionType.EXPENSE),
                f.transactions
                    .observeRecent(10)
                    .first()
                    .map { it.type },
            )
        }

    @Test
    fun threeAndFivePartsMakeThatManyMovements() =
        runBlocking {
            val card = f.account("Tarjeta", 0)
            val other = f.account("Otra", 0)
            val original = expense(card, f.category("Mercado"), 50_000)
            f.transactions.insert(original)
            val parts = listOf(SplitPart(pesos(10_000))) + List(4) { SplitPart(pesos(10_000), other) }
            assertNull(split(original, parts))
            assertEquals(
                5,
                f.transactions
                    .observeRecent(10)
                    .first()
                    .size,
            )
        }

    @Test
    fun partsThatDoNotSumToTheTotalAreRefused() {
        val card = UUID.randomUUID()
        val original = expense(card, UUID.randomUUID())
        assertEquals(
            SplitError.SUM_MISMATCH,
            com.dev.julian09.financeapp.domain.usecase.splitError(
                original,
                listOf(SplitPart(pesos(10_000)), SplitPart(pesos(10_000), card)),
            ),
        )
        assertEquals(
            SplitError.ACCOUNT_REQUIRED,
            com.dev.julian09.financeapp.domain.usecase
                .splitError(original, listOf(SplitPart(pesos(30_000)), SplitPart(pesos(30_000)))),
        )
    }
}
