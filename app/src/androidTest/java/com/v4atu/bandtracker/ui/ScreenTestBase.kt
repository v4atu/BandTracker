package com.v4atu.bandtracker.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule

/** Base de las pruebas de pantallas completas: tema real y escala de fuente fija en 1 (como en Penpot). */
abstract class ScreenTestBase {

    @get:Rule
    val compose = createComposeRule()

    protected val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    protected val density: Float = context.resources.displayMetrics.density

    protected fun showScreen(dark: Boolean = true, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
                BandTrackerTheme(darkTheme = dark) { content() }
            }
        }
    }

    protected fun string(id: Int): String = context.getString(id)

    protected fun SemanticsNodeInteraction.topDp(): Float = fetchSemanticsNode().boundsInRoot.top / density
    protected fun SemanticsNodeInteraction.bottomDp(): Float = fetchSemanticsNode().boundsInRoot.bottom / density
    protected fun SemanticsNodeInteraction.leftDp(): Float = fetchSemanticsNode().boundsInRoot.left / density
    protected fun SemanticsNodeInteraction.rightDp(): Float = fetchSemanticsNode().boundsInRoot.right / density
    protected fun SemanticsNodeInteraction.heightDp(): Float = fetchSemanticsNode().size.height / density
    protected fun SemanticsNodeInteraction.centerYDp(): Float = (topDp() + bottomDp()) / 2

    protected fun assertDp(label: String, expected: Float, actual: Float, tolerance: Float = 1f) =
        assertEquals(label, expected, actual, tolerance)
}
