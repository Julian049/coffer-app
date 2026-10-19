package com.dev.julian09.financeapp.ui.feature.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.finance.CardPeriodCalculator
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.CardOverview
import com.dev.julian09.financeapp.domain.model.CardPaymentKind
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.PaymentBreakdown
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.usecase.ObserveCards
import com.dev.julian09.financeapp.domain.usecase.PayCreditCard
import com.dev.julian09.financeapp.domain.usecase.TransactionErrors
import com.dev.julian09.financeapp.ui.feature.accounts.digitsOnly
import com.dev.julian09.financeapp.ui.feature.accounts.wholePesos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class PayCardState(
    val overview: CardOverview? = null,
    val kind: CardPaymentKind = CardPaymentKind.MINIMUM,
    val amountDigits: String = "",
    val sourceId: UUID? = null,
    val occurredAt: Instant = Instant.EPOCH,
    val errors: TransactionErrors = emptyMap(),
    /** Registered: the sheet closes. */
    val done: Boolean = false,
) {
    val amount: Money get() = Money.ofPesos(amountDigits.ifEmpty { "0" }.toLong())

    /** How the amount splits (cargos, then intereses, then capital). */
    val breakdown: PaymentBreakdown? get() = overview?.let { CardPeriodCalculator.breakdown(it.statement, amount) }
}

/** Hoja · Pagar tarjeta (S7-UI03, SEQ-07), opened from Home, Detalle de tarjeta or Gastos recurrentes. */
@HiltViewModel
class PayCardViewModel
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val cards: ObserveCards,
        private val pay: PayCreditCard,
        private val clock: Clock,
    ) : ViewModel() {
        private val _state = MutableStateFlow(PayCardState())
        val state: StateFlow<PayCardState> = _state.asStateFlow()

        /** Where the money comes from: active accounts that are not debts. */
        val sources: StateFlow<List<AccountBalance>> =
            accounts
                .observeBalances()
                .map { list -> list.filter { it.account.active && !it.account.type.isDebt } }
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        fun open(cardId: UUID) {
            viewModelScope.launch {
                val overview = cards().first().firstOrNull { it.card.accountId == cardId } ?: return@launch
                val minimum = overview.statement.minimum
                val kind = if (minimum > Money.ZERO) CardPaymentKind.MINIMUM else CardPaymentKind.TOTAL
                _state.value =
                    PayCardState(
                        overview = overview,
                        kind = kind,
                        amountDigits = suggested(overview, kind).wholePesos(),
                        sourceId =
                            accounts
                                .observeBalances()
                                .first()
                                .firstOrNull { it.account.active && !it.account.type.isDebt }
                                ?.account
                                ?.id,
                        occurredAt = clock.now(),
                    )
            }
        }

        private fun suggested(
            overview: CardOverview,
            kind: CardPaymentKind,
        ) = if (kind == CardPaymentKind.MINIMUM) overview.statement.minimum else overview.statement.total

        private fun edit(transform: (PayCardState) -> PayCardState) = _state.update { transform(it).copy(errors = emptyMap()) }

        /** Choosing an option proposes its amount; it stays editable (REQ-TC-11). */
        fun setKind(value: CardPaymentKind) =
            edit { s -> s.copy(kind = value, amountDigits = s.overview?.let { suggested(it, value).wholePesos() } ?: s.amountDigits) }

        fun setAmount(text: String) = edit { it.copy(amountDigits = digitsOnly(text)) }

        fun setSource(id: UUID) = edit { it.copy(sourceId = id) }

        fun setOccurredAt(value: Instant) = edit { it.copy(occurredAt = value) }

        fun confirm() {
            val s = _state.value
            val cardId = s.overview?.card?.accountId ?: return
            viewModelScope.launch {
                val errors = pay(cardId, s.kind, s.sourceId ?: UUID(0, 0), s.amount, s.occurredAt)
                _state.update { if (errors.isEmpty()) it.copy(done = true) else it.copy(errors = errors) }
            }
        }

        fun reset() {
            _state.value = PayCardState()
        }
    }
