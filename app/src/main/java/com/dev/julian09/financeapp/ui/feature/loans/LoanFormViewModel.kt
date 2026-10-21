package com.dev.julian09.financeapp.ui.feature.loans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.finance.PlannedLoanInstallment
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.Amortization
import com.dev.julian09.financeapp.domain.model.LoanMode
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.usecase.ConvertLoan
import com.dev.julian09.financeapp.domain.usecase.CreateLoan
import com.dev.julian09.financeapp.domain.usecase.LoanDraft
import com.dev.julian09.financeapp.domain.usecase.LoanErrors
import com.dev.julian09.financeapp.domain.usecase.ObserveLoans
import com.dev.julian09.financeapp.domain.usecase.nextPaymentDate
import com.dev.julian09.financeapp.ui.feature.accounts.digitsOnly
import com.dev.julian09.financeapp.ui.feature.cards.parsePercent
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
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class LoanFormState(
    /** REQ-PRE-09: the simple loan being turned into a detailed one; null for Nuevo préstamo. */
    val convertingId: UUID? = null,
    val mode: LoanMode = LoanMode.DETAILED,
    val name: String = "",
    val amountDigits: String = "",
    val inProgress: Boolean = false,
    val destinationId: UUID? = null,
    val countText: String = "",
    val paymentDay: String = "",
    val rateText: String = "",
    val rateType: RateType = RateType.MV,
    val amortization: Amortization = Amortization.FIXED_PAYMENT,
    val firstDueDate: LocalDate? = null,
    val monthlyDigits: String = "",
    val insurance: Boolean = false,
    val insuranceDigits: String = "",
    val otherCosts: Boolean = false,
    val otherCostsDigits: String = "",
    val errors: LoanErrors = emptyMap(),
    val saved: Boolean = false,
) {
    val draft: LoanDraft
        get() =
            LoanDraft(
                name = name,
                mode = mode,
                amount = pesos(amountDigits),
                paymentDay = paymentDay.toIntOrNull() ?: 0,
                inProgress = inProgress || convertingId != null,
                destinationId = destinationId,
                count = countText.toIntOrNull() ?: 0,
                rate = parsePercent(rateText),
                rateType = rateType,
                amortization = amortization,
                firstDueDate = firstDueDate,
                monthlyPayment = pesos(monthlyDigits),
                insurance = if (insurance) pesos(insuranceDigits) else Money.ZERO,
                otherCosts = if (otherCosts) pesos(otherCostsDigits) else Money.ZERO,
            )

    /** "Primera cuota estimada" of the form; null until the detailed fields are valid. */
    val firstInstallment: PlannedLoanInstallment? get() = draft.plan().firstOrNull()
}

private fun pesos(digits: String) = Money.ofPesos(digits.ifEmpty { "0" }.toLong())

/** Nuevo préstamo (S10-UI01), detailed or simple, and "Pasar a detallado" of a simple loan (ConvertLoanRoute). */
@HiltViewModel
class LoanFormViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        accounts: AccountRepository,
        observeLoans: ObserveLoans,
        private val createLoan: CreateLoan,
        private val convertLoan: ConvertLoan,
        private val clock: Clock,
    ) : ViewModel() {
        private val _state = MutableStateFlow(LoanFormState(convertingId = savedState.get<String>("loanId")?.let(UUID::fromString)))
        val state: StateFlow<LoanFormState> = _state.asStateFlow()

        /** Where the money can arrive: active accounts that are not debts. */
        val destinations: StateFlow<List<Account>> =
            accounts
                .observeActive()
                .map { list -> list.filter { !it.type.isDebt } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        init {
            _state.value.convertingId?.let { id ->
                viewModelScope.launch {
                    val loan = observeLoans().first().firstOrNull { it.loan.accountId == id } ?: return@launch
                    _state.update {
                        it.copy(
                            name = loan.account.account.name,
                            amountDigits = loan.debt.wholePesosText(),
                            paymentDay = loan.loan.paymentDay.toString(),
                            firstDueDate = nextPaymentDate(clock.today(), loan.loan.paymentDay),
                        )
                    }
                }
            }
        }

        private fun edit(transform: (LoanFormState) -> LoanFormState) = _state.update { transform(it).copy(errors = emptyMap()) }

        fun setName(value: String) = edit { it.copy(name = value) }

        fun setMode(value: LoanMode) = edit { it.copy(mode = value) }

        fun setAmount(value: String) = edit { it.copy(amountDigits = digitsOnly(value)) }

        fun setInProgress(value: Boolean) = edit { it.copy(inProgress = value) }

        fun setDestination(id: UUID) = edit { it.copy(destinationId = id) }

        fun setCount(value: String) = edit { it.copy(countText = value.filter(Char::isDigit).take(3)) }

        fun setPaymentDay(value: String) =
            edit {
                val text = value.filter(Char::isDigit).take(2)
                val day = text.toIntOrNull()
                // The first due date follows the payment day until it is picked by hand
                val due = if (day in 1..31 && it.firstDueDate == null) nextPaymentDate(clock.today(), day!!) else it.firstDueDate
                it.copy(paymentDay = text, firstDueDate = due)
            }

        fun setRate(value: String) = edit { it.copy(rateText = value.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(8)) }

        fun setRateType(value: RateType) = edit { it.copy(rateType = value) }

        fun setAmortization(value: Amortization) = edit { it.copy(amortization = value) }

        fun setFirstDueDate(value: LocalDate) = edit { it.copy(firstDueDate = value) }

        fun setMonthly(value: String) = edit { it.copy(monthlyDigits = digitsOnly(value)) }

        fun setInsurance(on: Boolean) = edit { it.copy(insurance = on) }

        fun setInsuranceAmount(value: String) = edit { it.copy(insuranceDigits = digitsOnly(value)) }

        fun setOtherCosts(on: Boolean) = edit { it.copy(otherCosts = on) }

        fun setOtherCostsAmount(value: String) = edit { it.copy(otherCostsDigits = digitsOnly(value)) }

        fun save() {
            val s = _state.value
            viewModelScope.launch {
                val errors = s.convertingId?.let { convertLoan(it, s.draft) } ?: createLoan(s.draft)
                _state.update { if (errors.isEmpty()) it.copy(saved = true) else it.copy(errors = errors) }
            }
        }
    }

/** Whole pesos as digits, for an amount field. */
internal fun Money.wholePesosText(): String = toBigDecimal().setScale(0, java.math.RoundingMode.DOWN).toPlainString()
