package com.dev.julian09.financeapp.ui.feature.cards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CardRepository
import com.dev.julian09.financeapp.domain.usecase.CardDraft
import com.dev.julian09.financeapp.domain.usecase.CardErrors
import com.dev.julian09.financeapp.domain.usecase.CardField
import com.dev.julian09.financeapp.domain.usecase.CreateCard
import com.dev.julian09.financeapp.domain.usecase.UpdateCard
import com.dev.julian09.financeapp.ui.feature.accounts.digitsOnly
import com.dev.julian09.financeapp.ui.feature.accounts.wholePesos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

data class CardFormState(
    /** Null for Nueva tarjeta. */
    val editingId: UUID? = null,
    val name: String = "",
    val limitDigits: String = "",
    val availableDigits: String = "",
    /** The debt the card was created with, shown when editing. */
    val initialDebt: Money = Money.ZERO,
    val cutOffDay: String = "",
    val paymentDay: String = "",
    val rateText: String = "",
    val rateType: RateType = RateType.MV,
    val firstInstallmentInterestFree: Boolean = false,
    val errors: CardErrors = emptyMap(),
    val saved: Boolean = false,
) {
    val limit: Money get() = pesos(limitDigits)
    val available: Money get() = pesos(availableDigits)

    /** REQ-TC-13: cupo total − cupo disponible, never below zero while typing. */
    val debt: Money get() = if (editingId != null) initialDebt else (limit - available).let { if (it.isNegative) Money.ZERO else it }

    val rate: BigDecimal? get() = parsePercent(rateText)
}

private fun pesos(digits: String) = Money.ofPesos(digits.ifEmpty { "0" }.toLong())

private fun day(text: String) = text.filter(Char::isDigit).take(2)

/** Nueva tarjeta de crédito and its edit (S7-UI01); the route's id decides which. */
@HiltViewModel
class CardFormViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        private val accounts: AccountRepository,
        private val cards: CardRepository,
        private val createCard: CreateCard,
        private val updateCard: UpdateCard,
    ) : ViewModel() {
        private val _state = MutableStateFlow(CardFormState(editingId = savedState.get<String>("id")?.let(UUID::fromString)))
        val state: StateFlow<CardFormState> = _state.asStateFlow()

        init {
            _state.value.editingId?.let { id ->
                viewModelScope.launch {
                    val account =
                        accounts
                            .observeBalances()
                            .first()
                            .firstOrNull { it.account.id == id }
                            ?.account ?: return@launch
                    val card = cards.observeCards().first().firstOrNull { it.accountId == id } ?: return@launch
                    _state.update {
                        it.copy(
                            name = account.name,
                            limitDigits = card.creditLimit.wholePesos(),
                            initialDebt = account.initialBalance,
                            cutOffDay = card.cutOffDay.toString(),
                            paymentDay = card.paymentDay.toString(),
                            rateText = percentText(card.rate),
                            rateType = card.rateType,
                            firstInstallmentInterestFree = card.firstInstallmentInterestFree,
                        )
                    }
                }
            }
        }

        private fun edit(transform: (CardFormState) -> CardFormState) = _state.update { transform(it).copy(errors = emptyMap()) }

        fun setName(value: String) = edit { it.copy(name = value) }

        fun setLimit(value: String) = edit { it.copy(limitDigits = digitsOnly(value)) }

        fun setAvailable(value: String) = edit { it.copy(availableDigits = digitsOnly(value)) }

        fun setCutOffDay(value: String) = edit { it.copy(cutOffDay = day(value)) }

        fun setPaymentDay(value: String) = edit { it.copy(paymentDay = day(value)) }

        fun setRate(value: String) = edit { it.copy(rateText = value.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(8)) }

        fun setRateType(value: RateType) = edit { it.copy(rateType = value) }

        fun setFirstInterestFree(value: Boolean) = edit { it.copy(firstInstallmentInterestFree = value) }

        fun save() {
            val s = _state.value
            val rate = s.rate
            if (rate == null) {
                _state.update { it.copy(errors = mapOf(CardField.RATE to LedgerError.RATE_INVALID)) }
                return
            }
            val draft =
                CardDraft(
                    name = s.name,
                    creditLimit = s.limit,
                    available = s.available,
                    cutOffDay = s.cutOffDay.toIntOrNull() ?: 0,
                    paymentDay = s.paymentDay.toIntOrNull() ?: 0,
                    rate = rate,
                    rateType = s.rateType,
                    firstInstallmentInterestFree = s.firstInstallmentInterestFree,
                )
            viewModelScope.launch {
                val errors = if (s.editingId == null) createCard(draft) else updateCard(s.editingId, draft)
                _state.update { if (errors.isEmpty()) it.copy(saved = true) else it.copy(errors = errors) }
            }
        }
    }
