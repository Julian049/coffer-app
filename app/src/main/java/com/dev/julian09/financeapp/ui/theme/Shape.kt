package com.dev.julian09.financeapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radii of design-system.md §4. */
object CofferShapes {
    val card = RoundedCornerShape(12.dp)
    val highlight = RoundedCornerShape(16.dp)
    val fab = RoundedCornerShape(14.dp)
    val sheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val dialog = RoundedCornerShape(20.dp)
    val pill = RoundedCornerShape(percent = 50)
}

val CofferMaterialShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = CofferShapes.card,
        medium = CofferShapes.card,
        large = CofferShapes.highlight,
        extraLarge = CofferShapes.dialog,
    )
