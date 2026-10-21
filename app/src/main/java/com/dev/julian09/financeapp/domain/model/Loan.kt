package com.dev.julian09.financeapp.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class LoanMode { DETAILED, SIMPLE }

/** REQ-PRE-03 (SRS 1.2): fixed payment (French) or fixed principal, chosen per loan (PO, 2026-10-20). */
enum class Amortization { FIXED_PAYMENT, FIXED_PRINCIPAL }

/**
 * Terms of a LOAN account (REQ-PRE-01 to 03, 08); [accountId] is also its id. A detailed loan has a plan; a loan in
 * progress has no [disbursementAccountId] and its [principal] is the balance when it was registered.
 */
data class Loan(
    val accountId: UUID,
    val mode: LoanMode,
    val paymentDay: Int,
    val disbursementAccountId: UUID? = null,
    val principal: Money? = null,
    val installmentsCount: Int? = null,
    /** A fraction: 0.015 = 1,5 %. */
    val rate: BigDecimal? = null,
    val rateType: RateType? = null,
    val firstDueDate: LocalDate? = null,
    val monthlyPayment: Money? = null,
    val lifeInsurance: Money = Money.ZERO,
    val otherCosts: Money = Money.ZERO,
    val amortization: Amortization = Amortization.FIXED_PRINCIPAL,
)

/** One installment of a loan's plan (installments.loan_account_id). */
data class LoanInstallment(
    val id: UUID,
    val loanId: UUID,
    val number: Int,
    val dueDate: LocalDate,
    val principal: Money,
    val interest: Money,
    val insurance: Money = Money.ZERO,
    val otherCosts: Money = Money.ZERO,
    val status: InstallmentStatus = InstallmentStatus.SCHEDULED,
    val paidAt: Instant? = null,
) {
    val total: Money get() = principal + interest + insurance + otherCosts

    /** What goes to "Intereses y cargos financieros" when it is paid (REQ-PRE-05). */
    val charges: Money get() = insurance + otherCosts
}

/** A loan with its debt and plan, as Detalle de préstamo and Home need it. */
data class LoanOverview(
    val loan: Loan,
    val account: AccountBalance,
    /** Not deleted, by due date. */
    val installments: List<LoanInstallment> = emptyList(),
) {
    /** Positive; the balance of a loan shows negative (REQ-TC-04). */
    val debt: Money get() = -account.balance

    val scheduled: List<LoanInstallment> get() = installments.filter { it.status == InstallmentStatus.SCHEDULED }

    val paid: Int get() = installments.count { it.status == InstallmentStatus.PAID }

    val next: LoanInstallment? get() = scheduled.firstOrNull()
}

/** The next installment of a loan (REQ-PRE-04): [id] is the loan's account id. */
data class LoanDue(
    val overview: LoanOverview,
    override val amount: Money,
    override val dueDate: LocalDate,
    override val daysLeft: Long,
    override val urgency: Urgency,
    override val inWindow: Boolean,
) : DuePayment {
    override val id: UUID get() = overview.loan.accountId
    override val title: String get() = "Cuota ${overview.account.account.name}"
}

/** REQ-PRE-06. */
enum class ExtraPaymentMode { REDUCE_TERM, REDUCE_PAYMENT }

enum class LoanPaymentKind(
    /** transactions.payment_kind */
    val code: String,
) {
    INSTALLMENT("LOAN_INSTALL"),
    EXTRA("LOAN_EXTRA"),
}
