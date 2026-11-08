package com.v4atu.bandtracker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** DESIGN.md §2: tokens de color de Penpot (Oscuro/Claro) y su contraste WCAG medido. */
class PaletteTest {

    private fun hex(color: Color) = "#%06X".format(color.toArgb() and 0xFFFFFF)

    // --- Valores exactos de los tokens ---

    @Test
    fun darkPalette_matchesPenpotTokens() {
        assertEquals("#0A0A0A", hex(DarkPalette.bg))
        assertEquals("#141414", hex(DarkPalette.surface))
        assertEquals("#EDEDED", hex(DarkPalette.textPrimary))
        assertEquals("#A0A0A0", hex(DarkPalette.textSecondary))
        assertEquals("#02A45E", hex(DarkPalette.income))
        assertEquals("#FF6B6B", hex(DarkPalette.expense))
        assertEquals("#0A0A0A", hex(DarkPalette.onAccent))
        assertEquals("#2A2A2A", hex(DarkPalette.divider))
    }

    @Test
    fun lightPalette_matchesPenpotTokens() {
        assertEquals("#FAFAFA", hex(LightPalette.bg))
        assertEquals("#EFEFEF", hex(LightPalette.surface))
        assertEquals("#141414", hex(LightPalette.textPrimary))
        assertEquals("#595959", hex(LightPalette.textSecondary))
        assertEquals("#00703C", hex(LightPalette.income))
        assertEquals("#B3261E", hex(LightPalette.expense))
        assertEquals("#FFFFFF", hex(LightPalette.onAccent))
        assertEquals("#D0D0D0", hex(LightPalette.divider))
    }

    @Test
    fun allTokensAreFullyOpaque() {
        for (p in listOf(DarkPalette, LightPalette)) {
            for (c in listOf(p.bg, p.surface, p.textPrimary, p.textSecondary, p.income, p.expense, p.onAccent, p.divider)) {
                assertEquals(0xFF, (c.toArgb() ushr 24) and 0xFF)
            }
        }
    }

    // --- Contraste: reproduce la tabla de DESIGN §2 ---

    private fun channel(v: Int): Double {
        val s = v / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double {
        val argb = color.toArgb()
        return 0.2126 * channel((argb shr 16) and 0xFF) +
            0.7152 * channel((argb shr 8) and 0xFF) +
            0.0722 * channel(argb and 0xFF)
    }

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = luminance(a).coerceAtLeast(luminance(b)) to luminance(a).coerceAtMost(luminance(b))
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertContrast(expected: Double, a: Color, b: Color, label: String) {
        assertEquals(label, expected, contrast(a, b), 0.01)
    }

    @Test
    fun darkContrast_matchesDesignTable() {
        val p = DarkPalette
        assertContrast(16.91, p.textPrimary, p.bg, "texto principal / bg")
        assertContrast(15.74, p.textPrimary, p.surface, "texto principal / surface")
        assertContrast(7.57, p.textSecondary, p.bg, "texto secundario / bg")
        assertContrast(7.04, p.textSecondary, p.surface, "texto secundario / surface")
        assertContrast(6.10, p.income, p.bg, "ingreso / bg")
        assertContrast(7.13, p.expense, p.bg, "egreso / bg")
        assertContrast(5.68, p.income, p.surface, "ingreso / surface")
        assertContrast(6.64, p.expense, p.surface, "egreso / surface")
        assertContrast(6.10, p.onAccent, p.income, "on-accent / income")
        assertContrast(7.13, p.onAccent, p.expense, "on-accent / expense")
        assertContrast(16.91, p.bg, p.textPrimary, "aviso")
    }

    @Test
    fun lightContrast_matchesDesignTable() {
        val p = LightPalette
        assertContrast(17.65, p.textPrimary, p.bg, "texto principal / bg")
        assertContrast(16.02, p.textPrimary, p.surface, "texto principal / surface")
        assertContrast(6.71, p.textSecondary, p.bg, "texto secundario / bg")
        assertContrast(6.09, p.textSecondary, p.surface, "texto secundario / surface")
        assertContrast(5.95, p.income, p.bg, "ingreso / bg")
        assertContrast(6.26, p.expense, p.bg, "egreso / bg")
        assertContrast(5.40, p.income, p.surface, "ingreso / surface")
        assertContrast(5.68, p.expense, p.surface, "egreso / surface")
        assertContrast(6.21, p.onAccent, p.income, "on-accent / income")
        assertContrast(6.54, p.onAccent, p.expense, "on-accent / expense")
        assertContrast(17.65, p.bg, p.textPrimary, "aviso")
    }

    @Test
    fun everyTextPairMeetsWcagAa_inBothThemes() {
        for (p in listOf(DarkPalette, LightPalette)) {
            val textPairs = listOf(
                p.textPrimary to p.bg, p.textPrimary to p.surface,
                p.textSecondary to p.bg, p.textSecondary to p.surface,
                p.income to p.bg, p.income to p.surface,
                p.expense to p.bg, p.expense to p.surface,
                p.onAccent to p.income, p.onAccent to p.expense,
            )
            for ((fg, bg) in textPairs) {
                assertTrue("contraste ${contrast(fg, bg)} < 4.5", contrast(fg, bg) >= 4.5)
            }
        }
    }

    // --- Mapeo a ColorScheme y colores extendidos (DESIGN §2, propuesta) ---

    @Test
    fun extendedColors_carryIncomeExpenseOnAccentAndDivider() {
        val e = DarkPalette.toExtendedColors()

        assertEquals(DarkPalette.income, e.income)
        assertEquals(DarkPalette.expense, e.expense)
        assertEquals(DarkPalette.onAccent, e.onAccent)
        assertEquals(DarkPalette.divider, e.divider)
    }

    // DESIGN §1: «el resto es neutro». Sin esto, Material pinta cursores, ripple y diálogos con su morado base.
    @Test
    fun colorScheme_hasNoStrayMaterialDefaults_everythingElseIsNeutralOrPaletteBased() {
        for ((palette, dark) in listOf(DarkPalette to true, LightPalette to false)) {
            val s = palette.toColorScheme(dark)
            val neutral = setOf(palette.bg, palette.surface, palette.textPrimary, palette.textSecondary, palette.divider)

            assertEquals(palette.textPrimary, s.primary)
            assertEquals(palette.bg, s.onPrimary)
            assertEquals(palette.expense, s.error)
            assertEquals(palette.onAccent, s.onError)
            assertEquals(palette.divider, s.outline)
            assertEquals(palette.divider, s.outlineVariant)
            for (container in listOf(
                s.surfaceVariant, s.surfaceContainerLowest, s.surfaceContainerLow, s.surfaceContainer,
                s.surfaceContainerHigh, s.surfaceContainerHighest, s.surfaceDim, s.surfaceBright, s.surfaceTint,
            )) {
                assertTrue("color $container no es de la paleta", container in neutral)
            }
            for (accent in listOf(s.secondary, s.tertiary, s.primaryContainer, s.secondaryContainer, s.tertiaryContainer)) {
                assertTrue("color $accent no es neutro", accent in neutral)
            }
        }
    }

    @Test
    fun colorScheme_mapsBackgroundSurfaceAndTextTokens() {
        for ((palette, dark) in listOf(DarkPalette to true, LightPalette to false)) {
            val scheme = palette.toColorScheme(dark)

            assertEquals(palette.bg, scheme.background)
            assertEquals(palette.surface, scheme.surface)
            assertEquals(palette.textPrimary, scheme.onBackground)
            assertEquals(palette.textPrimary, scheme.onSurface)
            assertEquals(palette.textSecondary, scheme.onSurfaceVariant)
            // El aviso usa los colores invertidos (DESIGN §6): fondo text.primary, texto bg.
            assertEquals(palette.textPrimary, scheme.inverseSurface)
            assertEquals(palette.bg, scheme.inverseOnSurface)
        }
    }
}
