package com.v4atu.bandtracker.ui.history

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import com.v4atu.bandtracker.data.Account
import com.v4atu.bandtracker.data.Entry
import com.v4atu.bandtracker.domain.EntryType
import com.v4atu.bandtracker.ui.ScreenTestBase
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Herramienta de revisión visual (DESIGN §7): guarda capturas del Historial en
 * `getExternalFilesDir()/gallery/` para compararlas con el tablero «5.3 Historial» de Penpot.
 */
class HistoryGalleryTest : ScreenTestBase() {

    private val out = File(context.getExternalFilesDir(null), "gallery").apply { mkdirs() }
    private val account = Account(1, "u1", "Account 1", "COP")

    private fun save(name: String, bitmap: Bitmap) {
        val file = File(out, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 0)
    }

    // Los mismos datos del tablero de Penpot (montos en unidad mínima: COP tiene 2 decimales).
    private val entries = listOf(
        Entry(6, "u6", 1, EntryType.EXPENSE, 1_500_000, LocalDate.of(2026, 11, 6), "Almuerzo", "Corrientazo"),
        Entry(5, "u5", 1, EntryType.INCOME, 12_000_000, LocalDate.of(2026, 11, 6), "Venta", null),
        Entry(4, "u4", 1, EntryType.EXPENSE, 300_000, LocalDate.of(2026, 11, 5), "Pasaje bus", null),
        Entry(3, "u3", 1, EntryType.EXPENSE, 200_000, LocalDate.of(2026, 11, 5), null, null),
        Entry(2, "u2", 1, EntryType.INCOME, 150_000_000, LocalDate.of(2026, 10, 30), "Salario", "Quincena"),
    )

    private fun show(dark: Boolean) = showScreen(dark) {
        HistoryScreen(
            state = HistoryUiState(loaded = true, account = account, entries = entries),
            onEntryClick = {}, onLoadMore = {},
        )
    }

    @Test
    fun history() {
        show(dark = true)
        save("history", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun historyLight() {
        show(dark = false)
        save("history-light", compose.onRoot().captureToImage().asAndroidBitmap())
    }
}
