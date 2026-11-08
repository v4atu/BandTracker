package com.v4atu.bandtracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokens de color de Penpot (DESIGN.md §2). Los mismos nombres existen en dos sets, `Oscuro` y
 * `Claro`. Contraste WCAG medido en DESIGN §2; `PaletteTest` lo vuelve a comprobar.
 */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val income: Color,
    val expense: Color,
    val onAccent: Color,
    val divider: Color,
)

val DarkPalette = Palette(
    bg = Color(0xFF0A0A0A),
    surface = Color(0xFF141414),
    textPrimary = Color(0xFFEDEDED),
    textSecondary = Color(0xFFA0A0A0),
    income = Color(0xFF02A45E),
    expense = Color(0xFFFF6B6B),
    onAccent = Color(0xFF0A0A0A),
    divider = Color(0xFF2A2A2A),
)

val LightPalette = Palette(
    bg = Color(0xFFFAFAFA),
    surface = Color(0xFFEFEFEF),
    textPrimary = Color(0xFF141414),
    textSecondary = Color(0xFF595959),
    income = Color(0xFF00703C),
    expense = Color(0xFFB3261E),
    onAccent = Color(0xFFFFFFFF),
    divider = Color(0xFFD0D0D0),
)

/** Lo que `ColorScheme` de Material 3 no trae: ingreso, egreso, texto sobre botones de color y divisor. */
@Immutable
data class BandTrackerColors(
    val income: Color,
    val expense: Color,
    val onAccent: Color,
    val divider: Color,
)

val LocalBandTrackerColors = staticCompositionLocalOf {
    BandTrackerColors(
        income = Color.Unspecified,
        expense = Color.Unspecified,
        onAccent = Color.Unspecified,
        divider = Color.Unspecified,
    )
}

fun Palette.toExtendedColors() = BandTrackerColors(income, expense, onAccent, divider)

/**
 * Mapeo de DESIGN §2. Todo lo que el diseño no define queda **neutro** (no el morado base de
 * Material): solo ingreso (verde) y egreso (rojo) llevan color, y el rojo también marca errores.
 */
fun Palette.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        background = bg,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        onSurfaceVariant = textSecondary,
        inverseSurface = textPrimary, // el aviso usa los colores invertidos (DESIGN §6)
        inverseOnSurface = bg,
        inversePrimary = textPrimary,
        primary = textPrimary,
        onPrimary = bg,
        primaryContainer = surface,
        onPrimaryContainer = textPrimary,
        secondary = textPrimary,
        onSecondary = bg,
        secondaryContainer = surface,
        onSecondaryContainer = textPrimary,
        tertiary = textPrimary,
        onTertiary = bg,
        tertiaryContainer = surface,
        onTertiaryContainer = textPrimary,
        error = expense,
        onError = onAccent,
        errorContainer = surface,
        onErrorContainer = expense,
        outline = divider,
        outlineVariant = divider,
        scrim = Color.Black,
        surfaceVariant = surface,
        surfaceTint = surface, // sin tinte tonal por elevación
        surfaceDim = bg,
        surfaceBright = surface,
        surfaceContainerLowest = bg,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface,
    )
}
