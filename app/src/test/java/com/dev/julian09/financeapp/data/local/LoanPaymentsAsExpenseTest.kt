package com.dev.julian09.financeapp.data.local

import com.dev.julian09.financeapp.data.local.entity.SettingsEntity
import com.dev.julian09.financeapp.data.local.entity.SyncMetaEntity
import com.dev.julian09.financeapp.data.sync.SyncMeta
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.TransactionFilter
import com.dev.julian09.financeapp.domain.usecase.SearchTransactions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

/** S15-QA06: the principal of loan payments is an expense of the loans' category (S15-DOM05, DAT03). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LoanPaymentsAsExpenseTest {
    private val f = LedgerFixture()
    private val search = SearchTransactions(f.transactions, f.categories)
    private val october = TransactionFilter(Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"))
    private val at = Instant.parse("2026-10-10T12:00:00Z")

    @After
    fun close() = f.db.close()

    private suspend fun settings(loanCategory: UUID?) =
        f.db.settings().upsert(
            SettingsEntity(
                id = UUID.randomUUID(),
                reminderLeadDays = 5,
                notificationsEnabled = true,
                homeBalanceView = "AVAILABLE",
                amountDecimals = 0,
                homeCards = "[]",
                createdAt = at,
                updatedAt = at,
                loanPaymentCategoryId = loanCategory,
            ),
        )

    private suspend fun payment(
        kind: String,
        amount: Long,
        principal: Long?,
        from: UUID,
        loan: UUID,
    ) {
        val id = f.tx("TRANSFER", amount, from, at = at, destination = loan)
        f.db.transactions().upsert(
            listOf(
                f.db
                    .transactions()
                    .get(id)!!
                    .copy(paymentKind = kind, principal = principal?.let { it * 10_000 }),
            ),
        )
    }

    @Test
    fun homeHistoryAndCategoriesCountThePrincipalAsAnExpenseOfDeudas() =
        runBlocking {
            val cash = f.account("Efectivo", 1_000_000)
            val loan = f.account("Préstamo", type = "LOAN")
            val debts = f.category("Deudas")
            settings(debts)
            f.tx("EXPENSE", 10_000, cash, at = at, category = f.category("Comida"))
            // An installment of 120 with 100 of principal; an extra payment; a simple loan's payment (no split)
            payment("LOAN_INSTALL", 120_000, 100_000, cash, loan)
            payment("LOAN_EXTRA", 50_000, 50_000, cash, loan)
            payment("LOAN_INSTALL", 30_000, null, cash, loan)

            val from = at.minusSeconds(86_400)
            val to = at.plusSeconds(86_400)
            assertEquals(
                Money.ofPesos(190_000).scaled,
                f.db
                    .transactions()
                    .observeTotals(from, to)
                    .first()
                    .expense,
            )
            assertEquals(Money.ofPesos(190_000), search(october, 100).first().totals.expense)
            // The filter by «Deudas» finds the three payments
            val filtered = search(october.copy(categoryIds = setOf(debts)), 100).first()
            assertEquals(3, filtered.transactions.size)
            assertEquals(Money.ofPesos(180_000), filtered.totals.expense)
            val byCategory =
                f.db
                    .transactions()
                    .observeCategoryTotals(from, to)
                    .first()
                    .single { it.categoryId == debts }
            assertEquals(Money.ofPesos(180_000).scaled, byCategory.total)
            assertEquals(3, byCategory.count)
        }

    @Test
    fun theLoansCategoryIsCreatedOnceAndOnlyWhenNoneIsSet() =
        runBlocking {
            f.db.syncMeta().put(SyncMetaEntity(SyncMeta.OWNER, "user-1"))
            // No settings row yet: nothing to do until the first download
            assertFalse(f.accounts.ensureLoanCategory())
            settings(null)
            assertTrue(f.accounts.ensureLoanCategory())
            val id =
                f.db
                    .settings()
                    .observe()
                    .first()!!
                    .loanPaymentCategoryId
            assertNotNull(id)
            assertEquals(
                "Deudas",
                f.db
                    .categories()
                    .get(id!!)!!
                    .name,
            )
            assertFalse(f.accounts.ensureLoanCategory())
            // Deleted by the user and cleared: it is not brought back
            f.db.settings().upsert(
                f.db
                    .settings()
                    .observe()
                    .first()!!
                    .copy(loanPaymentCategoryId = null),
            )
            assertFalse(f.accounts.ensureLoanCategory())
            assertNull(
                f.db
                    .settings()
                    .observe()
                    .first()!!
                    .loanPaymentCategoryId,
            )
        }
}
