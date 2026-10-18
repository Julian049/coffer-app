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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Composable
fun DeactivatedAccountsScreen(
    onBack: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    DeactivatedAccountsContent(accounts?.filter { !it.account.active }, onBack, viewModel::reactivate)
}

private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-CO"))

/** REQ-CTA-04: out of Home, selectors and the total, but still in reports; can come back. */
@Composable
fun DeactivatedAccountsContent(
    accounts: List<AccountBalance>?,
    onBack: () -> Unit,
    onReactivate: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Cuentas desactivadas", onBack = onBack)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.textSecondary)
                    Text(
                        "No aparecen en inicio, selectores ni saldo total, pero sus transacciones siguen en los reportes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }
            if (accounts.isNullOrEmpty()) {
                Text("No tienes cuentas desactivadas.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                return@Column
            }
            AppCard {
                accounts.forEachIndexed { i, item ->
                    if (i > 0) HorizontalDivider(thickness = 1.dp, color = colors.border)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccountIcon(item, size = 40)
                        Column(Modifier.weight(1f)) {
                            Text(item.account.name, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                            val date = item.account.deactivatedAt?.let { DATE.format(it.atZone(ZoneId.systemDefault())).replace(".", "") }
                            Text(
                                listOfNotNull(date?.let { "Desactivada el $it" }, movements(item.transactions)).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                        AppButton("Reactivar", onClick = { onReactivate(item.account.id) }, kind = ButtonKind.Secondary)
                    }
                }
            }
        }
    }
}

private fun movements(count: Int) = if (count == 1) "1 movimiento" else "$count movimientos"
