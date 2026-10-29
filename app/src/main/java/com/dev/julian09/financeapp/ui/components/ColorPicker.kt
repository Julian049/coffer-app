package com.dev.julian09.financeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.domain.model.TagColors
import com.dev.julian09.financeapp.ui.feature.history.tagColor
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/** REQ-ETQ-05, REQ-CAT-05: six 32 dp circles, the picked one with ✓ (changes-ui-2.0 §2); [selected] null = none picked. */
@Composable
fun ColorPicker(
    selected: String?,
    onPick: (String) -> Unit,
) {
    val colors = CofferTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Color", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TagColors.entries.forEach { c ->
                val picked = c.hex == selected
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tagColor(c.hex))
                            .selectable(picked, role = Role.RadioButton, onClick = { onPick(c.hex) })
                            .semantics { contentDescription = c.label },
                    contentAlignment = Alignment.Center,
                ) {
                    if (picked) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
