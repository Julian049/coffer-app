package com.dev.julian09.financeapp.ui.feature.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.usecase.CreateAccount
import com.dev.julian09.financeapp.domain.usecase.SetLastDigits
import com.dev.julian09.financeapp.domain.usecase.UpdateAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AccountFormState(
    /** Null for Nueva cuenta. */
    val editingId: UUID? = null,
    val name: String = "",
    /** Whole pesos as typed, digits only. */
    val balanceDigits: String = "",
    val type: AccountType = AccountType.CASH,
    val icon: String = "wallet",
    val color: String = "#4A3328",
    val includeInTotal: Boolean = true,
    /** REQ-COR-06: up to 4 digits as typed. */
    val lastDigits: String = "",
    val error: LedgerError? = null,
    val saved: Boolean = false,
)

/** Pesos without decimals (amountDecimals = 0); at most 13 digits so the ×10⁴ Long never overflows. */
fun digitsOnly(text: String): String = text.filter(Char::isDigit).trimStart('0').take(13)

fun Money.wholePesos(): String =
    toBigDecimal()
        .toBigInteger()
        .toString()
        .let { if (it == "0") "" else it }

/** Nueva cuenta and Editar cuenta (S4-UI01); the route's id decides which one. */
@HiltViewModel
class AccountFormViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        private val repository: AccountRepository,
        private val createAccount: CreateAccount,
        private val updateAccount: UpdateAccount,
        private val setLastDigits: SetLastDigits,
    ) : ViewModel() {
        private val _state = MutableStateFlow(AccountFormState(editingId = savedState.get<String>("id")?.let(UUID::fromString)))
        val state: StateFlow<AccountFormState> = _state.asStateFlow()

        init {
            _state.value.editingId?.let { id ->
                viewModelScope.launch {
                    val account =
                        repository
                            .observeBalances()
                            .first()
                            .first { it.account.id == id }
                            .account
                    _state.update {
                        it.copy(
                            name = account.name,
                            balanceDigits = account.initialBalance.wholePesos(),
                            type = account.type,
                            icon = account.icon ?: it.icon,
                            color = account.color ?: it.color,
                            includeInTotal = account.includeInTotal,
                            lastDigits = account.lastDigits.orEmpty(),
                        )
                    }
                }
            }
        }

        fun setName(value: String) = _state.update { it.copy(name = value, error = null) }

        fun setBalance(value: String) = _state.update { it.copy(balanceDigits = digitsOnly(value)) }

        fun setType(value: AccountType) = _state.update { it.copy(type = value, error = null) }

        fun setIcon(value: String) = _state.update { it.copy(icon = value) }

        fun setColor(value: String) = _state.update { it.copy(color = value) }

        fun setIncludeInTotal(value: Boolean) = _state.update { it.copy(includeInTotal = value) }

        fun setLastDigits(value: String) = _state.update { it.copy(lastDigits = value.filter(Char::isDigit).take(4)) }

        fun save() {
            val s = _state.value
            val balance = Money.ofPesos(s.balanceDigits.ifEmpty { "0" }.toLong())
            viewModelScope.launch {
                val id = s.editingId ?: UUID.randomUUID()
                val error =
                    if (s.editingId == null) {
                        createAccount(Account(id, s.name, s.type, balance, s.includeInTotal, icon = s.icon, color = s.color))
                    } else {
                        updateAccount(s.editingId, s.name, s.includeInTotal, s.icon, s.color)
                    }
                if (error == null) setLastDigits(id, s.lastDigits)
                _state.update { if (error == null) it.copy(saved = true) else it.copy(error = error) }
            }
        }
    }
