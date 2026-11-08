package com.v4atu.bandtracker.ui.entry

import android.graphics.Bitmap
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.domain.EntryType
import com.v4atu.bandtracker.ui.ScreenTestBase
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Herramienta de revisión visual (DESIGN §7): guarda capturas del formulario de captura en
 * `getExternalFilesDir()/gallery/` para compararlas con «5.2 Ingreso», «5.2 Egreso» y «sugerencias».
 */
class EntryFormGalleryTest : ScreenTestBase() {

    private val out = File(context.getExternalFilesDir(null), "gallery").apply { mkdirs() }
    private val today = LocalDate.of(2026, 11, 6)

    private fun save(name: String, bitmap: Bitmap) {
        val file = File(out, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 0)
    }

    private fun show(
        type: EntryType,
        canSave: Boolean = false,
        suggestions: List<String> = emptyList(),
        amountText: String = "",
        dark: Boolean = true,
        isEdit: Boolean = false,
    ) = showScreen(dark) {
        val amount = rememberTextFieldState(amountText)
        val subject = rememberTextFieldState()
        val note = rememberTextFieldState()
        EntryFormScreen(
            state = EntryFormUiState(
                loaded = true, isEdit = isEdit, type = type, currencyCode = "COP", date = today, today = today,
                canSave = canSave, suggestions = suggestions,
            ),
            amount = amount, subject = subject, note = note, onDateClick = {}, onSave = {}, onDelete = {},
        )
    }

    @Test
    fun income() {
        show(EntryType.INCOME)
        save("entry-income", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun incomeFilled() {
        show(EntryType.INCOME, canSave = true, amountText = "120000")
        save("entry-income-filled", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun expense() {
        show(EntryType.EXPENSE)
        save("entry-expense", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun edit() {
        show(EntryType.EXPENSE, canSave = true, amountText = "15000", isEdit = true)
        save("entry-edit", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun incomeLight() {
        show(EntryType.INCOME, dark = false)
        save("entry-income-light", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun incomeWithSuggestions() {
        show(
            EntryType.INCOME, canSave = true, amountText = "120000",
            suggestions = listOf("Almuerzo", "Alquiler", "Almacén", "Alimentos", "Alojamiento"),
        )
        compose.onNodeWithText(string(R.string.entry_subject)).performClick()
        compose.waitForIdle()
        save("entry-income-suggestions", compose.onRoot().captureToImage().asAndroidBitmap())
    }
}
