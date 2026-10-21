package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.FixedClock
import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.model.Amortization
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.LoanMode
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.usecase.ConvertLoan
import com.dev.julian09.financeapp.domain.usecase.CreateLoan
import com.dev.julian09.financeapp.domain.usecase.LoanDraft
import com.dev.julian09.financeapp.domain.usecase.LoanField
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** S10-QA01: CreateLoan (REQ-PRE-01, 02, 08, 09; AT-HU14-1). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CreateLoanTest {
    private val f = LedgerFixture(FixedClock(Instant.parse("2026-10-16T12:00:00Z")))
    private val scheduler = FakeSyncScheduler()
    private val create = CreateLoan(f.accounts, f.loans, scheduler, f.clock)

    @After
    fun close() = f.db.close()

    private fun detailed(destination: java.util.UUID?) =
        LoanDraft(
            name = "Libre inversión",
            mode = LoanMode.DETAILED,
            amount = Money.ofPesos(5_000_000),
            paymentDay = 5,
            destinationId = destination,
            count = 24,
            rate = BigDecimal("0.015"),
            rateType = RateType.MV,
            amortization = Amortization.FIXED_PRINCIPAL,
            firstDueDate = LocalDate.of(2026, 11, 5),
            insurance = Money.ofPesos(8_000),
        )

    @Test
    fun aNewLoanDisbursesByTransferAndEachInstallmentIsPending() =
        runBlocking {
            val bank = f.account("Bancolombia", 1_000_000)
            assertEquals(emptyMap<LoanField, LedgerError>(), create(detailed(bank)))
            val loan = f.observeLoans().first().single()
            assertEquals(Money.ofPesos(5_000_000), loan.debt)
            assertEquals(24, loan.scheduled.size)
            assertEquals(Money.ofPesos(291_333), loan.next!!.total)
            // The disbursement is a transfer: the money arrives, but it is not income
            val balances = f.accounts.observeBalances().first()
            assertEquals(Money.ofPesos(6_000_000), balances.first { it.account.id == bank }.balance)
            assertEquals(
                Money.ZERO,
                f.transactions
                    .observeTotals(Instant.EPOCH, Instant.parse("2030-01-01T00:00:00Z"))
                    .first()
                    .income,
            )
            // AT-HU14-1: the first installment is a pending payment
            val due = f.loanDues().first().single()
            assertEquals(LocalDate.of(2026, 11, 5), due.dueDate)
            assertEquals(Money.ofPesos(291_333), due.amount)
        }

    @Test
    fun aLoanInProgressStartsFromTodaysBalanceWithoutADisbursement() =
        runBlocking {
            val draft =
                detailed(
                    null,
                ).copy(inProgress = true, amount = Money.ofPesos(4_375_000), count = 21, amortization = Amortization.FIXED_PAYMENT)
            assertEquals(emptyMap<LoanField, LedgerError>(), create(draft))
            val loan = f.observeLoans().first().single()
            assertEquals(Money.ofPesos(4_375_000), loan.debt)
            assertNull(loan.loan.disbursementAccountId)
            assertEquals(Amortization.FIXED_PAYMENT, loan.loan.amortization)
            assertEquals(Money.ofPesos(4_375_000), loan.scheduled.fold(Money.ZERO) { s, i -> s + i.principal })
            assertTrue(f.transactions.observeCount().first() == 0)
        }

    @Test
    fun aNewDetailedLoanNeedsItsDestinationAndASimpleOneItsPayment() =
        runBlocking {
            assertEquals(LedgerError.ACCOUNT_REQUIRED, create(detailed(null))[LoanField.DESTINATION])
            val simple = LoanDraft("Vehículo", LoanMode.SIMPLE, Money.ofPesos(4_375_000), paymentDay = 5)
            assertEquals(LedgerError.AMOUNT_REQUIRED, create(simple)[LoanField.MONTHLY])
            assertEquals(emptyMap<LoanField, LedgerError>(), create(simple.copy(monthlyPayment = Money.ofPesos(282_000))))
            val loan = f.observeLoans().first().single()
            assertEquals(Money.ofPesos(4_375_000), loan.debt)
            assertEquals(
                Money.ofPesos(282_000),
                f
                    .loanDues()
                    .first()
                    .single()
                    .amount,
            )

            // REQ-PRE-09: to detailed from the current debt
            assertEquals(
                emptyMap<LoanField, LedgerError>(),
                ConvertLoan(f.observeLoans, f.loans, scheduler)(loan.loan.accountId, detailed(null).copy(count = 16)),
            )
            val converted = f.observeLoans().first().single()
            assertEquals(LoanMode.DETAILED, converted.loan.mode)
            assertEquals(Money.ofPesos(4_375_000), converted.scheduled.fold(Money.ZERO) { s, i -> s + i.principal })
        }
}
