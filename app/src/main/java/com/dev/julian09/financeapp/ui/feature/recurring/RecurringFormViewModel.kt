package com.dev.julian09.financeapp.ui.feature.recurring

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.Frequency
import com.dev.julian09.financeapp.domain.model.IntervalUnit
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Recurring
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.RecurringRepository
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import com.dev.julian09.financeapp.domain.usecase.CreateRecurring
import com.dev.julian09.financeapp.domain.usecase.EndRecurrence
import com.dev.julian09.financeapp.domain.usecase.RecurringErrors
import com.dev.julian09.financeapp.domain.usecase.UpdateRecurring
import com.dev.julian09.financeapp.ui.feature.accounts.digitsOnly
import com.dev.julian09.financeapp.ui.feature.accounts.wholePesos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class RecurringFormState(
    val editingId: UUID? = null,
    val name: String = "",
    /** Whole pesos as typed, digits only. */
    val amountDigits: String = "",
    val accountId: UUID? = null,
    val categoryId: UUID? = null,
    val startDate: LocalDate,
    val frequency: Frequency = Frequency.MONTHLY,
    val intervalDigits: String = "2",
    val intervalUnit: IntervalUnit = IntervalUnit.MONTH,
    val errors: RecurringErrors = emptyMap(),
    val saved: Boolean = false,
)

/** Active accounts, expense categories (REQ-REC-11) and the reminder lead days for the help text. */
data class RecurringChoices(
    val accounts: List<AccountBalance> = emptyList(),
    val categories: List<Category> = emptyList(),
    val leadDays: Int = 5,
)

/** Nuevo gasto recurrente and its edit; the route's id decides which. */
@HiltViewModel
class RecurringFormViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        accounts: AccountRepository,
        categories: CategoryRepository,
        settings: SettingsRepository,
        private val recurring: RecurringRepository,
        private val transactions: TransactionRepository,
        private val create: CreateRecurring,
        private val update: UpdateRecurring,
        private val endRecurrence: EndRecurrence,
        clock: Clock,
    ) : ViewModel() {
        private val _state =
            MutableStateFlow(
                RecurringFormState(editingId = savedState.get<String>("id")?.let(UUID::fromString), startDate = clock.today()),
            )
        val state: StateFlow<RecurringFormState> = _state.asStateFlow()

        val choices: StateFlow<RecurringChoices> =
            combine(accounts.observeBalances(), categories.observeAll(), settings.observe()) { accs, cats, s ->
                RecurringChoices(
                    accs.filter { it.account.active && it.account.type.isPickable },
                    cats.filter {
                        it.kind ==
                            CategoryKind.EXPENSE
                    },
                    s.reminderLeadDays,
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, RecurringChoices())

        init {
            viewModelScope.launch {
                val id = _state.value.editingId
                if (id != null) {
                    recurring.observe(id).first()?.let { r ->
                        _state.update {
                            it.copy(
                                name = r.name,
                                amountDigits = r.estimatedAmount.wholePesos(),
                                accountId = r.accountId,
                                categoryId = r.categoryId,
                                startDate = r.startDate,
                                frequency = r.frequency,
                                intervalDigits = r.intervalCount?.toString() ?: it.intervalDigits,
                                intervalUnit = r.intervalUnit ?: it.intervalUnit,
                            )
                        }
                    }
                } else {
                    // Same default as a new movement: the last account used, else the first active one
                    val account =
                        transactions.lastUsedAccountId() ?: accounts
                            .observeActive()
                            .first()
                            .firstOrNull { it.type.isPickable }
                            ?.id
                    _state.update { it.copy(accountId = account) }
                }
            }
        }

        private fun edit(transform: (RecurringFormState) -> RecurringFormState) = _state.update { transform(it).copy(errors = emptyMap()) }

        fun setName(value: String) = edit { it.copy(name = value) }

        fun setAmount(text: String) = edit { it.copy(amountDigits = digitsOnly(text)) }

        fun setAccount(id: UUID) = edit { it.copy(accountId = id) }

        fun setCategory(id: UUID) = edit { it.copy(categoryId = id) }

        fun setStartDate(value: LocalDate) = edit { it.copy(startDate = value) }

        fun setFrequency(value: Frequency) = edit { it.copy(frequency = value) }

        fun setInterval(text: String) = edit { it.copy(intervalDigits = text.filter(Char::isDigit).take(3)) }

        fun setIntervalUnit(value: IntervalUnit) = edit { it.copy(intervalUnit = value) }

        /** S15-UI09: «Finalizar recurrencia» from the edit too; the confirmation is asked by the screen. */
        fun end() {
            val id = _state.value.editingId ?: return
            viewModelScope.launch {
                endRecurrence(id)
                _state.update { it.copy(saved = true) }
            }
        }

        fun save() {
            val s = _state.value
            val custom = s.frequency == Frequency.CUSTOM
            val r =
                Recurring(
                    id = s.editingId ?: UUID.randomUUID(),
                    name = s.name,
                    estimatedAmount = Money.ofPesos(s.amountDigits.ifEmpty { "0" }.toLong()),
                    accountId = s.accountId ?: UUID(0, 0),
                    categoryId = s.categoryId ?: UUID(0, 0),
                    startDate = s.startDate,
                    frequency = s.frequency,
                    // The use case sets the real one
                    nextDueDate = s.startDate,
                    intervalCount = s.intervalDigits.toIntOrNull().takeIf { custom },
                    intervalUnit = s.intervalUnit.takeIf { custom },
                )
            viewModelScope.launch {
                val errors = if (s.editingId == null) create(r) else update(r)
                _state.update { if (errors.isEmpty()) it.copy(saved = true) else it.copy(errors = errors) }
            }
        }
    }
