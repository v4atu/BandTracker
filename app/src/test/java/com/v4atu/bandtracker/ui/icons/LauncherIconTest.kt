package com.v4atu.bandtracker.ui.icons

import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El ícono de la app: un ícono adaptativo (fondo, primer plano y monocromo para los íconos temáticos
 * de Android 13+) con sus PNG en las cinco densidades. Con minSdk 33 basta el ícono adaptativo; estas
 * pruebas vigilan que todo lo que declara exista, tenga el tamaño correcto y no queden restos del
 * ícono anterior, que darían recursos duplicados.
 */
class LauncherIconTest {

    private val root: File = generateSequence(File(System.getProperty("user.dir")!!).absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() }
    private val res = File(root, "app/src/main/res")

    /** Densidad → factor sobre mdpi. */
    private val densities = mapOf("mdpi" to 1.0, "hdpi" to 1.5, "xhdpi" to 2.0, "xxhdpi" to 3.0, "xxxhdpi" to 4.0)

    private fun pixels(file: File): Pair<Int, Int> {
        val image = requireNotNull(ImageIO.read(file)) { "${file.path} no es una imagen legible" }
        return image.width to image.height
    }

    @Test
    fun theManifest_usesTheLauncherIcon() {
        val manifest = File(root, "app/src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("""android:icon="@mipmap/ic_launcher""""))
    }

    @Test
    fun theManifest_doesNotPointToAResourceThatIsNotThere() {
        val manifest = File(root, "app/src/main/AndroidManifest.xml").readText()

        Regex("""android:(?:icon|roundIcon)="@mipmap/([a-z_]+)"""").findAll(manifest).forEach { match ->
            val name = match.groupValues[1]
            val exists = res.listFiles { f -> f.isDirectory && f.name.startsWith("mipmap") }!!
                .any { dir -> dir.listFiles { f -> f.nameWithoutExtension == name }!!.isNotEmpty() }
            assertTrue("el manifest usa @mipmap/$name y no existe", exists)
        }
    }

    @Test
    fun theAdaptiveIcon_declaresBackgroundForegroundAndMonochrome() {
        val xml = File(res, "mipmap-anydpi-v26/ic_launcher.xml").readText()

        assertTrue(xml.contains("<adaptive-icon"))
        assertTrue(xml.contains("""<background android:drawable="@mipmap/ic_launcher_background""""))
        assertTrue(xml.contains("""<foreground android:drawable="@mipmap/ic_launcher_foreground""""))
        assertTrue("sin capa monocromo no hay ícono temático", xml.contains("""<monochrome android:drawable="@mipmap/ic_launcher_monochrome""""))
    }

    @Test
    fun everyLayer_existsInEveryDensity_at108dpScaled() {
        listOf("ic_launcher_background", "ic_launcher_foreground", "ic_launcher_monochrome").forEach { layer ->
            densities.forEach { (density, factor) ->
                val file = File(res, "mipmap-$density/$layer.png")
                assertTrue("falta ${file.relativeTo(root)}", file.exists())
                val side = (108 * factor).toInt()
                assertEquals("${file.name} en $density", side to side, pixels(file))
            }
        }
    }

    @Test
    fun theLegacyIcon_existsInEveryDensity_at48dpScaled() {
        densities.forEach { (density, factor) ->
            val file = File(res, "mipmap-$density/ic_launcher.png")
            assertTrue("falta ${file.relativeTo(root)}", file.exists())
            val side = (48 * factor).toInt()
            assertEquals("ic_launcher.png en $density", side to side, pixels(file))
        }
    }

    @Test
    fun noLeftoversOfThePreviousIcon_whichWouldDuplicateResources() {
        val webp = res.walkTopDown().filter { it.name.startsWith("ic_launcher") && it.extension == "webp" }.toList()
        assertTrue("restos .webp: ${webp.map { it.name }}", webp.isEmpty())

        assertFalse("mipmap-anydpi (sin -v26) del ícono anterior", File(res, "mipmap-anydpi").exists())
        assertFalse(File(res, "drawable/ic_launcher_background.xml").exists())
        assertFalse(File(res, "drawable/ic_launcher_foreground.xml").exists())
        assertTrue("ic_launcher_round ya no existe", res.walkTopDown().none { it.name.startsWith("ic_launcher_round") })
    }

    @Test
    fun theForegroundAndMonochromeLayers_areTransparentAroundTheDrawing() {
        // En un ícono adaptativo el dibujo vive en la zona segura (66 dp de 108): las esquinas deben ser transparentes.
        listOf("ic_launcher_foreground", "ic_launcher_monochrome").forEach { layer ->
            val image = ImageIO.read(File(res, "mipmap-xxxhdpi/$layer.png"))
            val corners = listOf(0 to 0, image.width - 1 to 0, 0 to image.height - 1, image.width - 1 to image.height - 1)
            corners.forEach { (x, y) ->
                val alpha = image.getRGB(x, y) ushr 24
                assertEquals("$layer: esquina ($x,$y) debe ser transparente", 0, alpha)
            }
        }
    }
}
