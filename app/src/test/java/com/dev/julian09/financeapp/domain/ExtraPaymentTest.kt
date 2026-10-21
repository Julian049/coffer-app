package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.FixedClock
import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.domain.model.Amortization
import com.dev.julian09.financeapp.domain.model.ExtraPaymentMode
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.LoanMode
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.usecase.CreateLoan
import com.dev.julian09.financeapp.domain.usecase.ExtraPayment
import com.dev.julian09.financeapp.domain.usecase.LoanDraft
import com.dev.julian09.financeapp.domain.usecase.PayLoanInstallment
import com.dev.julian09.financeapp.domain.usecase.TransactionField
import com.dev.julian09.financeapp.domain.usecase.TransactionValidator
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** S10-QA02: paying installments and extra payments (REQ-PRE-04 to 06, AT-HU14-2). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExtraPaymentTest {
    private val now = Instant.parse("2026-11-05T15:00:00Z")
    private val f = LedgerFixture(FixedClock(now))
    private val scheduler = FakeSyncScheduler()
    private val validator = TransactionValidator(f.accounts, f.categories)
    private val pay = PayLoanInstallment(f.observeLoans, f.loans, validator, scheduler, f.clock)
    private val extra = ExtraPayment(f.observeLoans, f.loans, validator, scheduler)

    @After
    fun close() = f.db.close()

    private suspend fun loan(amortization: Amortization): java.util.UUID {
        CreateLoan(f.accounts, f.loans, scheduler, f.clock)(
            LoanDraft(
                name = "Libre inversión",
                mode = LoanMode.DETAILED,
                amount = Money.ofPesos(5_000_000),
                paymentDay = 5,
                inProgress = true,
                count = 24,
                rate = BigDecimal("0.015"),
                rateType = RateType.MV,
                amortization = amortization,
                firstDueDate = LocalDate.of(2026, 11, 5),
                insurance = Money.ofPesos(8_000),
            ),
        )
        return f
            .observeLoans()
            .first()
            .single()
            .loan.accountId
    }

    @Test
    fun payingAnInstallmentLowersTheDebtByItsPrincipalOnly() =
        runBlocking {
            val bank = f.account("Bancolombia", 2_000_000)
            val id = loan(Amortization.FIXED_PRINCIPAL)
            assertTrue(pay(id, bank, Money.ZERO, now).isEmpty())
            val loan = f.observeLoans().first().single()
            assertEquals(Money.ofPesos(4_791_667), loan.debt)
            assertEquals(1, loan.paid)
            assertEquals(2, loan.next!!.number)
            val payment =
                f.transactions
                    .observeRecent(1)
                    .first()
                    .single()
            assertEquals(Money.ofPesos(291_333), payment.amount)
            assertEquals(Money.ofPesos(75_000), payment.interest)
            assertEquals(Money.ofPesos(8_000), payment.fees)
            assertEquals("LOAN_INSTALL", payment.paymentKind)
            // The next pending payment is the second installment
            assertEquals(
                LocalDate.of(2026, 12, 5),
                f
                    .loanDues()
                    .first()
                    .single()
                    .dueDate,
            )
        }

    @Test
    fun anExtraPaymentAsksWhatToReduceAndRecalculatesThePlan() =
        runBlocking {
            val bank = f.account("Bancolombia", 2_000_000)
            val id = loan(Amortization.FIXED_PRINCIPAL)
            val before = f.observeLoans().first().single()
            val shorter = ExtraPayment.simulate(before, Money.ofPesos(1_000_000), ExtraPaymentMode.REDUCE_TERM)
            val lower = ExtraPayment.simulate(before, Money.ofPesos(1_000_000), ExtraPaymentMode.REDUCE_PAYMENT)
            assertTrue(shorter.size < 24)
            assertEquals(24, lower.size)
            assertTrue(lower.first().total < before.next!!.total)

            assertTrue(extra(id, bank, Money.ofPesos(1_000_000), ExtraPaymentMode.REDUCE_TERM, now).isEmpty())
            val after = f.observeLoans().first().single()
            assertEquals(Money.ofPesos(4_000_000), after.debt)
            assertEquals(shorter.size, after.scheduled.size)
            assertEquals(24, after.installments.count { it.status == InstallmentStatus.CANCELLED })
            assertEquals(
                LedgerError.OVER_DEBT,
                extra(id, bank, Money.ofPesos(9_000_000), ExtraPaymentMode.REDUCE_TERM, now)[TransactionField.AMOUNT],
            )
        }
}
