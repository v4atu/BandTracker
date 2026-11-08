package com.v4atu.bandtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Tema de BandTracker. Sin color dinámico a propósito (DESIGN §2): la app usa su propia paleta,
 * no la del fondo de pantalla del usuario, aunque `minSdk = 33` lo permitiría siempre.
 */
@Composable
fun BandTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette

    CompositionLocalProvider(LocalBandTrackerColors provides palette.toExtendedColors()) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(darkTheme),
            typography = materialTypographyWithIstok(),
            content = content,
        )
    }
}

/** Acceso a lo propio del diseño: `BandTrackerTheme.colors.income`, `BandTrackerTheme.text.body`… */
object BandTrackerTheme {
    val colors: BandTrackerColors
        @Composable get() = LocalBandTrackerColors.current

    val text: BandTrackerTextStyles get() = BandTrackerTextStyles

    val dimens: BandTrackerDimens get() = BandTrackerDimens
}
