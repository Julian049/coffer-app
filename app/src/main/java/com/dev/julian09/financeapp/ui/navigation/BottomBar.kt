package com.dev.julian09.financeapp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme

enum class Tab(
    val label: String,
    val icon: ImageVector,
    val route: Any,
) {
    Home("Inicio", Icons.Outlined.Home, HomeRoute),
    History("Historial", Icons.AutoMirrored.Outlined.FormatListBulleted, HistoryRoute),
    Reports("Reportes", Icons.Outlined.BarChart, ReportsRoute),
    Accounts("Cuentas", Icons.Outlined.AccountBalanceWallet, AccountsRoute),
}

/** Prototype component "Barra de navegación": 64 dp, top border, 56×28 caramel pill on the active tab. */
@Composable
fun BottomBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxWidth().background(colors.background).navigationBarsPadding()) {
        HorizontalDivider(thickness = 1.dp, color = colors.border)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tab ->
                val active = tab == selected
                val tint = if (active) colors.primary else colors.textSecondary
                // The whole cell takes the tap, the press shows on the pill like the active state (#130)
                val interaction = remember { MutableInteractionSource() }
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .selectable(
                                selected = active,
                                interactionSource = interaction,
                                indication = null,
                                role = Role.Tab,
                                onClick = { onSelect(tab) },
                            ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        Modifier
                            .size(width = 56.dp, height = 28.dp)
                            .clip(CofferShapes.pill)
                            .background(if (active) colors.primaryContainer else Color.Transparent)
                            .indication(interaction, ripple()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                    }
                    Text(tab.label, style = MaterialTheme.typography.labelMedium, color = tint)
                }
            }
        }
    }
}
