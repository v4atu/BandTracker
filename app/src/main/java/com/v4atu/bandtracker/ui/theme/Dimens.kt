package com.v4atu.bandtracker.ui.theme

import androidx.compose.ui.unit.dp

/** Espaciado, radios, márgenes y áreas táctiles (DESIGN §4). */
object BandTrackerDimens {
    val Space8 = 8.dp
    val Space16 = 16.dp
    val Space24 = 24.dp
    val Space28 = 28.dp

    /** `radius.md`: tarjetas, filas, botones, campos y diálogos. (`radius.full` = círculo: `CircleShape`.) */
    val RadiusMd = 12.dp

    val ScreenHorizontal = 28.dp
    val ScreenTop = 38.dp
    val ScreenBottom = 32.dp

    /** Mínimo de cualquier elemento que se toque (guía de accesibilidad de Compose). */
    val MinTouchTarget = 48.dp
}
