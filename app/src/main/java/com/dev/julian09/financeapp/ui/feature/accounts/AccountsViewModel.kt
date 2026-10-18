package com.dev.julian09.financeapp.ui.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountImpact
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.RemoveMode
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.usecase.ReactivateAccount
import com.dev.julian09.financeapp.domain.usecase.RemoveAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** The sheets of SEQ-12: options, the three ways to remove, and the reinforced confirmation (REQ-CTA-05). */
sealed interface AccountSheet {
    val account: AccountBalance

    data class Options(
        override val account: AccountBalance,
    ) : AccountSheet

    data class Remove(
        override val account: AccountBalance,
        val impact: AccountImpact,
        val mode: RemoveMode = RemoveMode.DEACTIVATE,
        val targetId: UUID? = null,
    ) : AccountSheet

    data class Confirm(
        override val account: AccountBalance,
        val impact: AccountImpact,
        val typed: String = "",
        val error: LedgerError? = null,
    ) : AccountSheet {
        /** REQ-CTA-05: enabled only when the typed name matches, ignoring case. */
        val canDelete: Boolean get() = typed.trim().equals(account.account.name.trim(), ignoreCase = true)
    }
}

@HiltViewModel
class AccountsViewModel
    @Inject
    constructor(
        private val repository: AccountRepository,
        private val removeAccount: RemoveAccount,
        private val reactivateAccount: ReactivateAccount,
    ) : ViewModel() {
        /** Every not deleted account, active or not. */
        val accounts: StateFlow<List<AccountBalance>?> =
            repository.observeBalances().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private val _sheet = MutableStateFlow<AccountSheet?>(null)
        val sheet: StateFlow<AccountSheet?> = _sheet.asStateFlow()

        fun openOptions(account: AccountBalance) {
            _sheet.value = AccountSheet.Options(account)
        }

        fun dismiss() {
            _sheet.value = null
        }

        fun startRemove() {
            val account = _sheet.value?.account ?: return
            viewModelScope.launch { _sheet.value = AccountSheet.Remove(account, repository.impact(account.account.id)) }
        }

        fun selectMode(mode: RemoveMode) = _sheet.update { (it as? AccountSheet.Remove)?.copy(mode = mode) ?: it }

        fun selectTarget(id: UUID) = _sheet.update { (it as? AccountSheet.Remove)?.copy(targetId = id) ?: it }

        fun typeConfirmation(text: String) = _sheet.update { (it as? AccountSheet.Confirm)?.copy(typed = text, error = null) ?: it }

        /** Deactivate and move run at once; deleting everything goes through the confirmation first. */
        fun applyRemove() {
            val remove = _sheet.value as? AccountSheet.Remove ?: return
            if (remove.mode == RemoveMode.DELETE_ALL) {
                _sheet.value = AccountSheet.Confirm(remove.account, remove.impact)
                return
            }
            viewModelScope.launch {
                if (removeAccount(remove.account.account.id, remove.mode, remove.targetId) == null) _sheet.value = null
            }
        }

        fun confirmDelete() {
            val confirm = _sheet.value as? AccountSheet.Confirm ?: return
            viewModelScope.launch {
                val error = removeAccount(confirm.account.account.id, RemoveMode.DELETE_ALL, confirmation = confirm.typed)
                _sheet.value = if (error == null) null else confirm.copy(error = error)
            }
        }

        fun reactivate(id: UUID) {
            viewModelScope.launch { reactivateAccount(id) }
        }
    }
