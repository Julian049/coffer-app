package com.dev.julian09.financeapp.ui.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.port.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Read-only list of accounts (S3-UI03); Sprint 4 brings balances and editing. */
@HiltViewModel
class AccountsViewModel
    @Inject
    constructor(
        accounts: AccountRepository,
    ) : ViewModel() {
        val accounts: StateFlow<List<Account>?> =
            accounts.observeActive().stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                null,
            )
    }
