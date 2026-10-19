package com.dev.julian09.financeapp.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** REQ-TC-02: effective annual or monthly (mes vencido). */
enum class RateType { EA, MV }

/** Terms of a CREDIT_CARD account (REQ-TC-01 to 03); [accountId] is also its id. [rate] is a fraction: 0.02 = 2 %. */
data class CreditCard(
    val accountId: UUID,
    val creditLimit: Money,
    val cutOffDay: Int,
    val paymentDay: Int,
    val rate: BigDecimal,
    val rateType: RateType,
    val firstInstallmentInterestFree: Boolean = false,
)

/** A card purchase in 2 to 72 installments (REQ-TC-05 to 07); its plan is [Installment]s. */
data class DeferredPurchase(
    val id: UUID,
    val cardAccountId: UUID,
    val purchaseTransactionId: UUID,
    val amount: Money,
    val installmentsCount: Int,
    val rate: BigDecimal,
    val rateType: RateType,
    val firstInstallmentInterestFree: Boolean,
    val firstDueDate: LocalDate,
)

enum class InstallmentStatus { SCHEDULED, PAID, CANCELLED }

data class Installment(
    val id: UUID,
    val deferredPurchaseId: UUID,
    val number: Int,
    val dueDate: LocalDate,
    val principal: Money,
    val interest: Money,
    val otherCosts: Money = Money.ZERO,
    val status: InstallmentStatus = InstallmentStatus.SCHEDULED,
    val paidAt: Instant? = null,
) {
    val total: Money get() = principal + interest + otherCosts
}

/** One billing period of a card: purchases from [start] to the cut on [end], paid on [dueDate]. */
data class CardPeriod(
    val start: LocalDate,
    val end: LocalDate,
    val dueDate: LocalDate,
)

/** Capital, intereses y cargos of a payment (REQ-TC-11); they add up to the amount. */
data class PaymentBreakdown(
    val principal: Money,
    val interest: Money,
    val fees: Money,
) {
    val total: Money get() = principal + interest + fees
}

/**
 * What a card owes on [CardPeriod.dueDate] (REQ-TC-08, detailed-design §6.9.3): purchases in one installment of the
 * period (and the initial debt, REQ-TC-13) plus the installments due that day. [paid] are the payments made after the
 * cut; [debt] is the whole debt, positive.
 */
data class CardStatement(
    val period: CardPeriod,
    val onePayment: Money,
    val installmentsPrincipal: Money,
    val interest: Money,
    val fees: Money,
    val paid: Money,
    val debt: Money,
    /** Interest and fees of the period not paid yet. */
    val pendingInterest: Money,
    val pendingFees: Money,
) {
    /** Pago mínimo before any payment: lo que vence en el periodo. */
    val due: Money get() = onePayment + installmentsPrincipal + interest + fees

    /** Pago mínimo still to pay. */
    val minimum: Money get() = if (paid >= due) Money.ZERO else due - paid

    /** Pago total: the whole debt plus the period's unpaid interest and fees, no future interest (REQ-TC-12). */
    val total: Money get() = debt + pendingInterest + pendingFees
}

/** A card with its debt and current statement, as Detalle de tarjeta and Home need it. */
data class CardOverview(
    val card: CreditCard,
    val account: AccountBalance,
    val statement: CardStatement,
    /** Not deleted, with installments still to pay, newest first. */
    val deferred: List<DeferredPurchase> = emptyList(),
    val installments: List<Installment> = emptyList(),
) {
    val debt: Money get() = statement.debt

    val available: Money get() = card.creditLimit - debt
}

/** Something to pay on a date (REQ-INI-03): a recurring expense or a card statement. */
sealed interface DuePayment {
    val id: UUID
    val title: String
    val amount: Money
    val dueDate: LocalDate

    /** Negative when overdue. */
    val daysLeft: Long
    val urgency: Urgency

    /** Inside the reminder window: shown in Home (REQ-REC-04). */
    val inWindow: Boolean
}

/** The payment of a card statement (REQ-TC-10): [id] is the card's account id. */
data class CardDue(
    val overview: CardOverview,
    override val daysLeft: Long,
    override val urgency: Urgency,
    override val inWindow: Boolean,
) : DuePayment {
    override val id: UUID get() = overview.card.accountId
    override val title: String get() = "Pago ${overview.account.account.name}"
    override val amount: Money get() = overview.statement.minimum
    override val dueDate: LocalDate get() = overview.statement.period.dueDate
}

enum class CardPaymentKind(
    /** transactions.payment_kind */
    val code: String,
) {
    MINIMUM("CARD_MIN"),
    TOTAL("CARD_TOTAL"),
}
