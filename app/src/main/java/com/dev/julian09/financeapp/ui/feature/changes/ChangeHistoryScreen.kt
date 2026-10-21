package com.dev.julian09.financeapp.ui.feature.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.ChangeAction
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.ConfirmDialog
import com.dev.julian09.financeapp.ui.components.EmptyState
import com.dev.julian09.financeapp.ui.feature.history.dayLabel
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.LocalDate
import java.util.UUID

@Composable
fun ChangeHistoryScreen(
    onBack: () -> Unit,
    viewModel: ChangeHistoryViewModel = hiltViewModel(),
) {
    val days by viewModel.days.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    ChangeHistoryContent(days, onBack, viewModel::undo)
    if (failed) {
        ConfirmDialog(
            title = "No se pudo deshacer",
            message = "La cuenta o la categoría de este movimiento ya no está disponible. Reactívala o edita el movimiento.",
            confirmText = "Entendido",
            onConfirm = viewModel::dismissFailure,
            onDismiss = viewModel::dismissFailure,
        )
    }
}

/** Historial de cambios (S11-UI01): grouped by day, with Restaurar and Revertir on the latest change of a movement. */
@Composable
fun ChangeHistoryContent(
    days: List<ChangeDay>?,
    onBack: () -> Unit,
    onUndo: (UUID) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Historial de cambios", onBack = onBack)
        if (days == null) return@Column
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    Text(
                        "Se guardan los cambios de tus transacciones durante 90 días.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }
            if (days.isEmpty()) {
                EmptyState(title = "Sin cambios", message = "Aquí verás lo que creas, editas o eliminas.")
            }
            days.forEach { day ->
                Text(dayLabel(day.date, today), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                AppCard {
                    day.rows.forEachIndexed { i, row ->
                        if (i > 0) HorizontalDivider(thickness = 1.dp, color = colors.border)
                        ChangeRowItem(row, onUndo = { onUndo(row.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeRowItem(
    row: ChangeRow,
    onUndo: () -> Unit,
) {
    val colors = CofferTheme.colors
    val (icon, tint) =
        when (row.action) {
            ChangeAction.CREATE -> Icons.Outlined.Add to colors.income
            ChangeAction.UPDATE -> Icons.Outlined.Edit to colors.iconOnContainer
            ChangeAction.DELETE -> Icons.Outlined.DeleteOutline to colors.expense
            ChangeAction.RESTORE -> Icons.Outlined.Restore to colors.iconOnContainer
        }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(36.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(row.detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        when {
            row.canRestore -> UndoButton("Restaurar", Icons.Outlined.History, onUndo)
            row.canRevert -> UndoButton("Revertir", Icons.Outlined.History, onUndo)
        }
    }
}

@Composable
private fun UndoButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val colors = CofferTheme.colors
    Row(
        Modifier
            .clip(CofferShapes.pill)
            .border(1.dp, colors.border, CofferShapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
    }
}
