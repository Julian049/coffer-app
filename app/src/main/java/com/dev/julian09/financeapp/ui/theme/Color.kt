package com.dev.julian09.financeapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Tokens of design-system.md §2, same names in light and dark. Read them through [CofferTheme.colors]. */
@Immutable
data class CofferColors(
    val background: Color,
    val surface: Color,
    val surfaceHighlight: Color,
    val border: Color,
    val borderHighlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val iconContainer: Color,
    val iconOnContainer: Color,
    val income: Color,
    val expense: Color,
    val warning: Color,
) {
    /** Transfers are neither income nor expense: always neutral. */
    val transfer: Color get() = textSecondary

    val danger: Color get() = expense

    /** Home's quick-action strip (Figma `#F2EEEA` in light): a neutral between the background and the highlighted border. */
    val quickStrip: Color get() = lerp(background, borderHighlight, 0.3f)
}

val LightCofferColors =
    CofferColors(
        background = Color(0xFFFAFAF9),
        surface = Color(0xFFFFFFFF),
        surfaceHighlight = Color(0xFFFFFFFF),
        border = Color(0xFFECEAE7),
        borderHighlight = Color(0xFFDAD6D1),
        textPrimary = Color(0xFF1C1917),
        textSecondary = Color(0xFF78716C),
        primary = Color(0xFF4A3328),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFF6E9DD),
        onPrimaryContainer = Color(0xFF7A4520),
        iconContainer = Color(0xFFF8F0E8),
        iconOnContainer = Color(0xFF9C5F33),
        income = Color(0xFF2F7A45),
        expense = Color(0xFFB03A2E),
        warning = Color(0xFF9A6B1F),
    )

val DarkCofferColors =
    CofferColors(
        background = Color(0xFF121110),
        surface = Color(0xFF1C1A19),
        surfaceHighlight = Color(0xFF242120),
        border = Color(0xFF2E2B29),
        borderHighlight = Color(0xFF3F3B38),
        textPrimary = Color(0xFFF5F5F4),
        textSecondary = Color(0xFFA8A29E),
        primary = Color(0xFFD9B99A),
        onPrimary = Color(0xFF2A1C14),
        primaryContainer = Color(0xFF3A2A1E),
        onPrimaryContainer = Color(0xFFF0C9A6),
        iconContainer = Color(0xFF2A221C),
        iconOnContainer = Color(0xFFD08D58),
        income = Color(0xFF8CC77F),
        expense = Color(0xFFF0897C),
        warning = Color(0xFFE0B25E),
    )
