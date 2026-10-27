package com.dev.julian09.financeapp.ui.feature.mail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.theme.CofferTheme

@Composable
fun ConnectEmailScreen(
    onBack: () -> Unit,
    viewModel: ConnectEmailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    @Suppress("DEPRECATION") // LocalClipboard is suspend-only; a copy button does not need it
    val clipboard = LocalClipboardManager.current
    ConnectEmailContent(
        state,
        onBack = onBack,
        onCopy = { clipboard.setText(AnnotatedString(it)) },
        onRefresh = viewModel::refresh,
        onNotify = viewModel::setNotifyDrafts,
    )
}

/** REQ-COR-01: the address, how to forward the bank emails to it, and the notice switch. Figma's screen comes in part 2. */
@Composable
fun ConnectEmailContent(
    state: ConnectEmailState,
    onBack: () -> Unit = {},
    onCopy: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onNotify: (Boolean) -> Unit = {},
) {
    val colors = CofferTheme.colors
    Column(Modifier.fillMaxSize()) {
        AppHeader(title = "Conectar correo", onBack = onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Reenvía a Coffer los comprobantes de Bancolombia y RappiCard. Cada uno aparece en Historial, en «Por revisar», " +
                    "y solo cuenta en tu saldo cuando lo apliques.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            AppCard {
                Text("Tu dirección de reenvío", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                Text(
                    state.address ?: if (state.loading) "Cargando…" else "Sin conexión. Vuelve a intentarlo.",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                state.address?.let { address ->
                    AppButton(
                        "Copiar dirección",
                        onClick = { onCopy(address) },
                        kind = ButtonKind.Secondary,
                        icon = Icons.Outlined.ContentCopy,
                    )
                }
            }
            AppCard {
                Text("Cómo conectarlo en Gmail", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                STEPS.forEachIndexed { i, step ->
                    Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
                state.confirmation?.let {
                    Text("Código de confirmación de Gmail: $it", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    AppButton("Copiar código", onClick = { onCopy(it) }, kind = ButtonKind.Secondary, icon = Icons.Outlined.ContentCopy)
                }
                AppButton("Buscar el código de nuevo", onClick = onRefresh, kind = ButtonKind.Ghost, icon = Icons.Outlined.Refresh)
            }
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Avisar movimientos por revisar", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text(
                            "Una sola notificación con cuántos llegaron",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Switch(
                        checked = state.notifyDrafts,
                        onCheckedChange = onNotify,
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.primary, checkedThumbColor = colors.onPrimary),
                    )
                }
            }
            Text(
                "Para dejar de enviarlos, borra el filtro en Gmail. Coffer nunca entra a tu bandeja: solo ve lo que reenvías.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

private val STEPS =
    listOf(
        "En Gmail (en el computador), abre Configuración → Ver todos los ajustes → Reenvío y correo POP/IMAP.",
        "Toca «Añadir una dirección de reenvío» y pega tu dirección de Coffer.",
        "Gmail envía un código a Coffer. Vuelve aquí, toca «Buscar el código de nuevo» y pégalo en Gmail.",
        "Abre un comprobante de Bancolombia, toca ⋮ → «Filtrar mensajes como este», elige «Reenviar a» tu dirección de Coffer " +
            "y crea el filtro. Repite con uno de RappiCard.",
    )
