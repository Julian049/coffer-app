package com.dev.julian09.financeapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LocalCofferColors = staticCompositionLocalOf { LightCofferColors }

/** Light and dark follow the system. No dynamic color: the brand palette always wins. */
@Composable
fun CofferTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkCofferColors else LightCofferColors
    CompositionLocalProvider(LocalCofferColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterial(darkTheme),
            typography = CofferTypography,
            shapes = CofferMaterialShapes,
            content = content,
        )
    }
}

object CofferTheme {
    val colors: CofferColors
        @Composable @ReadOnlyComposable
        get() = LocalCofferColors.current
}

// Material components read these slots; every surface slot is the flat surface (no tonal tint, no shadows)
private fun CofferColors.toMaterial(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = primary,
        onSecondary = onPrimary,
        secondaryContainer = primaryContainer,
        onSecondaryContainer = onPrimaryContainer,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surface,
        onSurfaceVariant = textSecondary,
        surfaceTint = Color.Transparent,
        surfaceBright = surface,
        surfaceDim = background,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface,
        outline = border,
        outlineVariant = border,
        error = expense,
        onError = onPrimary,
        scrim = Color.Black.copy(alpha = 0.32f),
    )
}
