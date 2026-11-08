package com.v4atu.bandtracker.ui.icons

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * DESIGN.md §5: 14 íconos de Lucide como VectorDrawable (24 dp, trazo de 2, puntas y uniones
 * redondeadas). Los conteos de trazos salen de los componentes `Ícono` de Penpot.
 */
class IconResourcesTest {

    private val drawables = File("src/main/res/drawable")

    /** nombre del recurso -> cantidad de trazos del componente en Penpot */
    private val expectedStrokes = mapOf(
        "ic_plus" to 2,
        "ic_minus" to 1,
        "ic_list" to 6,
        "ic_circle_question_mark" to 3,
        "ic_chevron_down" to 1,
        "ic_chevron_right" to 1,
        "ic_chevron_left" to 1,
        "ic_check" to 1,
        "ic_upload" to 3,
        "ic_download" to 3,
        "ic_external_link" to 3,
        "ic_trash" to 5,
        "ic_search" to 2,
        "ic_calendar" to 4,
    )

    private fun vector(name: String): Element {
        val file = File(drawables, "$name.xml")
        assertTrue("falta ${file.path}", file.exists())
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
    }

    private fun Element.attr(name: String): String = getAttribute("android:$name")

    private fun paths(root: Element): List<Element> =
        (0 until root.getElementsByTagName("path").length).map { root.getElementsByTagName("path").item(it) as Element }

    @Test
    fun allFourteenIconsExist_andNothingElseIsNamedIc() {
        val icons = drawables.listFiles { f -> f.name.startsWith("ic_") && !f.name.startsWith("ic_launcher") }!!
            .map { it.name.removeSuffix(".xml") }.toSet()

        assertEquals(expectedStrokes.keys, icons)
    }

    @Test
    fun everyIcon_isA24dpVectorWith24Viewport() {
        for (name in expectedStrokes.keys) {
            val root = vector(name)
            assertEquals(name, "vector", root.tagName)
            assertEquals(name, "24dp", root.attr("width"))
            assertEquals(name, "24dp", root.attr("height"))
            assertEquals(name, "24", root.attr("viewportWidth"))
            assertEquals(name, "24", root.attr("viewportHeight"))
        }
    }

    @Test
    fun everyIcon_hasTheStrokeCountOfItsPenpotComponent() {
        for ((name, count) in expectedStrokes) {
            assertEquals(name, count, paths(vector(name)).size)
        }
    }

    @Test
    fun everyPath_isA2pxRoundStroke_withoutFill() {
        for (name in expectedStrokes.keys) {
            for (path in paths(vector(name))) {
                assertEquals(name, "2", path.attr("strokeWidth"))
                assertEquals(name, "round", path.attr("strokeLineCap"))
                assertEquals(name, "round", path.attr("strokeLineJoin"))
                assertTrue(name, path.attr("strokeColor").isNotBlank())
                assertFalse("$name no debe rellenar", path.hasAttribute("android:fillColor"))
            }
        }
    }

    @Test
    fun everyPath_hasOnlyAbsoluteMoveLineCurveArcAndClose() {
        val allowed = Regex("^[MLCAHVZ0-9.,\\-\\s]+$")
        for (name in expectedStrokes.keys) {
            for (path in paths(vector(name))) {
                val data = path.attr("pathData")
                assertTrue("$name: pathData vacío", data.isNotBlank())
                assertTrue("$name: comandos no esperados en '$data'", allowed.matches(data))
                assertTrue("$name: debe empezar con M", data.trimStart().startsWith("M"))
            }
        }
    }

    @Test
    fun everyCoordinate_staysInsideTheViewport() {
        // Con trazo de 2 el límite útil es 0..24; ninguna coordenada debe salirse (señal de origen mal restado).
        for (name in expectedStrokes.keys) {
            for (path in paths(vector(name))) {
                val numbers = Regex("-?\\d+(\\.\\d+)?").findAll(path.attr("pathData")).map { it.value.toDouble() }
                for (n in numbers) assertTrue("$name: coordenada $n fuera de 0..24", n in 0.0..24.0)
            }
        }
    }

    @Test
    fun plusIcon_isTheLucideCross() {
        val data = paths(vector("ic_plus")).map { it.attr("pathData").replace(" ", "") }.toSet()

        assertEquals(setOf("M5,12L19,12", "M12,5L12,19"), data)
    }
}
