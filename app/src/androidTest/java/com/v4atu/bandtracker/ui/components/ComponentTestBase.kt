package com.v4atu.bandtracker.ui.components

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme
import com.v4atu.bandtracker.ui.theme.DarkPalette
import com.v4atu.bandtracker.ui.theme.LightPalette
import com.v4atu.bandtracker.ui.theme.Palette
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule

/**
 * Base de las pruebas de componentes (DESIGN §6). Cada componente se dibuja dentro del tema real,
 * sobre el fondo `bg`, en un ancho de 355 dp (el de las pantallas de Penpot con margen de 28; cabe
 * en pantallas de 384 dp o más).
 */
abstract class ComponentTestBase {

    @get:Rule
    val compose = createComposeRule()

    protected val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val density = context.resources.displayMetrics.density

    protected fun palette(dark: Boolean): Palette = if (dark) DarkPalette else LightPalette

    protected fun show(dark: Boolean = true, fontScale: Float = 1f, content: @Composable () -> Unit) {
        compose.setContent {
            // Escala de fuente fija: el interlineado de Penpot está medido a 1.0 (un teléfono con otra escala no debe alterar las medidas).
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                BandTrackerTheme(darkTheme = dark) {
                    Box(Modifier.background(MaterialTheme.colorScheme.background).width(355.dp)) { content() }
                }
            }
        }
    }

    /** Dibuja [content] con los recursos (strings) en el idioma [locale]: SPEC §6 e criterio 8. */
    protected fun showLocalized(locale: Locale, content: @Composable () -> Unit) {
        val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
        val localized = context.createConfigurationContext(config)
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides config,
                LocalResources provides localized.resources,
            ) {
                BandTrackerTheme(darkTheme = true) {
                    Box(Modifier.background(MaterialTheme.colorScheme.background).width(355.dp)) { content() }
                }
            }
        }
    }

    /** Color del píxel en el punto (x, y) medido en dp desde la esquina superior izquierda del nodo. */
    protected fun SemanticsNodeInteraction.colorAt(xDp: Float, yDp: Float): Color {
        val pixels = captureToImage().toPixelMap()
        return pixels[(xDp * density).toInt().coerceIn(0, pixels.width - 1), (yDp * density).toInt().coerceIn(0, pixels.height - 1)]
    }

    protected fun SemanticsNodeInteraction.assertColorAt(expected: Color, xDp: Float, yDp: Float, label: String) {
        val actual = colorAt(xDp, yDp)
        // Sin antialias en zonas planas: tolerancia mínima por conversión de espacio de color.
        val ok = kotlin.math.abs(actual.red - expected.red) < 0.01f &&
            kotlin.math.abs(actual.green - expected.green) < 0.01f &&
            kotlin.math.abs(actual.blue - expected.blue) < 0.01f
        assertEquals("$label en ($xDp, $yDp) dp: esperado $expected, hay $actual", true, ok)
    }

    /** Alto del nodo en dp, con ±[toleranceDp] por el redondeo de cada medida a píxeles enteros. */
    protected fun SemanticsNodeInteraction.assertHeightWithin(expectedDp: Float, toleranceDp: Float = 1f) {
        val actual = fetchSemanticsNode().size.height / density
        assertEquals("alto en dp", expectedDp, actual, toleranceDp)
    }

    protected fun hasRole(role: Role): SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    protected fun string(id: Int): String = context.getString(id)
}
