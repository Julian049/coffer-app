package com.dev.julian09.financeapp.ui.feature.loans

import androidx.compose.runtime.getValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.LoanMode
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.usecase.EditLoan
import com.dev.julian09.financeapp.domain.usecase.LoanEdit
import com.dev.julian09.financeapp.domain.usecase.LoanErrors
import com.dev.julian09.financeapp.domain.usecase.ObserveLoans
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.feature.accounts.digitsOnly
import com.dev.julian09.financeapp.ui.feature.accounts.wholePesos
import com.dev.julian09.financeapp.ui.feature.cards.parsePercent
import com.dev.julian09.financeapp.ui.feature.cards.percentText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class LoanEditState(
    val loanId: UUID,
    val loaded: Boolean = false,
    val detailed: Boolean = true,
    /** «$5.000.000 · 24 cuotas · desde 5 nov 2026». */
    val summary: String = "",
    val name: String = "",
    val rateText: String = "",
    val rateType: RateType = RateType.MV,
    val paymentDay: String = "",
    val insurance: Boolean = false,
    val insuranceDigits: String = "",
    val otherCosts: Boolean = false,
    val otherCostsDigits: String = "",
    val errors: LoanErrors = emptyMap(),
    val saved: Boolean = false,
)

private val DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-CO"))

/** Editar préstamo (S12-DOM01, Figma `100:326`). */
@HiltViewModel
class LoanEditViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        private val observeLoans: ObserveLoans,
        private val editLoan: EditLoan,
    ) : ViewModel() {
        private val _state = MutableStateFlow(LoanEditState(UUID.fromString(savedState.get<String>("id"))))
        val state: StateFlow<LoanEditState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                val overview = observeLoans().first().firstOrNull { it.loan.accountId == _state.value.loanId } ?: return@launch
                val loan = overview.loan
                val summary =
                    listOfNotNull(
                        loan.principal?.let(::formatMoney),
                        loan.installmentsCount?.let { "$it cuotas" },
                        loan.firstDueDate?.let { "desde " + DAY_MONTH_YEAR.format(it).replace(".", "") },
                    ).joinToString(" · ")
                _state.update {
                    it.copy(
                        loaded = true,
                        detailed = loan.mode == LoanMode.DETAILED,
                        summary = summary,
                        name = overview.account.account.name,
                        rateText = loan.rate?.let(::percentText).orEmpty(),
                        rateType = loan.rateType ?: RateType.MV,
                        paymentDay = loan.paymentDay.toString(),
                        insurance = loan.lifeInsurance > Money.ZERO,
                        insuranceDigits = loan.lifeInsurance.wholePesos(),
                        otherCosts = loan.otherCosts > Money.ZERO,
                        otherCostsDigits = loan.otherCosts.wholePesos(),
                    )
                }
            }
        }

        private fun edit(transform: (LoanEditState) -> LoanEditState) = _state.update { transform(it).copy(errors = emptyMap()) }

        fun setName(value: String) = edit { it.copy(name = value) }

        fun setRate(value: String) = edit { it.copy(rateText = value.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(8)) }

        fun setRateType(value: RateType) = edit { it.copy(rateType = value) }

        fun setPaymentDay(value: String) = edit { it.copy(paymentDay = value.filter(Char::isDigit).take(2)) }

        fun setInsurance(value: Boolean) = edit { it.copy(insurance = value) }

        fun setInsuranceAmount(value: String) = edit { it.copy(insuranceDigits = digitsOnly(value)) }

        fun setOtherCosts(value: Boolean) = edit { it.copy(otherCosts = value) }

        fun setOtherCostsAmount(value: String) = edit { it.copy(otherCostsDigits = digitsOnly(value)) }

        fun save() {
            val s = _state.value

            fun pesos(
                on: Boolean,
                digits: String,
            ) = if (on) Money.ofPesos(digits.ifEmpty { "0" }.toLong()) else Money.ZERO
            viewModelScope.launch {
                val errors =
                    editLoan(
                        s.loanId,
                        LoanEdit(
                            name = s.name,
                            // Text that is not a number is a negative rate, which the use case refuses with its message
                            rate = parsePercent(s.rateText) ?: BigDecimal(-1),
                            rateType = s.rateType,
                            paymentDay = s.paymentDay.toIntOrNull() ?: 0,
                            lifeInsurance = pesos(s.insurance, s.insuranceDigits),
                            otherCosts = pesos(s.otherCosts, s.otherCostsDigits),
                        ),
                    )
                _state.update { if (errors.isEmpty()) it.copy(saved = true) else it.copy(errors = errors) }
            }
        }
    }
