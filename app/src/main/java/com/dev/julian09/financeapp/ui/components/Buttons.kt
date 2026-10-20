package com.dev.julian09.financeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/** Bottom space for the lists under the + of Home and History: its 56 dp plus 16 dp around, so the last row shows (E7, #81). */
val FabClearance = 88.dp

/**
 * One family of actions (design-system.md §2): `Primary` (espresso / latte), `Secondary` (surface with border),
 * `Ghost` (text only) and `Danger` (destructive, outlined in the expense color).
 */
enum class ButtonKind { Primary, Secondary, Ghost, Danger }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = CofferTheme.colors
    val (container, content) =
        when (kind) {
            ButtonKind.Primary -> colors.primary to colors.onPrimary
            ButtonKind.Secondary -> colors.surface to colors.textPrimary
            ButtonKind.Ghost -> Color.Transparent to colors.primary
            ButtonKind.Danger -> colors.surface to colors.danger
        }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CofferShapes.card,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = content,
                disabledContainerColor = container,
                disabledContentColor = colors.textSecondary.copy(alpha = 0.5f),
            ),
        border =
            when (kind) {
                ButtonKind.Secondary, ButtonKind.Danger -> BorderStroke(1.dp, colors.border)
                else -> null
            },
        elevation = null,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        ButtonContent(text, icon)
    }
}

@Composable
private fun RowScope.ButtonContent(
    text: String,
    icon: ImageVector?,
) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
    }
    Text(text, style = MaterialTheme.typography.titleSmall)
}
