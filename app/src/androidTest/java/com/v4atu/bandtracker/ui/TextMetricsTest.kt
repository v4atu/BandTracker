package com.v4atu.bandtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.v4atu.bandtracker.ui.components.BandText
import com.v4atu.bandtracker.ui.theme.BandTrackerTextStyles
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * DESIGN §3: interlineado de 1.2 × el tamaño. Istok Web mide ~1.44× de alto natural y Compose no
 * baja una línea suelta de esa altura con ningún `LineHeightStyle` (medido en dispositivo), así que
 * `BandText` fija la caja de la línea a 1.2× centrando el texto, como CSS. Se mide en el dispositivo.
 */
class TextMetricsTest {

    @get:Rule
    val compose = createComposeRule()

    private val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density

    private fun styles(): Map<String, Pair<TextStyle, Float>> = mapOf(
        "amount" to (BandTrackerTextStyles.amount to 48f),
        "title" to (BandTrackerTextStyles.title to 36f),
        "action" to (BandTrackerTextStyles.action to 24f),
        "body" to (BandTrackerTextStyles.body to 16f),
        "bodyEmphasis" to (BandTrackerTextStyles.bodyEmphasis to 16f),
        "note" to (BandTrackerTextStyles.note to 12f),
    )

    @Test
    fun aSingleLineIsExactly1_2TimesTheFontSizeTall_forEveryStyle() {
        compose.setContent {
            BandTrackerTheme {
                Column {
                    for ((name, pair) in styles()) {
                        BandText("Hgñ¿Áy", style = pair.first, color = Color.White, modifier = Modifier.testTag(name))
                    }
                }
            }
        }

        for ((name, pair) in styles()) {
            val heightDp = compose.onNodeWithTag(name).fetchSemanticsNode().size.height / density
            // 1 px de tolerancia por redondeo a píxeles enteros.
            assertEquals("$name: alto de una línea", pair.second * 1.2f, heightDp, 1f / density + 0.01f)
        }
    }

    @Test
    fun theLetterIsCenteredInsideItsLineBox_notPushedOutOfIt() {
        // La «H» (altura de mayúscula) debe quedar entera dentro de la caja de 1.2×, para cada estilo.
        compose.setContent {
            BandTrackerTheme {
                Column {
                    for ((name, pair) in styles()) {
                        BandText(
                            "H", style = pair.first, color = Color.White,
                            modifier = Modifier.testTag(name).background(Color.Black).width(80.dp),
                        )
                    }
                }
            }
        }

        for ((name, _) in styles()) {
            val pixels = compose.onNodeWithTag(name).captureToImage().toPixelMap()
            val inkRows = (0 until pixels.height).filter { y -> (0 until pixels.width).any { x -> pixels[x, y].red > 0.5f } }
            assertTrue("$name: no se dibujó la letra", inkRows.isNotEmpty())
            // La tinta no toca el borde superior ni el inferior de la caja (la «H» no se recorta).
            assertTrue("$name: la «H» toca el borde superior (fila ${inkRows.first()})", inkRows.first() > 0)
            assertTrue("$name: la «H» toca el borde inferior (fila ${inkRows.last()} de ${pixels.height})", inkRows.last() < pixels.height - 1)
            // Y está razonablemente centrada: el margen superior e inferior no difieren en más de un tercio del alto.
            val top = inkRows.first()
            val bottom = pixels.height - 1 - inkRows.last()
            assertTrue("$name: descentrada (arriba $top px, abajo $bottom px)", kotlin.math.abs(top - bottom) <= pixels.height / 3)
        }
    }
}
