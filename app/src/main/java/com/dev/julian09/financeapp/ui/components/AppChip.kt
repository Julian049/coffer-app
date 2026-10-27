package com.dev.julian09.financeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/**
 * Pill filter. Selected: caramel container without border; otherwise surface with border. [dot] is a tag's colour
 * before the label (INC-10).
 */
@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dot: Color? = null,
) {
    val colors = CofferTheme.colors
    Surface(
        // INC-10: clipped first, so the press highlight stays inside the pill
        modifier = modifier.clip(CofferShapes.pill).selectable(selected = selected, onClick = onClick, role = Role.Checkbox),
        shape = CofferShapes.pill,
        color = if (selected) colors.primaryContainer else colors.surface,
        border = if (selected) null else BorderStroke(1.dp, colors.border),
    ) {
        Row(
            Modifier.padding(start = if (dot != null) 10.dp else 14.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            dot?.let { Box(Modifier.size(8.dp).background(it, CircleShape)) }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) colors.onPrimaryContainer else colors.textSecondary,
            )
        }
    }
}
