package com.dev.julian09.financeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/** 64 dp screen header with a 1 dp bottom border: optional back button, title and trailing actions. */
@Composable
fun AppHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    /** The period of a report, under the title (changes-ui-2.0 §3). */
    subtitle: String? = null,
    /** Beside the title: a tag's colour dot in Detalle de etiqueta (changes-ui-2.0 §2). */
    titleTrailing: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxWidth().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(start = if (onBack == null) 16.dp else 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary)
                }
                Spacer(Modifier.size(4.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    titleTrailing?.invoke()
                }
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        HorizontalDivider(thickness = 1.dp, color = colors.border)
    }
}

/** Avatar in `primary`; opens Settings. Shows the initial once there is a session (Sprint 2), a person icon until then. */
@Composable
fun Avatar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    initial: Char? = null,
    size: Int = 32,
) {
    val colors = CofferTheme.colors
    Box(
        modifier =
            modifier
                .size(48.dp) // touch target
                .clip(CircleShape)
                .clickable(onClickLabel = "Abrir ajustes", role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size.dp).background(colors.primary, CircleShape), contentAlignment = Alignment.Center) {
            if (initial != null) {
                Text(initial.uppercase(), style = MaterialTheme.typography.titleSmall, color = colors.onPrimary)
            } else {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = "Ajustes",
                    tint = colors.onPrimary,
                    modifier = Modifier.size((size * 0.55f).dp),
                )
            }
        }
    }
}
