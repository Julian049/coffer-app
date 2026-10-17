package com.dev.julian09.financeapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// design-system.md §5: system sans serif, only weights 400 and 500, minimum 11 sp
private fun style(
    size: Int,
    weight: FontWeight,
    lineHeight: Int,
) = TextStyle(fontFamily = FontFamily.Default, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp)

val CofferTypography =
    Typography(
        displaySmall = style(32, FontWeight.Medium, 40), // main balance
        headlineSmall = style(24, FontWeight.Medium, 32),
        titleLarge = style(18, FontWeight.Medium, 24), // screen and sheet titles
        titleMedium = style(16, FontWeight.Medium, 22),
        titleSmall = style(14, FontWeight.Medium, 20), // row titles, buttons
        bodyLarge = style(16, FontWeight.Normal, 24),
        bodyMedium = style(14, FontWeight.Normal, 20),
        bodySmall = style(12, FontWeight.Normal, 16), // metadata
        labelLarge = style(14, FontWeight.Medium, 20),
        labelMedium = style(12, FontWeight.Medium, 16), // chips, tab labels, field labels
        labelSmall = style(11, FontWeight.Medium, 16),
    )
