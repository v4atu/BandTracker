package com.v4atu.bandtracker.ui.entry

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.domain.DateLabels
import com.v4atu.bandtracker.domain.EntryType
import com.v4atu.bandtracker.ui.ScreenTestBase
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC §5.2 y DESIGN §7: tableros «5.2 Ingreso», «5.2 Egreso» y «5.2 Ingreso · sugerencias de sujeto». */
class EntryFormScreenTest : ScreenTestBase() {

    private val today = LocalDate.of(2026, 11, 6)
    private val locale get() = context.resources.configuration.locales[0]

    private fun state(
        type: EntryType = EntryType.INCOME,
        canSave: Boolean = true,
        date: LocalDate = today,
        suggestions: List<String> = emptyList(),
        isEdit: Boolean = false,
    ) = EntryFormUiState(
        loaded = true, isEdit = isEdit, type = type, currencyCode = "USD", date = date, today = today,
        canSave = canSave, suggestions = suggestions,
    )

    private class Calls {
        var date = 0
        var save = 0
        var delete = 0
    }

    private lateinit var amount: TextFieldState
    private lateinit var subject: TextFieldState
    private lateinit var note: TextFieldState

    /** Barras del sistema (dp) que la pantalla respeta con insets: se suman a los márgenes de Penpot. */
    private var topInsetDp = 0f
    private var bottomInsetDp = 0f

    private fun show(state: EntryFormUiState, calls: Calls = Calls(), dark: Boolean = true) {
        showScreen(dark) {
            val metrics = LocalDensity.current
            topInsetDp = WindowInsets.safeDrawing.getTop(metrics) / metrics.density
            bottomInsetDp = WindowInsets.safeDrawing.getBottom(metrics) / metrics.density
            amount = rememberTextFieldState()
            subject = rememberTextFieldState()
            note = rememberTextFieldState()
            EntryFormScreen(
                state = state, amount = amount, subject = subject, note = note,
                onDateClick = { calls.date++ }, onSave = { calls.save++ }, onDelete = { calls.delete++ },
            )
        }
    }

    private val dateLabel get() = string(R.string.entry_date)
    private val subjectLabel get() = string(R.string.entry_subject)
    private val noteLabel get() = string(R.string.entry_note)
    private val optional get() = string(R.string.entry_optional)

    // --- Contenido ---

    @Test
    fun income_showsItsTitleAndItsButton() {
        show(state(EntryType.INCOME))

        compose.onNodeWithText(string(R.string.home_income)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.entry_save_income)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.entry_save_expense)).assertDoesNotExist()
    }

    @Test
    fun expense_showsItsTitleAndItsButton() {
        show(state(EntryType.EXPENSE))

        compose.onNodeWithText(string(R.string.home_expense)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.entry_save_expense)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.entry_save_income)).assertDoesNotExist()
    }

    @Test
    fun showsTheAmountWithTheCurrencySymbol_andTheThreeFields() {
        show(state())

        compose.onNode(hasSetTextAction() and hasContentDescription(string(R.string.entry_amount))).assertIsDisplayed()
        compose.onNodeWithText(dateLabel).assertIsDisplayed()
        compose.onNodeWithText(subjectLabel).assertIsDisplayed()
        compose.onNodeWithText(noteLabel).assertIsDisplayed()
        compose.onAllNodesWithText(optional).assertCountEquals(2)
    }

    @Test
    fun dateShowsToday_whenItIsToday_andTheFormattedDateOtherwise() {
        show(state(date = today))
        compose.onNodeWithText(string(R.string.entry_today)).assertIsDisplayed()
    }

    @Test
    fun dateShowsTheFormattedDate_whenItIsNotToday() {
        show(state(date = today.minusDays(3)))

        compose.onNodeWithText(string(R.string.entry_today)).assertDoesNotExist()
        compose.onNodeWithText(DateLabels.field(today.minusDays(3), locale)).assertIsDisplayed()
    }

    // --- Interacción ---

    @Test
    fun tappingTheDateField_asksToOpenThePicker() {
        val calls = Calls()
        show(state(), calls)

        compose.onNodeWithText(dateLabel).performClick()

        assertEquals(1, calls.date)
    }

    @Test
    fun theButtonIsDisabled_untilThereIsAValidAmount() {
        val calls = Calls()
        show(state(canSave = false), calls)

        compose.onNodeWithText(string(R.string.entry_save_income)).assertIsNotEnabled().performClick()

        assertEquals(0, calls.save)
    }

    @Test
    fun theEnabledButton_asksToSave() {
        val calls = Calls()
        show(state(canSave = true), calls)

        compose.onNodeWithText(string(R.string.entry_save_income)).assertIsEnabled().performClick()

        assertEquals(1, calls.save)
    }

    @Test
    fun whatTheUserTypes_goesToTheTextStates() {
        show(state())

        compose.onNode(hasSetTextAction() and hasContentDescription(string(R.string.entry_amount))).performTextInput("12.5")
        compose.onNodeWithText(subjectLabel).performTextInput("Taxi")
        compose.onNodeWithText(noteLabel).performTextInput("Aeropuerto")

        compose.runOnIdle {
            assertEquals("12.5", amount.text.toString())
            assertEquals("Taxi", subject.text.toString())
            assertEquals("Aeropuerto", note.text.toString())
        }
    }

    // --- Medidas de Penpot (tablero «5.2 Ingreso») ---

    @Test
    fun fieldsAre56dpTall_with16dpBetweenThem() {
        show(state())

        val date = compose.onNodeWithText(dateLabel)
        val subjectField = compose.onNodeWithText(subjectLabel)
        val noteField = compose.onNodeWithText(noteLabel)

        assertDp("alto de Fecha", 56f, date.heightDp())
        assertDp("alto de Sujeto", 56f, subjectField.heightDp())
        assertDp("alto de Nota", 56f, noteField.heightDp())
        assertDp("hueco Fecha→Sujeto", 16f, subjectField.topDp() - date.bottomDp())
        assertDp("hueco Sujeto→Nota", 16f, noteField.topDp() - subjectField.bottomDp())
    }

    @Test
    fun theButtonIs53dpTall_andSitsAtTheBottom() {
        show(state())

        val button = compose.onNodeWithText(string(R.string.entry_save_income))
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

        assertDp("alto del botón", 53f, button.heightDp())
        assertDp("margen inferior (32 + barra del sistema)", 32f + bottomInsetDp, root.bottom / density - button.bottomDp())
    }

    @Test
    fun theTitleIsAtTheTop_centered() {
        show(state())

        val title = compose.onNodeWithText(string(R.string.home_income))
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

        // El nodo de un Text mide el alto natural de la fuente y sobresale de su caja de línea (44 dp en
        // Penpot, ver BandText): lo que coincide con el diseño es el centro, a 38 + barra + 22 dp.
        assertDp("centro vertical del título", 38f + topInsetDp + 22f, title.centerYDp())
        assertDp("centrado", root.width / density / 2, (title.leftDp() + title.rightDp()) / 2)
    }

    // --- Editar (tablero «5.3 Editar registro») ---

    @Test
    fun editing_showsTheEditTitle_theSaveButton_andDelete() {
        show(state(EntryType.EXPENSE, isEdit = true))

        compose.onNodeWithText(string(R.string.entry_edit_title)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.entry_save)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.entry_delete)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.home_expense)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.entry_save_expense)).assertDoesNotExist()
    }

    @Test
    fun creating_hasNoDeleteButton_andNoEditTitle() {
        show(state(EntryType.INCOME, isEdit = false))

        compose.onNodeWithText(string(R.string.entry_delete)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.entry_edit_title)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.entry_save)).assertDoesNotExist()
    }

    @Test
    fun editing_theSaveButtonFollowsTheSameRulesAsCreating() {
        val calls = Calls()
        show(state(isEdit = true, canSave = false), calls)

        compose.onNodeWithText(string(R.string.entry_save)).assertIsNotEnabled().performClick()

        assertEquals(0, calls.save)
    }

    @Test
    fun editing_saveAsksToSave_andDeleteAsksToDelete() {
        val calls = Calls()
        show(state(isEdit = true), calls)

        compose.onNodeWithText(string(R.string.entry_save)).performClick()
        compose.onNodeWithText(string(R.string.entry_delete)).performClick()

        assertEquals(1, calls.save)
        assertEquals(1, calls.delete)
    }

    @Test
    fun editing_deleteIs24dpBelowSave_48dpTall_andSitsAtTheBottom() {
        show(state(isEdit = true))

        val save = compose.onNodeWithText(string(R.string.entry_save))
        val delete = compose.onNodeWithText(string(R.string.entry_delete))
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

        // Penpot: «Guardar» 53 dp, 24 dp de hueco y «Borrar» de 48 dp pegado al margen inferior (32).
        assertDp("hueco Guardar→Borrar", 24f, delete.topDp() - save.bottomDp())
        assertDp("alto de Borrar", 48f, delete.heightDp())
        assertDp("margen inferior (32 + barra del sistema)", 32f + bottomInsetDp, root.bottom / density - delete.bottomDp())
    }

    // --- Sugerencias de sujeto (tablero «sugerencias») ---

    private val suggestions = listOf("Almuerzo", "Alquiler", "Almacén", "Alimentos", "Alojamiento")

    @Test
    fun suggestionsAreHidden_untilTheSubjectFieldHasFocus() {
        show(state(suggestions = suggestions))

        compose.onNodeWithText("Almuerzo").assertDoesNotExist()
    }

    @Test
    fun focusingTheSubject_showsAtMostTheSuggestionsGiven() {
        show(state(suggestions = suggestions))

        compose.onNodeWithText(subjectLabel).performClick()

        suggestions.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun theSuggestionsPanelSitsBetweenSubjectAndNote_pushingTheNoteDown() {
        show(state(suggestions = suggestions))
        compose.onNodeWithText(subjectLabel).performClick()

        val subjectField = compose.onNodeWithText(subjectLabel)
        val first = compose.onNodeWithText("Almuerzo")
        val last = compose.onNodeWithText("Alojamiento")
        val noteField = compose.onNodeWithText(noteLabel)

        // Penpot: panel a 16 dp bajo «Sujeto», filas de 48 dp con 4 dp de margen arriba y abajo, y «Nota» a 16 dp del panel.
        assertDp("alto de una sugerencia", 48f, first.heightDp())
        assertDp("Sujeto→primera sugerencia", 16f + 4f, first.topDp() - subjectField.bottomDp())
        assertDp("última sugerencia→Nota", 4f + 16f, noteField.topDp() - last.bottomDp())
    }

    @Test
    fun tappingASuggestion_fillsTheSubject_andClosesTheList() {
        show(state(suggestions = suggestions))
        compose.onNodeWithText(subjectLabel).performClick()

        compose.onNodeWithText("Alquiler").performClick()

        compose.runOnIdle { assertEquals("Alquiler", subject.text.toString()) }
        compose.onNodeWithText("Almuerzo").assertDoesNotExist()
    }

    @Test
    fun aSuggestionEqualToWhatIsTyped_isNotOffered() {
        show(state(suggestions = listOf("Taxi")))
        compose.onNodeWithText(subjectLabel).performClick()
        compose.onNodeWithText(subjectLabel).performTextInput("Taxi")

        compose.onAllNodesWithText("Taxi").assertCountEquals(1)
    }
}
