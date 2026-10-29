package com.dev.julian09.financeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

/** The big palette of «Elegir color»: the six of [TagColors] first (they are the ones shown), then more (REQ-ETQ-05, REQ-CAT-05). */
val COLOR_CHOICES: List<Pair<String, String>> =
    TagColors.entries.map { it.hex to it.label } +
        listOf(
            "#1E9C7E" to "Esmeralda",
            "#C2662D" to "Terracota",
            "#2F74C0" to "Azul",
            "#D2638F" to "Rosa",
            "#D9A21B" to "Mostaza",
            "#7B5FC0" to "Violeta",
            "#B5543F" to "Ladrillo",
            "#6E8B3D" to "Oliva",
            "#4F5FB0" to "Índigo",
            "#A86A2E" to "Cobre",
            "#8E4F9E" to "Orquídea",
            "#3D8FA8" to "Turquesa",
            "#C0506A" to "Frambuesa",
            "#5B7F5E" to "Salvia",
            "#9C5F33" to "Caramelo",
            "#4A3328" to "Espresso",
            "#6B7280" to "Grafito",
            "#0F766E" to "Pino",
            "#B45309" to "Ámbar",
            "#BE185D" to "Fucsia",
            "#1D4ED8" to "Cobalto",
            "#7C3AED" to "Amatista",
        )

private const val SHOWN = 5

/**
 * Color row: at most six circles, the last one opens [ColorPickerSheet] with the whole palette. A color picked there takes
 * the first place of the row (the same pattern as [IconRow]); [selected] null = none picked.
 */
@Composable
fun ColorPicker(
    selected: String?,
    onPick: (String) -> Unit,
) {
    val colors = CofferTheme.colors
    var picking by rememberSaveable { mutableStateOf(false) }
    val shown = (listOfNotNull(selected) + COLOR_CHOICES.map { it.first }).distinct().take(SHOWN)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Color", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            shown.forEach { hex -> ColorDot(hex, selected = hex == selected) { onPick(hex) } }
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .selectable(false, role = Role.Button, onClick = { picking = true })
                    .semantics { contentDescription = "Más colores" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.GridView, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
    if (picking) {
        ColorPickerSheet(
            selected,
            onPick = {
                onPick(it)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

/** Hoja · Elegir color: the whole palette; a tap picks and closes. */
@Composable
fun ColorPickerSheet(
    selected: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss, title = "Elegir color") {
        BubbleGrid(COLOR_CHOICES.map { it.first }, columns = 5) { hex -> ColorDot(hex, selected = hex == selected) { onPick(hex) } }
    }
}

@Composable
private fun ColorDot(
    hex: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = COLOR_CHOICES.firstOrNull { it.first == hex }?.second ?: hex
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tagColor(hex))
                .selectable(selected, role = Role.RadioButton, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}
