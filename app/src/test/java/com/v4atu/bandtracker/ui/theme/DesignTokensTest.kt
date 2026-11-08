package com.v4atu.bandtracker.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** DESIGN.md §3 (tipografía) y §4 (espaciado, radios, áreas táctiles). */
class DesignTokensTest {

    private val styles = BandTrackerTextStyles

    private fun assertStyle(name: String, style: androidx.compose.ui.text.TextStyle, size: Float, weight: FontWeight) {
        assertEquals("$name tamaño", size, style.fontSize.value, 0f)
        assertEquals("$name unidad", TextUnitType.Sp, style.fontSize.type)
        assertEquals("$name peso", weight, style.fontWeight)
        // Interlineado: 1.2 × el tamaño de fuente.
        assertEquals("$name interlineado", size * 1.2f, style.lineHeight.value, 0.001f)
        assertEquals("$name unidad interlineado", TextUnitType.Sp, style.lineHeight.type)
        assertSame("$name familia", IstokWeb, style.fontFamily)
    }

    @Test
    fun textStyles_matchPenpotTypographies() {
        assertStyle("Monto", styles.amount, 48f, FontWeight.Bold)
        assertStyle("Título", styles.title, 36f, FontWeight.Bold)
        assertStyle("Acción", styles.action, 24f, FontWeight.Bold)
        assertStyle("Cuerpo", styles.body, 16f, FontWeight.Normal)
        assertStyle("Énfasis", styles.bodyEmphasis, 16f, FontWeight.Bold)
        assertStyle("Nota", styles.note, 12f, FontWeight.Normal)
    }

    @Test
    fun spacingAndShapeTokens_matchPenpot() {
        assertEquals(8f, BandTrackerDimens.Space8.value, 0f)
        assertEquals(16f, BandTrackerDimens.Space16.value, 0f)
        assertEquals(24f, BandTrackerDimens.Space24.value, 0f)
        assertEquals(28f, BandTrackerDimens.Space28.value, 0f)
        assertEquals(12f, BandTrackerDimens.RadiusMd.value, 0f)
    }

    @Test
    fun screenMargins_andMinimumTouchTarget_matchDesign() {
        assertEquals(28f, BandTrackerDimens.ScreenHorizontal.value, 0f)
        assertEquals(38f, BandTrackerDimens.ScreenTop.value, 0f)
        assertEquals(32f, BandTrackerDimens.ScreenBottom.value, 0f)
        assertEquals(48f, BandTrackerDimens.MinTouchTarget.value, 0f)
    }
}
