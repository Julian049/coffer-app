package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.finance.CardPeriodCalculator
import com.dev.julian09.financeapp.domain.finance.InstallmentCalculator
import com.dev.julian09.financeapp.domain.finance.RateConverter
import com.dev.julian09.financeapp.domain.finance.RecurrenceCalculator
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.CardDue
import com.dev.julian09.financeapp.domain.model.CardOverview
import com.dev.julian09.financeapp.domain.model.CardPaymentKind
import com.dev.julian09.financeapp.domain.model.CreditCard
import com.dev.julian09.financeapp.domain.model.DeferredPurchase
import com.dev.julian09.financeapp.domain.model.Installment
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CardRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

private const val CARD_NAME_MAX = 60
private const val MAX_INSTALLMENTS = 72

/** The contract's Rate: a fraction with up to 3 integer digits and 6 decimals (NUMERIC(9,6)). */
private val RATE_MAX = BigDecimal(1000)

typealias CardErrors = Map<CardField, LedgerError>

enum class CardField { NAME, LIMIT, DEBT, CUT_OFF, PAYMENT, RATE }

/** Nueva tarjeta de crédito (REQ-TC-01 to 03, 13): [debt] is what is owed today, 0 by default (M2, PO 2026-10-20). */
data class CardDraft(
    val name: String,
    val creditLimit: Money,
    val debt: Money,
    val cutOffDay: Int,
    val paymentDay: Int,
    /** A fraction: 0.02 = 2 %. */
    val rate: BigDecimal,
    val rateType: RateType,
    val firstInstallmentInterestFree: Boolean,
    val icon: String? = null,
    val color: String? = null,
)

/** A rate the server takes (ck_ccd_rate, contract Rate), rounded to its 6 decimals. */
internal fun cleanRate(rate: BigDecimal): BigDecimal? =
    rate.setScale(6, RoundingMode.HALF_EVEN).takeIf { it.signum() >= 0 && it < RATE_MAX }

private fun CardDraft.errors(creating: Boolean): CardErrors {
    val errors = mutableMapOf<CardField, LedgerError>()
    when {
        name.isBlank() -> errors[CardField.NAME] = LedgerError.NAME_REQUIRED
        name.trim().length > CARD_NAME_MAX -> errors[CardField.NAME] = LedgerError.NAME_TOO_LONG
    }
    if (creditLimit <= Money.ZERO) errors[CardField.LIMIT] = LedgerError.AMOUNT_REQUIRED
    if (creating && debt > creditLimit) errors[CardField.DEBT] = LedgerError.OVER_LIMIT
    if (cutOffDay !in 1..31) errors[CardField.CUT_OFF] = LedgerError.DAY_INVALID
    if (paymentDay !in 1..31) errors[CardField.PAYMENT] = LedgerError.DAY_INVALID
    if (cleanRate(rate) == null) errors[CardField.RATE] = LedgerError.RATE_INVALID
    return errors
}

private fun CardDraft.terms(accountId: UUID) =
    CreditCard(accountId, creditLimit, cutOffDay, paymentDay, cleanRate(rate)!!, rateType, firstInstallmentInterestFree)

/** REQ-TC-01, 13: the account (its initial balance is the current debt) and its terms, written together. */
class CreateCard
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val cards: CardRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(draft: CardDraft): CardErrors {
            draft.errors(creating = true).takeIf { it.isNotEmpty() }?.let { return it }
            val last = accounts.observeBalances().first().maxOfOrNull { it.account.sortOrder } ?: -1
            val account =
                Account(
                    id = UUID.randomUUID(),
                    name = draft.name.trim(),
                    type = AccountType.CREDIT_CARD,
                    initialBalance = draft.debt,
                    icon = draft.icon,
                    color = draft.color,
                    sortOrder = last + 1,
                )
            cards.create(account, draft.terms(account.id))
            scheduler.requestSync()
            return emptyMap()
        }
    }

/** Editar tarjeta: name and terms; the initial debt stays as it was created (the account's initial balance). */
class UpdateCard
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val cards: CardRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(
            id: UUID,
            draft: CardDraft,
        ): CardErrors {
            draft.errors(creating = false).takeIf { it.isNotEmpty() }?.let { return it }
            val account =
                accounts
                    .observeBalances()
                    .first()
                    .firstOrNull { it.account.id == id }
                    ?.account ?: return emptyMap()
            cards.update(account.copy(name = draft.name.trim()), draft.terms(id))
            scheduler.requestSync()
            return emptyMap()
        }
    }

/** Every card with its debt, current statement and live deferred purchases (Detalle de tarjeta, Home). */
class ObserveCards
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val cards: CardRepository,
        private val clock: Clock,
    ) {
        operator fun invoke(): Flow<List<CardOverview>> =
            combine(
                accounts.observeBalances(),
                cards.observeCards(),
                cards.observeDeferred(),
                cards.observeInstallments(),
                cards.observeCardTransactions(),
            ) { balances, terms, deferred, installments, transactions ->
                val today = clock.today()
                terms.mapNotNull { card ->
                    val account = balances.firstOrNull { it.account.id == card.accountId } ?: return@mapNotNull null
                    val own = deferred.filter { it.cardAccountId == card.accountId }
                    val ownIds = own.map { it.id }.toSet()
                    val plan = installments.filter { it.deferredPurchaseId in ownIds }
                    val moves = transactions.filter { it.accountId == card.accountId || it.destinationAccountId == card.accountId }
                    val statement =
                        CardPeriodCalculator.statement(card, account.account, -account.balance, moves, plan, today, clock.zone())
                    val live =
                        own
                            .filter { d -> plan.any { it.deferredPurchaseId == d.id && it.status == InstallmentStatus.SCHEDULED } }
                            .sortedByDescending { it.firstDueDate }
                    CardOverview(card, account, statement, live, plan)
                }
            }
    }

/** REQ-TC-10, PO 2026-10-19: the card's payment is a pending like a recurring one, with the same window and urgency. */
class ObserveCardDues
    @Inject
    constructor(
        private val cards: ObserveCards,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) {
        operator fun invoke(): Flow<List<CardDue>> =
            combine(cards(), settings.observe()) { all, s ->
                val today = clock.today()
                all
                    .filter { it.account.account.active && it.statement.minimum > Money.ZERO }
                    .map {
                        val due = it.statement.period.dueDate
                        CardDue(
                            it,
                            daysLeft = ChronoUnit.DAYS.between(today, due),
                            urgency = RecurrenceCalculator.urgency(due, today),
                            inWindow = RecurrenceCalculator.inWindow(due, today, s.reminderLeadDays),
                        )
                    }.sortedBy { it.dueDate }
            }
    }

/**
 * SEQ-06 (REQ-TC-05 to 07): one installment is an ordinary expense on the card; two or more also get a deferred
 * purchase and its plan, with the card's "first installment interest-free" and the rate of the form (REQ-TC-06).
 */
class DeferCardPurchase
    @Inject
    constructor(
        private val cards: CardRepository,
        private val register: RegisterTransaction,
        private val validator: TransactionValidator,
        private val scheduler: SyncScheduler,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            purchase: Transaction,
            count: Int,
            rate: BigDecimal,
            rateType: RateType,
        ): TransactionErrors {
            if (count == 1) return register(purchase.copy(installments = 1))
            val errors = mutableMapOf<TransactionField, LedgerError>()
            if (count !in 2..MAX_INSTALLMENTS) errors[TransactionField.INSTALLMENTS] = LedgerError.INSTALLMENTS_INVALID
            val cleanRate = cleanRate(rate)
            if (cleanRate == null) errors[TransactionField.RATE] = LedgerError.RATE_INVALID
            val clean = purchase.copy(type = TransactionType.EXPENSE, installments = count).normalized()
            errors.putAll(validator.check(clean))
            val card = cards.observeCards().first().firstOrNull { it.accountId == clean.accountId }
            if (card == null) errors.putIfAbsent(TransactionField.ACCOUNT, LedgerError.ACCOUNT_REQUIRED)
            if (errors.isNotEmpty() || card == null || cleanRate == null) return errors
            val first =
                CardPeriodCalculator.firstDueDate(
                    clean.occurredAt.atZone(clock.zone()).toLocalDate(),
                    card.cutOffDay,
                    card.paymentDay,
                )
            val deferred =
                DeferredPurchase(
                    id = UUID.randomUUID(),
                    cardAccountId = card.accountId,
                    purchaseTransactionId = clean.id,
                    amount = clean.amount,
                    installmentsCount = count,
                    rate = cleanRate,
                    rateType = rateType,
                    firstInstallmentInterestFree = card.firstInstallmentInterestFree,
                    firstDueDate = first,
                )
            val plan =
                InstallmentCalculator.plan(
                    clean.amount,
                    count,
                    RateConverter.toMonthly(cleanRate, rateType),
                    first,
                    card.paymentDay,
                    card.firstInstallmentInterestFree,
                )
            cards.insertDeferred(
                clean,
                deferred,
                plan.map { Installment(UUID.randomUUID(), deferred.id, it.number, it.dueDate, it.principal, it.interest) },
            )
            scheduler.requestSync()
            return emptyMap()
        }
    }

/**
 * SEQ-07 (REQ-TC-10 to 12): a transfer to the card split into principal, interest and fees. Covering the minimum
 * settles the period's installments; the total also cancels the future ones, so no future interest is charged.
 */
class PayCreditCard
    @Inject
    constructor(
        private val observeCards: ObserveCards,
        private val cards: CardRepository,
        private val validator: TransactionValidator,
        private val scheduler: SyncScheduler,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            cardId: UUID,
            kind: CardPaymentKind,
            sourceAccountId: UUID,
            amount: Money,
            occurredAt: Instant,
        ): TransactionErrors {
            val overview = observeCards().first().firstOrNull { it.card.accountId == cardId } ?: return emptyMap()
            val statement = overview.statement
            val split = CardPeriodCalculator.breakdown(statement, amount)
            val payment =
                Transaction(
                    id = UUID.randomUUID(),
                    type = TransactionType.TRANSFER,
                    amount = amount,
                    occurredAt = occurredAt,
                    accountId = sourceAccountId,
                    destinationAccountId = cardId,
                    note = "Pago ${overview.account.account.name}",
                    principal = split.principal,
                    interest = split.interest,
                    fees = split.fees,
                    paymentKind = kind.code,
                )
            validator.check(payment).takeIf { it.isNotEmpty() }?.let { return it }
            val now = clock.now()
            val due = statement.period.dueDate
            val covered = kind == CardPaymentKind.TOTAL || amount >= statement.minimum
            val settled =
                overview.installments.filter { it.status == InstallmentStatus.SCHEDULED }.mapNotNull {
                    when {
                        !it.dueDate.isAfter(due) && covered -> it.copy(status = InstallmentStatus.PAID, paidAt = now)
                        it.dueDate.isAfter(due) && kind == CardPaymentKind.TOTAL -> it.copy(status = InstallmentStatus.CANCELLED)
                        else -> null
                    }
                }
            cards.pay(payment, settled)
            scheduler.requestSync()
            return emptyMap()
        }
    }
