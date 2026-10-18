package com.dev.julian09.financeapp.ui.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HistoryRow(
    val title: String,
    val subtitle: String,
    val amount: Money,
    val type: TransactionType,
)

data class HistoryDay(
    val date: LocalDate,
    val rows: List<HistoryRow>,
)

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
            combine(transactions.observeRecent(LIMIT), accounts.observeActive(), categories.observeAll()) { txs, accs, cats ->
                val accountName = accs.associate { it.id to it.name }
                val categoryName = cats.associate { it.id to it.name }
                val zone = ZoneId.systemDefault()
                txs
                    .groupBy { it.occurredAt.atZone(zone).toLocalDate() }
                    .map { (date, dayTxs) ->
                        HistoryDay(
                            date,
                            dayTxs.map { tx ->
                                val from = accountName[tx.accountId] ?: "Cuenta"
                                HistoryRow(
                                    title = tx.categoryId?.let(categoryName::get) ?: tx.note ?: label(tx.type),
                                    subtitle = tx.destinationAccountId?.let { "$from → ${accountName[it] ?: "Cuenta"}" } ?: from,
                                    amount = tx.amount,
                                    type = tx.type,
                                )
                            },
                        )
                    }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private fun label(type: TransactionType) =
            when (type) {
                TransactionType.EXPENSE -> "Gasto"
                TransactionType.INCOME -> "Ingreso"
                TransactionType.TRANSFER -> "Transferencia"
            }

        private companion object {
            // shortcut: newest 500 only, no paging; Sprint 5 brings the full Historial with filters
            const val LIMIT = 500
        }
    }
