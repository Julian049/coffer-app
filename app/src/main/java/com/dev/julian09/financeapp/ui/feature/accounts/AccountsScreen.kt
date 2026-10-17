package com.dev.julian09.financeapp.ui.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.EmptyState
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.theme.CofferTheme

@Composable
fun AccountsScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    avatarInitial: Char? = null,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    AccountsContent(accounts, onOpenSettings, modifier, avatarInitial)
}

@Composable
fun AccountsContent(
    accounts: List<Account>?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    avatarInitial: Char? = null,
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Cuentas", actions = { Avatar(onClick = onOpenSettings, initial = avatarInitial) })
        when {
            accounts == null -> Unit
            accounts.isEmpty() ->
                EmptyState(
                    title = "Aún no tienes cuentas",
                    message = "Aquí vas a crear y administrar tus cuentas, tarjetas y préstamos.",
                )
            else ->
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts, key = { it.id }) { account ->
                        AppCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(account.name, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                                    Text(
                                        listOfNotNull(typeLabel(account.type), account.institution).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary,
                                    )
                                }
                                // Balances need the domain calculation of Sprint 4; until then, the initial balance
                                Column(horizontalAlignment = Alignment.End) {
                                    MoneyText(account.initialBalance)
                                    Text("Saldo inicial", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }
        }
    }
}

private fun typeLabel(type: AccountType) =
    when (type) {
        AccountType.CASH -> "Efectivo"
        AccountType.DEBIT -> "Débito"
        AccountType.SAVINGS -> "Ahorros"
        AccountType.CREDIT_CARD -> "Tarjeta de crédito"
        AccountType.LOAN -> "Préstamo"
    }
