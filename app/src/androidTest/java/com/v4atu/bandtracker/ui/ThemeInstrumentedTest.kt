package com.v4atu.bandtracker.ui

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.res.ResourcesCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.ui.theme.BandTrackerColors
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme
import com.v4atu.bandtracker.ui.theme.DarkPalette
import com.v4atu.bandtracker.ui.theme.IstokWeb
import com.v4atu.bandtracker.ui.theme.LightPalette
import com.v4atu.bandtracker.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** DESIGN §2/§3 y SPEC §8: tema propio (sin color dinámico), fuente y avisos de licencia empaquetados. */
class ThemeInstrumentedTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private class Captured(val colors: BandTrackerColors, val scheme: ColorScheme, val typography: Typography)

    private fun capture(dark: Boolean): Captured {
        lateinit var result: Captured
        compose.setContent {
            BandTrackerTheme(darkTheme = dark) {
                result = Captured(BandTrackerTheme.colors, MaterialTheme.colorScheme, MaterialTheme.typography)
            }
        }
        compose.waitForIdle()
        return result
    }

    private fun assertProvides(palette: Palette, captured: Captured) {
        assertEquals(palette.income, captured.colors.income)
        assertEquals(palette.expense, captured.colors.expense)
        assertEquals(palette.onAccent, captured.colors.onAccent)
        assertEquals(palette.divider, captured.colors.divider)
        assertEquals(palette.bg, captured.scheme.background)
        assertEquals(palette.surface, captured.scheme.surface)
        assertEquals(palette.textPrimary, captured.scheme.onBackground)
        assertEquals(palette.textSecondary, captured.scheme.onSurfaceVariant)
    }

    // Este dispositivo es Android 13+ (minSdk 33): el color dinámico estaría disponible y taparía la paleta.
    @Test
    fun darkTheme_providesThePenpotPalette_notTheWallpaperColors() {
        assertProvides(DarkPalette, capture(dark = true))
    }

    @Test
    fun lightTheme_providesThePenpotPalette_notTheWallpaperColors() {
        assertProvides(LightPalette, capture(dark = false))
    }

    @Test
    fun materialComponents_useIstokWebToo() {
        val typography = capture(dark = false).typography

        assertEquals(IstokWeb, typography.bodyLarge.fontFamily)
        assertEquals(IstokWeb, typography.titleLarge.fontFamily)
        assertEquals(IstokWeb, typography.labelSmall.fontFamily)
    }

    @Test
    fun istokWebFonts_areBundledAndLoad() {
        assertNotNull(ResourcesCompat.getFont(context, R.font.istok_web_regular))
        assertNotNull(ResourcesCompat.getFont(context, R.font.istok_web_bold))
    }

    @Test
    fun licenseNoticesAreInsideTheApk() {
        fun asset(name: String) = context.assets.open("licenses/$name").bufferedReader().use { it.readText() }

        val ofl = asset("istok-web-OFL.txt")
        assertTrue(ofl.contains("SIL OPEN FONT LICENSE Version 1.1"))
        assertTrue(ofl.contains("Andrey V. Panov"))
        assertTrue(ofl.contains("Reserved Font Name Istok"))

        val lucide = asset("lucide-LICENSE.txt")
        assertTrue(lucide.contains("ISC License"))
    }
}
