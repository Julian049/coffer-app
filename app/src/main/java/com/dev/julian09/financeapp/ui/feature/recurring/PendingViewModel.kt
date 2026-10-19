package com.dev.julian09.financeapp.ui.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.finance.RecurrenceCalculator
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.PendingPayment
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.RecurringRepository
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.usecase.EndRecurrence
import com.dev.julian09.financeapp.domain.usecase.PayPending
import com.dev.julian09.financeapp.domain.usecase.SkipPending
import com.dev.julian09.financeapp.domain.usecase.TransactionErrors
import com.dev.julian09.financeapp.ui.feature.accounts.digitsOnly
import com.dev.julian09.financeapp.ui.feature.accounts.wholePesos
import com.dev.julian09.financeapp.ui.feature.transactions.categoryLabel
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
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

enum class PendingStep { ACTIONS, PAY, CONFIRM_END }

data class PendingSheetState(
    val pending: PendingPayment? = null,
    /** "Hogar > Arriendo" */
    val categoryLabel: String = "",
    /** Where "Omitir esta vez" jumps to. */
    val nextDate: LocalDate? = null,
    val step: PendingStep = PendingStep.ACTIONS,
    val amountDigits: String = "",
    val occurredAt: Instant = Instant.EPOCH,
    val accountId: UUID? = null,
    val errors: TransactionErrors = emptyMap(),
    /** Paid, skipped or finalized: the sheet closes. */
    val done: Boolean = false,
)

/** Hoja · Pendiente and Hoja · Pagar pendiente (S6-UI02), shared by Home and Gastos recurrentes. */
@HiltViewModel
class PendingViewModel
    @Inject
    constructor(
        accounts: AccountRepository,
        private val categories: CategoryRepository,
        private val recurring: RecurringRepository,
        private val settings: SettingsRepository,
        private val pay: PayPending,
        private val skip: SkipPending,
        private val end: EndRecurrence,
        private val clock: Clock,
    ) : ViewModel() {
        private val _state = MutableStateFlow(PendingSheetState())
        val state: StateFlow<PendingSheetState> = _state.asStateFlow()

        val accounts: StateFlow<List<AccountBalance>> =
            accounts
                .observeBalances()
                .map { list -> list.filter { it.account.active } }
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        /** [payNow] opens straight on the payment (Registrar pago of the list). */
        fun open(
            id: UUID,
            payNow: Boolean = false,
        ) {
            viewModelScope.launch {
                val r = recurring.observe(id).first() ?: return@launch
                val cats = categories.observeAll().first()
                _state.value =
                    PendingSheetState(
                        pending = RecurrenceCalculator.pending(r, clock.today(), settings.observe().first().reminderLeadDays),
                        categoryLabel = cats.firstOrNull { it.id == r.categoryId }?.let { categoryLabel(it, cats) }.orEmpty(),
                        nextDate = RecurrenceCalculator.next(r, r.nextDueDate),
                        step = if (payNow) PendingStep.PAY else PendingStep.ACTIONS,
                        // REQ-REC-07: last payment, today and the recurring expense's account, all editable
                        amountDigits = r.suggestedAmount.wholePesos(),
                        occurredAt = clock.now(),
                        accountId = r.accountId,
                    )
            }
        }

        private fun edit(transform: (PendingSheetState) -> PendingSheetState) = _state.update { transform(it).copy(errors = emptyMap()) }

        fun startPay() = edit { it.copy(step = PendingStep.PAY) }

        fun askEnd() = edit { it.copy(step = PendingStep.CONFIRM_END) }

        fun back() = edit { it.copy(step = PendingStep.ACTIONS) }

        fun setAmount(text: String) = edit { it.copy(amountDigits = digitsOnly(text)) }

        fun setOccurredAt(value: Instant) = edit { it.copy(occurredAt = value) }

        fun setAccount(id: UUID) = edit { it.copy(accountId = id) }

        fun confirmPay() {
            val s = _state.value
            val id = s.pending?.recurring?.id ?: return
            viewModelScope.launch {
                val errors = pay(id, Money.ofPesos(s.amountDigits.ifEmpty { "0" }.toLong()), s.occurredAt, s.accountId ?: UUID(0, 0))
                _state.update { if (errors.isEmpty()) it.copy(done = true) else it.copy(errors = errors) }
            }
        }

        fun confirmSkip() = finish { skip(it) }

        fun confirmEnd() = finish { end(it) }

        private fun finish(action: suspend (UUID) -> Unit) {
            val id =
                _state.value.pending
                    ?.recurring
                    ?.id ?: return
            viewModelScope.launch {
                action(id)
                _state.update { it.copy(done = true) }
            }
        }

        fun reset() {
            _state.value = PendingSheetState()
        }
    }
