package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.data.local.FixedClock
import com.dev.julian09.financeapp.data.local.LedgerFixture
import com.dev.julian09.financeapp.data.local.entity.SyncMetaEntity
import com.dev.julian09.financeapp.data.sync.SyncMeta
import com.dev.julian09.financeapp.domain.model.Amortization
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.LoanMode
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.CreateLoan
import com.dev.julian09.financeapp.domain.usecase.EditLoan
import com.dev.julian09.financeapp.domain.usecase.LoanDraft
import com.dev.julian09.financeapp.domain.usecase.LoanEdit
import com.dev.julian09.financeapp.domain.usecase.LoanField
import com.dev.julian09.financeapp.domain.usecase.OPENING_BALANCE
import com.dev.julian09.financeapp.domain.usecase.balance
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** S12-QA02 and QA03: the opening balance as a movement, and Editar préstamo. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OpeningBalanceAndLoanEditTest {
    private val now = Instant.parse("2026-11-01T15:00:00Z")
    private val f = LedgerFixture(FixedClock(now))
    private val scheduler = FakeSyncScheduler()
    private val owner = UUID.fromString("00000000-0000-0000-0000-0000000000aa")

    @After
    fun close() = f.db.close()

    private suspend fun owned() = f.db.syncMeta().put(SyncMetaEntity(SyncMeta.OWNER, owner.toString()))

    @Test
    fun anOpeningBalanceBecomesAnExcludedIncomeAndTheBalanceStaysTheSame() =
        runBlocking {
            owned()
            val bank = f.account("Bancolombia", 850_000)
            val card = f.account("Visa", 800_000, type = "CREDIT_CARD")
            val before =
                f.accounts
                    .observeBalances()
                    .first()
                    .associate { it.account.id to it.balance }

            assertTrue(f.accounts.moveOpeningBalances())

            val after =
                f.accounts
                    .observeBalances()
                    .first()
                    .associateBy { it.account.id }
            assertEquals(before[bank], after[bank]!!.balance)
            assertEquals(Money.ZERO, after[bank]!!.account.initialBalance)
            // A card's opening balance is debt: it stays a field
            assertEquals(Money.ofPesos(800_000), after[card]!!.account.initialBalance)
            val opening =
                f.transactions
                    .observeByAccount(bank)
                    .first()
                    .single()
            assertEquals(TransactionType.INCOME, opening.type)
            assertEquals(Money.ofPesos(850_000), opening.amount)
            assertEquals("Saldo inicial", opening.note)
            val category =
                f.categories
                    .observeAll()
                    .first()
                    .single { it.id == opening.categoryId }
            assertEquals(OPENING_BALANCE, category.systemKey)
            assertTrue(category.excludeFromReports)
            // Ids come from the user and the account: a second phone writes the same rows
            assertEquals(UUID.nameUUIDFromBytes("$owner:$OPENING_BALANCE".toByteArray()), category.id)
            assertEquals(UUID.nameUUIDFromBytes("$OPENING_BALANCE:$bank".toByteArray()), opening.id)
            // Nothing left to move
            assertFalse(f.accounts.moveOpeningBalances())
        }

    @Test
    fun withoutAnOwnerNothingMoves() =
        runBlocking {
            f.account("Billetera", 150_000, type = "CASH")
            assertFalse(f.accounts.moveOpeningBalances())
        }

    private suspend fun loan(): UUID {
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
                amortization = Amortization.FIXED_PRINCIPAL,
                firstDueDate = LocalDate.of(2026, 11, 5),
            ),
        )
        return f
            .observeLoans()
            .first()
            .single()
            .loan.accountId
    }

    @Test
    fun editingALoanRecalculatesTheInstallmentsLeftOnTheNewDay() =
        runBlocking {
            val id = loan()
            val edit = EditLoan(f.observeLoans, f.accounts, f.loans, scheduler, f.clock)
            val errors =
                edit(
                    id,
                    LoanEdit("Crédito libre", BigDecimal("0.01"), RateType.MV, paymentDay = 10, Money.ofPesos(8_000), Money.ZERO),
                )
            assertTrue(errors.isEmpty())
            val loan = f.observeLoans().first().single()
            assertEquals("Crédito libre", loan.account.account.name)
            assertEquals(24, loan.scheduled.size)
            val next = loan.next!!
            assertEquals(LocalDate.of(2026, 11, 10), next.dueDate)
            // Same principal left, interest at the new 1 % M.V. and the insurance in every installment
            assertEquals(Money.ofPesos(5_000_000), loan.balance)
            assertEquals(Money.ofPesos(50_000), next.interest)
            assertEquals(Money.ofPesos(8_000), next.insurance)
            assertEquals(24, loan.installments.count { it.status == InstallmentStatus.CANCELLED })
        }

    @Test
    fun aLoanEditNeedsANameAndAValidDay() =
        runBlocking {
            val id = loan()
            val errors =
                EditLoan(f.observeLoans, f.accounts, f.loans, scheduler, f.clock)(
                    id,
                    LoanEdit(" ", BigDecimal("0.015"), RateType.MV, paymentDay = 40, Money.ZERO, Money.ZERO),
                )
            assertEquals(LedgerError.NAME_REQUIRED, errors[LoanField.NAME])
            assertEquals(LedgerError.DAY_INVALID, errors[LoanField.PAYMENT_DAY])
        }

    @Test
    fun theNextInstallmentNeverStartsOverdueAfterChangingTheDay() =
        runBlocking {
            val id = loan()
            f.clock.instant = java.time.Instant.parse("2026-11-20T12:00:00Z")
            EditLoan(f.observeLoans, f.accounts, f.loans, scheduler, f.clock)(
                id,
                LoanEdit("Libre inversión", BigDecimal("0.015"), RateType.MV, paymentDay = 10, Money.ZERO, Money.ZERO),
            )
            // The 10th of November has passed on the 20th: the next one is December's
            assertEquals(
                LocalDate.of(2026, 12, 10),
                f
                    .observeLoans()
                    .first()
                    .single()
                    .next!!
                    .dueDate,
            )
        }

    @Test
    fun changingThePaymentDateMovesThePaymentAndTheInstallmentItSettled() =
        runBlocking {
            val id = loan()
            val cash = f.account("Efectivo", 10_000_000)
            val validator =
                com.dev.julian09.financeapp.domain.usecase
                    .TransactionValidator(f.accounts, f.categories)
            val paidOn = java.time.Instant.parse("2026-11-05T15:00:00Z")
            com.dev.julian09.financeapp.domain.usecase
                .PayLoanInstallment(f.observeLoans, f.loans, validator, scheduler, f.clock)(
                id,
                cash,
                Money.ZERO,
                paidOn,
            )
            val payment =
                f.db
                    .transactions()
                    .observeByAccount(id)
                    .first()
                    .single { it.paymentKind != null }
            assertEquals(
                paidOn,
                f.db
                    .cards()
                    .getInstallment(payment.installmentId!!)!!
                    .paidAt,
            )

            val newDate = java.time.Instant.parse("2026-11-03T15:00:00Z")
            assertTrue(
                com.dev.julian09.financeapp.domain.usecase
                    .ChangePaymentDate(f.transactions, scheduler)(payment.id, newDate),
            )
            assertEquals(
                newDate,
                f.db
                    .transactions()
                    .get(payment.id)!!
                    .occurredAt,
            )
            assertEquals(
                newDate,
                f.db
                    .cards()
                    .getInstallment(payment.installmentId!!)!!
                    .paidAt,
            )
            // The amount stays, and an ordinary movement is not a payment
            assertEquals(
                payment.amount,
                f.db
                    .transactions()
                    .get(payment.id)!!
                    .amount,
            )
            val other = f.tx("EXPENSE", 5, cash)
            assertFalse(
                com.dev.julian09.financeapp.domain.usecase
                    .ChangePaymentDate(f.transactions, scheduler)(other, newDate),
            )
        }
}
