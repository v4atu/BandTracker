package com.v4atu.bandtracker.ui.theme

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * El fondo de la ventana debe seguir el tema del sistema desde el primer fotograma, para no
 * parpadear en blanco con el teléfono en oscuro (guía «Dark theme» de Android).
 */
class WindowResourcesTest {

    private val res = File("src/main/res")

    private fun colors(dir: String): Map<String, String> {
        val file = File(res, "$dir/colors.xml")
        assertTrue("falta ${file.path}", file.exists())
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
        val nodes = doc.getElementsByTagName("color")
        return (0 until nodes.length).associate {
            val e = nodes.item(it) as Element
            e.getAttribute("name") to e.textContent.trim().uppercase()
        }
    }

    @Test
    fun windowBackground_isTheBgTokenInLightAndDark() {
        assertEquals("#FFFAFAFA", colors("values")["window_background"])
        assertEquals("#FF0A0A0A", colors("values-night")["window_background"])
    }

    @Test
    fun scaffoldPurplePaletteIsGone() {
        val names = colors("values").keys
        assertFalse(names.any { it.startsWith("purple") || it.startsWith("teal") })
    }

    @Test
    fun appTheme_followsTheSystemDayNightAndUsesTheWindowBackground() {
        val text = File(res, "values/themes.xml").readText()

        assertTrue(text, text.contains("DayNight"))
        assertFalse("no debe fijar el tema claro", text.contains("Theme.Material.Light"))
        assertTrue(text, text.contains("@color/window_background"))
    }
}
