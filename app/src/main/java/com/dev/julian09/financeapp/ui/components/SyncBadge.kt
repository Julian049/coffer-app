package com.dev.julian09.financeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.domain.model.SyncStatus
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/** Compact sync indicator: synced, N pending (warning), syncing or offline. */
@Composable
fun SyncBadge(
    status: SyncStatus,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    val (icon, label, tint) =
        when (status) {
            SyncStatus.Synced -> Triple(Icons.Outlined.CloudDone, "Sincronizado", colors.income)
            is SyncStatus.Pending ->
                Triple(
                    Icons.Outlined.CloudUpload,
                    if (status.count == 1) "1 pendiente" else "${status.count} pendientes",
                    colors.warning,
                )
            SyncStatus.Syncing -> Triple(Icons.Outlined.CloudSync, "Sincronizando…", colors.textSecondary)
            SyncStatus.Offline -> Triple(Icons.Outlined.CloudOff, "Sin conexión", colors.textSecondary)
        }
    Surface(modifier = modifier, shape = CofferShapes.pill, color = colors.surface, border = BorderStroke(1.dp, colors.border)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
        }
    }
}
