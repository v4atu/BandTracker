package com.v4atu.bandtracker.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.v4atu.bandtracker.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DESIGN §10 (abierto 2): comprobar que los trazos de Lucide se conservan al ser VectorDrawable
 * (grosor, puntas y uniones redondeadas). Se dibuja cada ícono a 4 px por dp y se mira el resultado.
 */
class IconRenderTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scale = 4 // px por unidad del viewport de 24
    private val size = 24 * scale

    private val icons = mapOf(
        "plus" to R.drawable.ic_plus,
        "minus" to R.drawable.ic_minus,
        "list" to R.drawable.ic_list,
        "circle-question-mark" to R.drawable.ic_circle_question_mark,
        "chevron-down" to R.drawable.ic_chevron_down,
        "chevron-right" to R.drawable.ic_chevron_right,
        "chevron-left" to R.drawable.ic_chevron_left,
        "check" to R.drawable.ic_check,
        "upload" to R.drawable.ic_upload,
        "download" to R.drawable.ic_download,
        "external-link" to R.drawable.ic_external_link,
        "trash" to R.drawable.ic_trash,
        "search" to R.drawable.ic_search,
        "calendar" to R.drawable.ic_calendar,
    )

    private fun render(id: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, id)
        assertNotNull(drawable)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable!!.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    /** Alfa del píxel en el punto (x, y) expresado en unidades del viewport de 24. */
    private fun Bitmap.alphaAt(x: Double, y: Double): Int =
        (getPixel((x * scale).toInt().coerceIn(0, size - 1), (y * scale).toInt().coerceIn(0, size - 1)) ushr 24) and 0xFF

    private fun Bitmap.assertOpaque(x: Double, y: Double, label: String) =
        assertTrue("$label: se esperaba trazo en ($x, $y), alfa=${alphaAt(x, y)}", alphaAt(x, y) >= 200)

    private fun Bitmap.assertEmpty(x: Double, y: Double, label: String) =
        assertEquals("$label: se esperaba vacío en ($x, $y)", 0, alphaAt(x, y))

    @Test
    fun everyIcon_inflatesAndDrawsSomething_insideItsLiveArea() {
        for ((name, id) in icons) {
            val bitmap = render(id)
            var minX = size; var minY = size; var maxX = -1; var maxY = -1; var painted = 0
            for (y in 0 until size) for (x in 0 until size) {
                if ((bitmap.getPixel(x, y) ushr 24) != 0) {
                    painted++
                    minX = minOf(minX, x); maxX = maxOf(maxX, x); minY = minOf(minY, y); maxY = maxOf(maxY, y)
                }
            }
            assertTrue("$name: no dibuja nada", painted > 0)
            // Lucide deja 2 unidades de margen; con trazo de 2 el borde llega como mucho a 1 unidad de 0 y de 24.
            assertTrue("$name: se sale por la izquierda/arriba ($minX,$minY)", minX >= scale && minY >= scale)
            assertTrue("$name: se sale por la derecha/abajo ($maxX,$maxY)", maxX <= size - scale && maxY <= size - scale)
        }
    }

    @Test
    fun strokeIsTwoUnitsThick() {
        val minus = render(R.drawable.ic_minus) // M5,12 L19,12
        minus.assertOpaque(12.0, 11.2, "minus borde superior")
        minus.assertOpaque(12.0, 12.8, "minus borde inferior")
        minus.assertEmpty(12.0, 10.5, "minus por encima del trazo")
        minus.assertEmpty(12.0, 13.5, "minus por debajo del trazo")
    }

    @Test
    fun capsAreRound_aZeroLengthSegmentStillDrawsADot() {
        // list: "M3,5 L3.01,5". Con puntas planas (butt) no se vería nada.
        val list = render(R.drawable.ic_list)
        list.assertOpaque(3.0, 5.0, "punto de la lista")
        list.assertOpaque(3.0, 12.0, "punto de la lista")
        list.assertOpaque(3.0, 19.0, "punto de la lista")
        // Y es redondo: la esquina del cuadrado de 2×2 queda vacía.
        list.assertEmpty(2.05, 4.05, "esquina del punto")
        // Extremo redondeado del segmento horizontal (M8,5 L21,5): sobresale 1 unidad por la derecha.
        list.assertOpaque(21.6, 5.0, "punta derecha")
    }

    @Test
    fun plusIsACross() {
        val plus = render(R.drawable.ic_plus)
        plus.assertOpaque(12.0, 12.0, "centro")
        plus.assertOpaque(6.0, 12.0, "brazo izquierdo")
        plus.assertOpaque(12.0, 6.0, "brazo superior")
        plus.assertEmpty(6.0, 6.0, "esquina")
    }

    @Test
    fun circleQuestionMark_hasRingAndHollowInside() {
        val icon = render(R.drawable.ic_circle_question_mark)
        icon.assertOpaque(2.0, 12.0, "aro izquierdo")
        icon.assertOpaque(22.0, 12.0, "aro derecho")
        icon.assertOpaque(12.0, 2.0, "aro superior")
        icon.assertEmpty(6.0, 15.0, "interior sin relleno")
        icon.assertOpaque(12.0, 17.0, "punto del signo")
    }

    @Test
    fun search_hasHollowLensAndHandle() {
        val search = render(R.drawable.ic_search)
        search.assertOpaque(3.0, 11.0, "aro de la lupa")
        search.assertEmpty(11.0, 11.0, "interior de la lupa")
        search.assertOpaque(20.0, 20.0, "mango")
    }

    @Test
    fun calendar_hasRoundedBodyRingsAndHeaderRule() {
        val calendar = render(R.drawable.ic_calendar)
        calendar.assertOpaque(12.0, 9.0, "línea del encabezado")
        calendar.assertEmpty(12.0, 16.0, "cuerpo sin relleno")
        calendar.assertOpaque(8.0, 3.0, "anilla izquierda")
        calendar.assertOpaque(3.0, 12.0, "borde izquierdo")
        // Esquina redondeada (radio 2, centro en (5,5), trazo entre radios 1 y 3): (2.2, 2.2) queda a 3.96 → fuera.
        calendar.assertEmpty(2.2, 2.2, "esquina redondeada")
    }

    @Test
    fun chevrons_pointInTheirDirection() {
        render(R.drawable.ic_chevron_right).also {
            it.assertOpaque(15.0, 12.0, "punta derecha")
            it.assertEmpty(9.0, 12.0, "lado vacío")
        }
        render(R.drawable.ic_chevron_left).also {
            it.assertOpaque(9.0, 12.0, "punta izquierda")
            it.assertEmpty(15.0, 12.0, "lado vacío")
        }
        render(R.drawable.ic_chevron_down).also {
            it.assertOpaque(12.0, 15.0, "punta inferior")
            it.assertEmpty(12.0, 9.0, "lado vacío")
        }
    }
}
