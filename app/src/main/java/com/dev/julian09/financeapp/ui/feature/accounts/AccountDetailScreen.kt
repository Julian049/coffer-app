package com.dev.julian09.financeapp.ui.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.accountTypeLabel
import com.dev.julian09.financeapp.ui.feature.cards.CardDetailSections
import com.dev.julian09.financeapp.ui.feature.cards.PayCardSheet
import com.dev.julian09.financeapp.ui.feature.history.DayGroup
import com.dev.julian09.financeapp.ui.feature.loans.LoanDetailSections
import com.dev.julian09.financeapp.ui.feature.loans.LoanPaymentSheet
import com.dev.julian09.financeapp.ui.feature.loans.LoanSheet
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.LocalDate
import java.util.UUID

@Composable
fun AccountDetailScreen(
    onBack: () -> Unit,
    onEdit: (UUID) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTransaction: (UUID) -> Unit,
    onConvertLoan: (UUID) -> Unit = {},
    viewModel: AccountDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var paying by rememberSaveable { mutableStateOf<String?>(null) }
    var loanSheet by rememberSaveable { mutableStateOf<LoanSheet?>(null) }
    PayCardSheet(paying?.let(UUID::fromString), onDismiss = { paying = null })
    LoanPaymentSheet(
        state
            ?.loan
            ?.loan
            ?.accountId
            ?.takeIf { loanSheet != null },
        loanSheet ?: LoanSheet.PAY,
        onDismiss = { loanSheet = null },
    )
    AccountDetailContent(
        state,
        onBack,
        onEdit,
        onOpenHistory,
        onOpenTransaction = onOpenTransaction,
        onPayCard = { paying = it.toString() },
        onLoan = { loanSheet = it },
        onConvertLoan = onConvertLoan,
    )
}

/** Detalle de cuenta: balance, how it is calculated (REQ-CTA-06) and its movements. */
@Composable
fun AccountDetailContent(
    state: AccountDetailState?,
    onBack: () -> Unit,
    onEdit: (UUID) -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    onOpenTransaction: (UUID) -> Unit = {},
    onPayCard: (UUID) -> Unit = {},
    onLoan: (LoanSheet) -> Unit = {},
    onConvertLoan: (UUID) -> Unit = {},
) {
    val colors = CofferTheme.colors
    val account = state?.account
    Column(modifier.fillMaxSize()) {
        AppHeader(
            title = account?.account?.name ?: "",
            onBack = onBack,
            actions = {
                if (account != null) {
                    IconButton(onClick = { onEdit(account.account.id) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar cuenta", tint = colors.textPrimary)
                    }
                }
            },
        )
        if (account == null) return@Column
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val card = state.card
            val loan = state.loan
            when {
                card != null -> CardDetailSections(card, onPay = { onPayCard(card.card.accountId) })
                loan != null ->
                    LoanDetailSections(
                        loan,
                        onPay = { onLoan(LoanSheet.PAY) },
                        onExtra = { onLoan(LoanSheet.EXTRA) },
                        onConvert = { onConvertLoan(loan.loan.accountId) },
                    )
                else -> AccountSummary(account)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Movimientos", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                TextButton(onClick = onOpenHistory) { Text("Ver en historial", color = colors.primary) }
            }
            if (state.days.isEmpty()) {
                Text("Esta cuenta aún no tiene movimientos.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
            state.days.forEach { DayGroup(it, today, onOpen = onOpenTransaction) }
        }
    }
}

/** Saldo and how it is calculated (REQ-CTA-06), for every account but cards. */
@Composable
private fun AccountSummary(account: AccountBalance) {
    val colors = CofferTheme.colors
    AppCard(Modifier.fillMaxWidth(), highlight = true) {
        Text("Saldo", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        MoneyText(account.balance, style = MaterialTheme.typography.displaySmall)
        Text(
            accountTypeLabel(account.account.type) +
                if (account.account.includeInTotal) " · incluida en el saldo total" else " · fuera del saldo total",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
    AppCard {
        Text("Cómo se calcula", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        val flows = account.flows
        Line("Saldo inicial", account.account.initialBalance, MoneyKind.Neutral)
        Line("Ingresos", flows.income, MoneyKind.Income)
        Line("Gastos", flows.expense, MoneyKind.Expense)
        Line("Transferencias", flows.transfersIn - flows.transfersOut, MoneyKind.Neutral)
    }
}

@Composable
private fun Line(
    label: String,
    amount: Money,
    kind: MoneyKind,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = CofferTheme.colors.textSecondary)
        MoneyText(amount, kind = if (amount == Money.ZERO) MoneyKind.Neutral else kind, style = MaterialTheme.typography.bodyMedium)
    }
}
