package com.v4atu.bandtracker.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.domain.EntryRowText
import com.v4atu.bandtracker.domain.EntryType
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Herramienta de revisión visual (DESIGN §6): dibuja los componentes en galerías y las guarda como
 * PNG en `getExternalFilesDir()/gallery/` para compararlas con los componentes de Penpot. Solo
 * verifica que cada galería se haya generado; la comparación la hace una persona.
 */
class ComponentGalleryTest : ComponentTestBase() {

    private val out: File = File(context.getExternalFilesDir(null), "gallery").apply { mkdirs() }

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val file = File(out, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("no se generó ${file.path}", file.length() > 0)
    }

    private val spaced = Arrangement.spacedBy(12.dp)

    @Composable
    private fun Actions() = Column(verticalArrangement = spaced) {
        ActionRow(R.drawable.ic_plus, "Ingreso", Tone.Income, onClick = {})
        ActionRow(R.drawable.ic_minus, "Egreso", Tone.Expense, onClick = {})
        ActionRow(R.drawable.ic_list, "Historial", Tone.Neutral, onClick = {})
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AccountSelector("Account 1", onClick = {}, modifier = Modifier.weight(1f))
            IconAction(R.drawable.ic_circle_question_mark, "Ayuda", onClick = {})
        }
        PrimaryButton("Ingresar", EntryType.INCOME, onClick = {})
        PrimaryButton("Egresar", EntryType.EXPENSE, onClick = {})
        PrimaryButton("Ingresar", EntryType.INCOME, enabled = false, onClick = {})
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton("Texto", Tone.Neutral, onClick = {})
            TextButton("Texto", Tone.Income, icon = R.drawable.ic_plus, onClick = {})
            TextButton("Borrar", Tone.Expense, icon = R.drawable.ic_trash, onClick = {})
        }
    }

    @Composable
    private fun FieldsAndLists() = Column(verticalArrangement = spaced) {
        FormField("Etiqueta", "", placeholder = "Opcional")
        FormField("Etiqueta", "Valor")
        FormField("Etiqueta", "Hoy", trailingIcon = R.drawable.ic_calendar, onClick = {})
        FormField("Nota", TextFieldState(), placeholder = "Opcional")
        EntryRow(EntryRowText.of("Sujeto", "Nota"), "+ 1.000", EntryType.INCOME, onClick = {})
        EntryRow(EntryRowText.of("Sujeto", null), "− 1.000", EntryType.EXPENSE, onClick = {})
        EntryRow(EntryRowText.of(null, null), "− 1.000", EntryType.EXPENSE, onClick = {})
        MonthHeader(YearMonth.of(2026, 11), locale = Locale.forLanguageTag("es-CO"))
        DayHeader(LocalDate.of(2026, 11, 6), locale = Locale.forLanguageTag("es-CO"))
        AccountCard("Account 1", "COP")
        ListOption("Opción", selected = false, onClick = {})
        ListOption("Opción", selected = true, onClick = {})
    }

    @Composable
    private fun Feedback() = Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SavedSnackbar("Ingreso guardado")
        ConfirmDialogContent(
            "¿Borrar este registro?", "Esta acción es definitiva y no se puede deshacer.",
            "Borrar", "Cancelar", destructive = true, onConfirm = {}, onDismiss = {},
        )
        ConfirmDialogContent("Título", "Mensaje", "Confirmar", "Cancelar", destructive = false, onConfirm = {}, onDismiss = {})
    }

    private fun gallery(name: String, dark: Boolean, content: @Composable () -> Unit) {
        show(dark) { content() }
        save("$name-${if (dark) "dark" else "light"}")
    }

    @Test fun actionsDark() = gallery("actions", true) { Actions() }
    @Test fun actionsLight() = gallery("actions", false) { Actions() }
    @Test fun fieldsDark() = gallery("fields", true) { FieldsAndLists() }
    @Test fun fieldsLight() = gallery("fields", false) { FieldsAndLists() }
    @Test fun feedbackDark() = gallery("feedback", true) { Feedback() }
    @Test fun feedbackLight() = gallery("feedback", false) { Feedback() }
}
