package com.dev.julian09.financeapp.ui.feature.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.finance.PlannedLoanInstallment
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.ExtraPaymentMode
import com.dev.julian09.financeapp.domain.model.LoanMode
import com.dev.julian09.financeapp.domain.model.LoanOverview
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.usecase.ExtraPayment
import com.dev.julian09.financeapp.domain.usecase.ObserveLoans
import com.dev.julian09.financeapp.domain.usecase.PayLoanInstallment
import com.dev.julian09.financeapp.domain.usecase.TransactionErrors
import com.dev.julian09.financeapp.ui.feature.accounts.digitsOnly
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
import java.util.UUID
import javax.inject.Inject

/** Which loan sheet is open: pay the installment or an extra payment to principal. */
enum class LoanSheet { PAY, EXTRA }

data class LoanPaymentState(
    val overview: LoanOverview? = null,
    val sheet: LoanSheet = LoanSheet.PAY,
    val amountDigits: String = "",
    val mode: ExtraPaymentMode = ExtraPaymentMode.REDUCE_TERM,
    val sourceId: UUID? = null,
    val errors: TransactionErrors = emptyMap(),
    val done: Boolean = false,
) {
    val amount: Money get() = Money.ofPesos(amountDigits.ifEmpty { "0" }.toLong())

    /** REQ-PRE-06: the sheet shows what each option leaves. */
    fun simulation(mode: ExtraPaymentMode): List<PlannedLoanInstallment> =
        overview?.takeIf { amount > Money.ZERO }?.let { ExtraPayment.simulate(it, amount, mode) }.orEmpty()
}

/** Hoja · Pagar cuota and Hoja · Abono a capital (S10-UI02), from Home, Detalle de préstamo or a notice. */
@HiltViewModel
class LoanPaymentViewModel
    @Inject
    constructor(
        private val accounts: AccountRepository,
        private val loans: ObserveLoans,
        private val pay: PayLoanInstallment,
        private val extra: ExtraPayment,
        private val clock: Clock,
    ) : ViewModel() {
        private val _state = MutableStateFlow(LoanPaymentState())
        val state: StateFlow<LoanPaymentState> = _state.asStateFlow()

        /** Where the money comes from: active accounts that are not debts. */
        val sources: StateFlow<List<AccountBalance>> =
            accounts
                .observeBalances()
                .map { list -> list.filter { it.account.active && !it.account.type.isDebt } }
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        fun open(
            loanId: UUID,
            sheet: LoanSheet,
        ) {
            viewModelScope.launch {
                val overview = loans().first().firstOrNull { it.loan.accountId == loanId } ?: return@launch
                val suggested =
                    when {
                        sheet == LoanSheet.EXTRA -> Money.ZERO
                        overview.loan.mode == LoanMode.DETAILED -> overview.next?.total ?: Money.ZERO
                        else -> (overview.loan.monthlyPayment ?: Money.ZERO).let { if (it > overview.debt) overview.debt else it }
                    }
                _state.value =
                    LoanPaymentState(
                        overview = overview,
                        sheet = sheet,
                        amountDigits = suggested.takeIf { it > Money.ZERO }?.wholePesosText().orEmpty(),
                        sourceId =
                            accounts
                                .observeBalances()
                                .first()
                                .firstOrNull { it.account.active && !it.account.type.isDebt }
                                ?.account
                                ?.id,
                    )
            }
        }

        private fun edit(transform: (LoanPaymentState) -> LoanPaymentState) = _state.update { transform(it).copy(errors = emptyMap()) }

        fun setAmount(text: String) = edit { it.copy(amountDigits = digitsOnly(text)) }

        fun setMode(value: ExtraPaymentMode) = edit { it.copy(mode = value) }

        fun setSource(id: UUID) = edit { it.copy(sourceId = id) }

        fun confirm() {
            val s = _state.value
            val loanId = s.overview?.loan?.accountId ?: return
            viewModelScope.launch {
                val errors =
                    when (s.sheet) {
                        LoanSheet.PAY -> pay(loanId, s.sourceId, s.amount, clock.now())
                        LoanSheet.EXTRA -> extra(loanId, s.sourceId, s.amount, s.mode, clock.now())
                    }
                _state.update { if (errors.isEmpty()) it.copy(done = true) else it.copy(errors = errors) }
            }
        }

        fun reset() {
            _state.value = LoanPaymentState()
        }
    }
