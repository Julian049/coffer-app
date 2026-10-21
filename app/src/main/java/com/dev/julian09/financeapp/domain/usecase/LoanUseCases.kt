package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.finance.CardPeriodCalculator
import com.dev.julian09.financeapp.domain.finance.LoanScheduler
import com.dev.julian09.financeapp.domain.finance.PlannedLoanInstallment
import com.dev.julian09.financeapp.domain.finance.RateConverter
import com.dev.julian09.financeapp.domain.finance.RecurrenceCalculator
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.Amortization
import com.dev.julian09.financeapp.domain.model.ExtraPaymentMode
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Loan
import com.dev.julian09.financeapp.domain.model.LoanDue
import com.dev.julian09.financeapp.domain.model.LoanInstallment
import com.dev.julian09.financeapp.domain.model.LoanMode
import com.dev.julian09.financeapp.domain.model.LoanOverview
import com.dev.julian09.financeapp.domain.model.LoanPaymentKind
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.LoanRepository
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

private const val LOAN_NAME_MAX = 60
private const val LOAN_INSTALLMENTS_MAX = 600

enum class LoanField { NAME, AMOUNT, DESTINATION, COUNT, RATE, FIRST_DUE, PAYMENT_DAY, MONTHLY }

typealias LoanErrors = Map<LoanField, LedgerError>

/**
 * Nuevo préstamo (REQ-PRE-01, 02, 08). [amount] is the money received, or today's balance when [inProgress] (a loan
 * already being paid has no disbursement, SRS 1.2) and in a simple loan.
 */
data class LoanDraft(
    val name: String,
    val mode: LoanMode,
    val amount: Money,
    val paymentDay: Int,
    val inProgress: Boolean = false,
    val destinationId: UUID? = null,
    val count: Int = 0,
    val rate: BigDecimal? = null,
    val rateType: RateType = RateType.MV,
    val amortization: Amortization = Amortization.FIXED_PAYMENT,
    val firstDueDate: LocalDate? = null,
    val monthlyPayment: Money = Money.ZERO,
    val insurance: Money = Money.ZERO,
    val otherCosts: Money = Money.ZERO,
) {
    val detailed: Boolean get() = mode == LoanMode.DETAILED

    fun errors(checkName: Boolean = true): LoanErrors {
        val errors = mutableMapOf<LoanField, LedgerError>()
        if (checkName) {
            when {
                name.isBlank() -> errors[LoanField.NAME] = LedgerError.NAME_REQUIRED
                name.trim().length > LOAN_NAME_MAX -> errors[LoanField.NAME] = LedgerError.NAME_TOO_LONG
            }
        }
        if (amount <= Money.ZERO) errors[LoanField.AMOUNT] = LedgerError.AMOUNT_REQUIRED
        if (paymentDay !in 1..31) errors[LoanField.PAYMENT_DAY] = LedgerError.DAY_INVALID
        if (detailed) {
            if (!inProgress && destinationId == null) errors[LoanField.DESTINATION] = LedgerError.ACCOUNT_REQUIRED
            if (count !in 1..LOAN_INSTALLMENTS_MAX) errors[LoanField.COUNT] = LedgerError.INSTALLMENTS_INVALID
            if (rate == null || cleanRate(rate) == null) errors[LoanField.RATE] = LedgerError.RATE_INVALID
            if (firstDueDate == null) errors[LoanField.FIRST_DUE] = LedgerError.DAY_INVALID
        } else if (monthlyPayment <= Money.ZERO) {
            errors[LoanField.MONTHLY] = LedgerError.AMOUNT_REQUIRED
        }
        return errors
    }

    /** The plan the form shows and saves; empty until the detailed fields are valid. */
    fun plan(): List<PlannedLoanInstallment> {
        val clean = rate?.let(::cleanRate) ?: return emptyList()
        if (!detailed || amount <= Money.ZERO || count !in 1..LOAN_INSTALLMENTS_MAX || firstDueDate == null || paymentDay !in 1..31) {
            return emptyList()
        }
        return LoanScheduler.plan(
            amount,
            count,
            RateConverter.toMonthly(clean, rateType),
            firstDueDate,
            paymentDay,
            amortization,
            insurance,
            otherCosts,
        )
    }

    fun terms(accountId: UUID) =
        Loan(
            accountId = accountId,
            mode = mode,
            paymentDay = paymentDay,
            disbursementAccountId = destinationId.takeIf { detailed && !inProgress },
            principal = amount.takeIf { detailed },
            installmentsCount = count.takeIf { detailed },
            rate = rate?.let(::cleanRate)?.takeIf { detailed },
            rateType = rateType.takeIf { detailed },
            firstDueDate = firstDueDate.takeIf { detailed },
            monthlyPayment = monthlyPayment.takeIf { !detailed },
            lifeInsurance = if (detailed) insurance else Money.ZERO,
            otherCosts = if (detailed) otherCosts else Money.ZERO,
            amortization = amortization,
        )
}

/** The first payment day after [today] (the form's default first due date). */
fun nextPaymentDate(
    today: LocalDate,
    paymentDay: Int,
): LocalDate {
    val thisMonth = CardPeriodCalculator.dayIn(YearMonth.from(today), paymentDay)
    return if (thisMonth.isAfter(today)) thisMonth else CardPeriodCalculator.dayIn(YearMonth.from(today).plusMonths(1), paymentDay)
}

private fun List<PlannedLoanInstallment>.toInstallments(loanId: UUID) =
    map { LoanInstallment(UUID.randomUUID(), loanId, it.number, it.dueDate, it.principal, it.interest, it.insurance, it.otherCosts) }

/**
 * SEQ-11 (S10-DOM01): the LOAN account, its terms and plan; a new detailed loan also records the disbursement as a
 * transfer from the loan to the destination account, so it is never income (REQ-PRE-01).
 */
class CreateLoan
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val loans: LoanRepository,
        private val scheduler: SyncScheduler,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(draft: LoanDraft): LoanErrors {
            draft.errors().takeIf { it.isNotEmpty() }?.let { return it }
            val last = accounts.observeBalances().first().maxOfOrNull { it.account.sortOrder } ?: -1
            val disburses = draft.detailed && !draft.inProgress
            val account =
                Account(
                    id = UUID.randomUUID(),
                    name = draft.name.trim(),
                    type = AccountType.LOAN,
                    initialBalance = if (disburses) Money.ZERO else draft.amount,
                    sortOrder = last + 1,
                )
            val disbursement =
                if (disburses) {
                    Transaction(
                        id = UUID.randomUUID(),
                        type = TransactionType.TRANSFER,
                        amount = draft.amount,
                        occurredAt = clock.now(),
                        accountId = account.id,
                        destinationAccountId = draft.destinationId,
                        note = "Desembolso ${account.name}",
                    )
                } else {
                    null
                }
            loans.create(account, draft.terms(account.id), draft.plan().toInstallments(account.id), disbursement)
            scheduler.requestSync()
            return emptyMap()
        }
    }

/** REQ-PRE-09: a simple loan becomes detailed from its current debt, without a disbursement. */
class ConvertLoan
    @Inject
    constructor(
        private val observeLoans: ObserveLoans,
        private val loans: LoanRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(
            loanId: UUID,
            draft: LoanDraft,
        ): LoanErrors {
            val overview = observeLoans().first().firstOrNull { it.loan.accountId == loanId } ?: return emptyMap()
            val detailed = draft.copy(mode = LoanMode.DETAILED, inProgress = true, amount = overview.debt)
            detailed.errors(checkName = false).takeIf { it.isNotEmpty() }?.let { return it }
            loans.update(detailed.terms(loanId), detailed.plan().toInstallments(loanId))
            scheduler.requestSync()
            return emptyMap()
        }
    }

/** Every loan with its debt and plan. */
class ObserveLoans
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val loans: LoanRepository,
    ) {
        operator fun invoke(): Flow<List<LoanOverview>> =
            combine(accounts.observeBalances(), loans.observeLoans(), loans.observeInstallments()) { balances, terms, installments ->
                terms.mapNotNull { loan ->
                    val account = balances.firstOrNull { it.account.id == loan.accountId } ?: return@mapNotNull null
                    LoanOverview(loan, account, installments.filter { it.loanId == loan.accountId })
                }
            }
    }

/**
 * REQ-PRE-04: the next installment of each active loan with debt, with the window and urgency of the recurring
 * expenses. A simple loan is due on its payment day, or the next month's once a payment was made this month.
 */
class ObserveLoanDues
    @Inject
    constructor(
        private val observeLoans: ObserveLoans,
        private val loans: LoanRepository,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) {
        operator fun invoke(): Flow<List<LoanDue>> {
            val today = clock.today()
            val month = YearMonth.from(today)
            val monthStart = month.atDay(1).atStartOfDay(clock.zone()).toInstant()
            return combine(observeLoans(), loans.observePaymentsSince(monthStart), settings.observe()) { all, payments, s ->
                all
                    .filter { it.account.account.active && it.debt > Money.ZERO }
                    .mapNotNull { o ->
                        val (amount, due) =
                            when (o.loan.mode) {
                                LoanMode.DETAILED -> o.next?.let { it.total to it.dueDate } ?: return@mapNotNull null
                                LoanMode.SIMPLE -> {
                                    val paid = payments.any { it.destinationAccountId == o.loan.accountId }
                                    val target = if (paid) month.plusMonths(1) else month
                                    (o.loan.monthlyPayment ?: Money.ZERO).let { if (it > o.debt) o.debt else it } to
                                        CardPeriodCalculator.dayIn(target, o.loan.paymentDay)
                                }
                            }
                        LoanDue(
                            o,
                            amount,
                            due,
                            daysLeft = ChronoUnit.DAYS.between(today, due),
                            urgency = RecurrenceCalculator.urgency(due, today),
                            inWindow = RecurrenceCalculator.inWindow(due, today, s.reminderLeadDays),
                        )
                    }.sortedBy { it.dueDate }
            }
        }
    }

/**
 * REQ-PRE-05 (SEQ-11): a transfer to the loan. Detailed: the next installment, split into principal (lowers the debt)
 * and interest, insurance and costs (to "Intereses y cargos financieros"). Simple: [amount] lowers the debt whole.
 */
class PayLoanInstallment
    @Inject
    constructor(
        private val observeLoans: ObserveLoans,
        private val loans: LoanRepository,
        private val validator: TransactionValidator,
        private val scheduler: SyncScheduler,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            loanId: UUID,
            sourceAccountId: UUID?,
            amount: Money,
            occurredAt: Instant,
        ): TransactionErrors {
            val overview = observeLoans().first().firstOrNull { it.loan.accountId == loanId } ?: return emptyMap()
            val name = overview.account.account.name
            val next = overview.next.takeIf { overview.loan.mode == LoanMode.DETAILED }
            val payment =
                Transaction(
                    id = UUID.randomUUID(),
                    type = TransactionType.TRANSFER,
                    amount = next?.total ?: amount,
                    occurredAt = occurredAt,
                    accountId = sourceAccountId ?: loanId,
                    destinationAccountId = loanId,
                    note = next?.let { "Cuota ${it.number} $name" } ?: "Pago $name",
                    principal = next?.principal,
                    interest = next?.interest,
                    fees = next?.charges,
                    installmentId = next?.id,
                    paymentKind = LoanPaymentKind.INSTALLMENT.code,
                )
            validator.check(payment).takeIf { it.isNotEmpty() }?.let { return it }
            loans.pay(payment, listOfNotNull(next?.copy(status = InstallmentStatus.PAID, paidAt = clock.now())))
            scheduler.requestSync()
            return emptyMap()
        }
    }

/** REQ-PRE-06 (AT-HU14-2): an extra payment to principal and the plan recalculated. */
class ExtraPayment
    @Inject
    constructor(
        private val observeLoans: ObserveLoans,
        private val loans: LoanRepository,
        private val validator: TransactionValidator,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(
            loanId: UUID,
            sourceAccountId: UUID?,
            amount: Money,
            mode: ExtraPaymentMode,
            occurredAt: Instant,
        ): TransactionErrors {
            val overview = observeLoans().first().firstOrNull { it.loan.accountId == loanId } ?: return emptyMap()
            if (amount > overview.balance) return mapOf(TransactionField.AMOUNT to LedgerError.OVER_DEBT)
            val payment =
                Transaction(
                    id = UUID.randomUUID(),
                    type = TransactionType.TRANSFER,
                    amount = amount,
                    occurredAt = occurredAt,
                    accountId = sourceAccountId ?: loanId,
                    destinationAccountId = loanId,
                    note = "Abono a capital ${overview.account.account.name}",
                    principal = amount,
                    interest = Money.ZERO,
                    fees = Money.ZERO,
                    paymentKind = LoanPaymentKind.EXTRA.code,
                )
            validator.check(payment).takeIf { it.isNotEmpty() }?.let { return it }
            val plan = simulate(overview, amount, mode).toInstallments(loanId)
            loans.pay(payment, overview.scheduled.map { it.copy(status = InstallmentStatus.CANCELLED) } + plan)
            scheduler.requestSync()
            return emptyMap()
        }

        companion object {
            /** The plan the sheet shows for each option before the payment is registered. */
            fun simulate(
                overview: LoanOverview,
                amount: Money,
                mode: ExtraPaymentMode,
            ): List<PlannedLoanInstallment> {
                val loan = overview.loan
                val first = overview.next ?: return emptyList()
                val rate = loan.rate ?: return emptyList()
                return LoanScheduler.afterExtraPayment(
                    balance = overview.balance,
                    extra = amount,
                    remaining = overview.scheduled.size,
                    currentPrincipal = first.principal,
                    currentPayment = first.principal + first.interest,
                    monthlyRate = RateConverter.toMonthly(rate, loan.rateType ?: RateType.MV),
                    nextDueDate = first.dueDate,
                    paymentDay = loan.paymentDay,
                    amortization = loan.amortization,
                    mode = mode,
                    insurance = loan.lifeInsurance,
                    otherCosts = loan.otherCosts,
                    firstNumber = first.number,
                )
            }
        }
    }

/** The principal still to pay in the plan (the debt of a detailed loan). */
val LoanOverview.balance: Money get() = scheduled.fold(Money.ZERO) { s, i -> s + i.principal }
