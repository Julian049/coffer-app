package com.dev.julian09.financeapp.ui.feature.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
        if (days == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.semantics { contentDescription = "Cargando historial" }, color = colors.primary)
            }
            return@Column
        }
        // One item per row: a day can hold thousands of entries after an import (#127)
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            item {
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
            }
            if (days.isEmpty()) {
                item {
                    EmptyState(
                        title = "Sin cambios",
                        message = "Aquí verás lo que creas, editas o eliminas.",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            days.forEach { day ->
                item(key = day.date.toString()) {
                    Text(
                        dayLabel(day.date, today),
                        Modifier.padding(vertical = 12.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                    )
                }
                itemsIndexed(day.rows, key = { _, row -> row.id }) { i, row ->
                    val first = i == 0
                    val last = i == day.rows.lastIndex
                    ChangeRowItem(
                        row,
                        onUndo = { onUndo(row.id) },
                        modifier =
                            Modifier
                                .cardSlice(first, last, colors.surface, colors.border)
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = if (first) 16.dp else 12.dp,
                                    bottom = if (last) 16.dp else 12.dp,
                                ),
                    )
                }
            }
        }
    }
}

/** One row's slice of an [AppCard] (12 dp corners, 1 dp border, inset dividers), so a lazy list can draw a card row by row. */
private fun Modifier.cardSlice(
    first: Boolean,
    last: Boolean,
    fill: Color,
    line: Color,
) = clipToBounds().drawBehind {
    val radius = 12.dp.toPx()
    val stroke = 1.dp.toPx()
    // Corners that belong to another slice are pushed outside the clip
    val top = if (first) 0f else -2 * radius
    val bottom = if (last) size.height else size.height + 2 * radius
    drawRoundRect(fill, Offset(0f, top), Size(size.width, bottom - top), CornerRadius(radius))
    drawRoundRect(
        line,
        Offset(stroke / 2, top + stroke / 2),
        Size(size.width - stroke, bottom - top - stroke),
        CornerRadius(radius),
        style = Stroke(stroke),
    )
    if (!first) drawLine(line, Offset(16.dp.toPx(), stroke / 2), Offset(size.width - 16.dp.toPx(), stroke / 2), stroke)
}

@Composable
private fun ChangeRowItem(
    row: ChangeRow,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
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
        modifier.fillMaxWidth().padding(vertical = 4.dp),
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
