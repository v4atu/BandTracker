package com.v4atu.bandtracker.ui.entry

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import com.v4atu.bandtracker.ui.ScreenTestBase
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Herramienta de revisión visual (DESIGN §7): guarda capturas del selector de fecha en
 * `getExternalFilesDir()/gallery/` para compararlas con el tablero «5.2 Selector de fecha».
 */
class DatePickerGalleryTest : ScreenTestBase() {

    private val out = File(context.getExternalFilesDir(null), "gallery").apply { mkdirs() }
    private val today = LocalDate.of(2026, 11, 6)

    private fun save(name: String, bitmap: Bitmap) {
        val file = File(out, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 0)
    }

    private fun show(dark: Boolean, initial: LocalDate = today) = showScreen(dark) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            DatePickerContent(
                initial = initial, today = today, locale = Locale.forLanguageTag("es-CO"),
                firstDayOfWeek = DayOfWeek.MONDAY, onConfirm = {}, onDismiss = {},
            )
        }
    }

    @Test
    fun datePicker() {
        show(dark = true)
        save("date-picker", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun datePickerLight() {
        show(dark = false)
        save("date-picker-light", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun datePickerPastMonth() {
        show(dark = true, initial = LocalDate.of(2026, 10, 14))
        save("date-picker-october", compose.onRoot().captureToImage().asAndroidBitmap())
    }
}
