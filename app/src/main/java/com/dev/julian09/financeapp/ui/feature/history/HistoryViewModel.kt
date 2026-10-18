package com.dev.julian09.financeapp.ui.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Basic read-only list by day (S3-UI03) to see the synced data; Sprint 5 replaces it with the full Historial. */
@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        transactions: TransactionRepository,
        accounts: AccountRepository,
        categories: CategoryRepository,
    ) : ViewModel() {
        val days: StateFlow<List<HistoryDay>?> =
            combine(transactions.observeRecent(LIMIT), accounts.observeBalances(), categories.observeAll()) { txs, accs, cats ->
                buildDays(txs, accs.associate { it.account.id to it.account.name }, cats)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private companion object {
            // shortcut: newest 500 only, no paging; Sprint 5 brings the full Historial with filters
            const val LIMIT = 500
        }
    }
