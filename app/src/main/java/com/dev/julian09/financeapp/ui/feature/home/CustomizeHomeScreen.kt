package com.dev.julian09.financeapp.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.HomeSection
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.ReorderableColumn
import com.dev.julian09.financeapp.ui.components.accountTypeShortLabel
import com.dev.julian09.financeapp.ui.theme.CofferTheme

@Composable
fun CustomizeHomeScreen(
    onBack: () -> Unit,
    viewModel: CustomizeHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CustomizeHomeContent(
        state,
        onBack,
        viewModel::moveCard,
        viewModel::setVisible,
        viewModel::moveAccount,
    )
}

private fun sectionTexts(key: String): Pair<String, String> =
    when (runCatching { HomeSection.valueOf(key) }.getOrNull()) {
        HomeSection.BALANCE -> "Saldo total" to "Disponible o neto"
        HomeSection.QUICK_ACTIONS -> "Accesos rápidos" to "Gasto, ingreso y transferencia"
        HomeSection.ACCOUNTS -> "Cuentas" to "Carrusel con el saldo de cada cuenta"
        HomeSection.PENDING -> "Próximos pagos" to "Recurrentes, tarjetas y préstamos"
        HomeSection.PINNED_TAGS -> "Etiquetas fijadas" to "Total del mes de cada etiqueta"
        HomeSection.RECENT -> "Movimientos recientes" to "Últimos 5 días agrupados por día"
        HomeSection.MONTHLY_TREND -> "Indicador mensual" to "Variación frente al mes anterior"
        null -> key to ""
    }

/** Personalizar inicio (S11-UI04, REQ-INI-04) and the order of the accounts (M9, #92). */
@Composable
fun CustomizeHomeContent(
    state: CustomizeHomeState,
    onBack: () -> Unit,
    onMoveCard: (Int, Int) -> Unit,
    onVisible: (String, Boolean) -> Unit,
    onMoveAccount: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Personalizar inicio", onBack = onBack)
        if (!state.loaded) return@Column
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Muestra u oculta las tarjetas de inicio. Mantén pulsado el asa para cambiar el orden. Los cambios se guardan solos.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            AppCard {
                ReorderableColumn(state.cards, key = { it.key }, onMove = onMoveCard) { card, handle ->
                    val (title, detail) = sectionTexts(card.key)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        handle()
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                        Switch(
                            checked = card.visible,
                            onCheckedChange = { onVisible(card.key, it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = colors.primary, checkedThumbColor = colors.onPrimary),
                        )
                    }
                }
            }
            if (state.accounts.size > 1) {
                Text("Orden de las cuentas", style = MaterialTheme.typography.titleSmall, color = colors.textSecondary)
                AppCard {
                    ReorderableColumn(state.accounts, key = { it.id }, onMove = onMoveAccount) { account, handle ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            handle()
                            Column(Modifier.weight(1f)) {
                                Text(account.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Text(
                                    accountTypeShortLabel(account.type),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
