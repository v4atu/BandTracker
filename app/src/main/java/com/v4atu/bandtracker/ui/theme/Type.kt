package com.v4atu.bandtracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import com.v4atu.bandtracker.R

/** Istok Web (SIL OFL 1.1), empaquetada en `res/font/` (SPEC §8: sin cargas remotas). */
val IstokWeb = FontFamily(
    Font(R.font.istok_web_regular, FontWeight.Normal),
    Font(R.font.istok_web_bold, FontWeight.Bold),
)

/** Los 6 estilos de Penpot (DESIGN §3). Interlineado: 1.2 × el tamaño de fuente. */
object BandTrackerTextStyles {
    private fun style(size: Int, weight: FontWeight) = TextStyle(
        fontFamily = IstokWeb,
        fontWeight = weight,
        fontSize = size.sp,
        // Entre líneas Compose respeta este valor, pero una línea suelta no baja de la altura natural
        // de Istok Web (~1.44×) con ninguna combinación de LineHeightStyle (medido en dispositivo).
        // Para que mida 1.2× como en Penpot se usa `BandText` / `Modifier.lineBox`.
        lineHeight = TextUnit(size * 1.2f, TextUnitType.Sp),
    )

    /** Monto en captura y edición. */
    val amount = style(48, FontWeight.Bold)

    /** Título de pantalla. */
    val title = style(36, FontWeight.Bold)

    /** Montos de fila, títulos de diálogo, botón primario. */
    val action = style(24, FontWeight.Bold)

    /** Etiquetas, mensajes, sugerencias, aviso. */
    val body = style(16, FontWeight.Normal)

    /** Etiqueta de las filas de acción, botones de texto, valores de campo, nombres. */
    val bodyEmphasis = style(16, FontWeight.Bold)

    /** Nota de la fila, encabezados de día, mensaje de error, avisos. */
    val note = style(12, FontWeight.Normal)
}

/**
 * Los 6 estilos no coinciden con los 15 de Material (DESIGN §3), así que no se fuerzan ahí. Solo
 * se cambia la familia en los 15, para que cualquier componente de Material (diálogos, menús)
 * use Istok Web y no la fuente del sistema; los tamaños siguen siendo los de Material.
 */
internal fun materialTypographyWithIstok(): Typography {
    val base = Typography()
    fun TextStyle.istok() = copy(fontFamily = IstokWeb)
    return base.copy(
        displayLarge = base.displayLarge.istok(), displayMedium = base.displayMedium.istok(),
        displaySmall = base.displaySmall.istok(),
        headlineLarge = base.headlineLarge.istok(), headlineMedium = base.headlineMedium.istok(),
        headlineSmall = base.headlineSmall.istok(),
        titleLarge = base.titleLarge.istok(), titleMedium = base.titleMedium.istok(),
        titleSmall = base.titleSmall.istok(),
        bodyLarge = base.bodyLarge.istok(), bodyMedium = base.bodyMedium.istok(),
        bodySmall = base.bodySmall.istok(),
        labelLarge = base.labelLarge.istok(), labelMedium = base.labelMedium.istok(),
        labelSmall = base.labelSmall.istok(),
    )
}
