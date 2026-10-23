package com.dev.julian09.financeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/** Surface with a 1 dp border and no shadow. `highlight` is the featured card (main balance). */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CofferTheme.colors
    val shape = if (highlight) CofferShapes.highlight else CofferShapes.card
    val color = if (highlight) colors.surfaceHighlight else colors.surface
    // A chosen card carries a 1.5 dp border in the selection color (design-system §selection)
    val border =
        if (selected) {
            BorderStroke(1.5.dp, colors.onPrimaryContainer)
        } else {
            BorderStroke(1.dp, if (highlight) colors.borderHighlight else colors.border)
        }
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(if (highlight) 20.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (highlight) 4.dp else 12.dp),
            content = content,
        )
    }
    if (onClick == null) {
        Surface(modifier = modifier, shape = shape, color = color, border = border, content = body)
    } else {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = border, content = body)
    }
}
