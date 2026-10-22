package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountFlows
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Loan
import com.dev.julian09.financeapp.domain.model.LoanDue
import com.dev.julian09.financeapp.domain.model.LoanMode
import com.dev.julian09.financeapp.domain.model.LoanOverview
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RecurringSummary
import com.dev.julian09.financeapp.domain.model.Urgency
import com.dev.julian09.financeapp.domain.model.withDues
import com.dev.julian09.financeapp.domain.usecase.CreateCategory
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.UUID

/** S13-QA02 and QA03: the month's commitments with cards and loans (#131) and unique category names. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CommitmentsAndNamesTest {
    private val f = LedgerFixture()

    @After
    fun close() = f.db.close()

    private fun loanDue(
        pesos: Long,
        due: LocalDate,
        urgency: Urgency = Urgency.UPCOMING,
    ): LoanDue {
        val account = Account(UUID.randomUUID(), "Libre inversión", AccountType.LOAN, Money.ZERO, true, true)
        val overview = LoanOverview(Loan(account.id, LoanMode.SIMPLE, 5), AccountBalance(account, AccountFlows()))
        return LoanDue(overview, Money.ofPesos(pesos), due, 0, urgency, inWindow = true)
    }

    @Test
    fun theMonthsCommitmentsAddTheInstallmentsDueUpToItsEnd() {
        val summary = RecurringSummary(committed = Money.ofPesos(938_000), pendingCount = 2, remaining = Money.ofPesos(938_000))
        val end = LocalDate.of(2026, 10, 31)
        val withDues =
            summary.withDues(
                listOf(
                    loanDue(281_958, LocalDate.of(2026, 10, 5), Urgency.OVERDUE),
                    // Next month: not this month's commitment
                    loanDue(278_833, LocalDate.of(2026, 11, 5)),
                    loanDue(0, LocalDate.of(2026, 10, 20)),
                ),
                end,
            )
        assertEquals(Money.ofPesos(1_219_958), withDues.committed)
        assertEquals(3, withDues.pendingCount)
        assertEquals(Money.ofPesos(1_219_958), withDues.remaining)
        assertEquals(1, withDues.overdueCount)
    }

    @Test
    fun aCategoryNameIsUniqueAcrossTheWholeTree() =
        runBlocking {
            val transport = f.category("Transporte")
            f.category("Taller", parent = transport)
            val home = f.category("Hogar")
            val create = CreateCategory(f.categories, FakeSyncScheduler())
            // Under another parent the name is still taken: categories are flat outside Categorías and Reportes
            assertEquals(
                LedgerError.NAME_TAKEN,
                create(Category(UUID.randomUUID(), "taller", CategoryKind.EXPENSE, parentId = home)),
            )
            assertNull(create(Category(UUID.randomUUID(), "Reparaciones", CategoryKind.EXPENSE, parentId = home)))
        }
}
