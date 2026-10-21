package com.dev.julian09.financeapp.ui.feature.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.CardOverview
import com.dev.julian09.financeapp.domain.model.LoanOverview
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import com.dev.julian09.financeapp.domain.usecase.ObserveCards
import com.dev.julian09.financeapp.domain.usecase.ObserveLoans
import com.dev.julian09.financeapp.ui.feature.history.HistoryDay
import com.dev.julian09.financeapp.ui.feature.history.buildDays
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject

data class AccountDetailState(
    val account: AccountBalance,
    val days: List<HistoryDay>,
    /** Only for a credit card (S7-UI01). */
    val card: CardOverview? = null,
    /** Only for a loan (S10-UI01). */
    val loan: LoanOverview? = null,
)

@HiltViewModel
class AccountDetailViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        accounts: AccountRepository,
        transactions: TransactionRepository,
        categories: CategoryRepository,
        cards: ObserveCards,
        loans: ObserveLoans,
    ) : ViewModel() {
        private val id: UUID = UUID.fromString(savedState.get<String>("id"))

        /** Null while loading or once the account is gone. */
        val state: StateFlow<AccountDetailState?> =
            combine(
                accounts.observeBalances(),
                transactions.observeByAccount(id),
                categories.observeAll(),
                cards(),
                loans(),
            ) { accs, txs, cats, overviews, loanOverviews ->
                accs.firstOrNull { it.account.id == id }?.let { account ->
                    AccountDetailState(
                        account,
                        buildDays(txs, accs.associate { it.account.id to it.account.name }, cats),
                        overviews.firstOrNull { it.card.accountId == id },
                        loanOverviews.firstOrNull { it.loan.accountId == id },
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }
