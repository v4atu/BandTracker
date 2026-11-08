package com.v4atu.bandtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.v4atu.bandtracker.R
import org.junit.Assert.assertEquals
import org.junit.Test

/** DESIGN §6: «Campo» y «Campo con ícono» (56 dp, línea divisoria de 1 dp, valor a la derecha). */
class FieldComponentsTest : ComponentTestBase() {

    // --- Campo de solo lectura (p. ej. la fecha) ---

    @Test
    fun filledField_showsLabelAndValue_andIs56dpTall() {
        show { FormField(label = "Sujeto", value = "Taxi", placeholder = "Opcional") }

        compose.onNodeWithText("Sujeto").assertIsDisplayed()
        compose.onNodeWithText("Sujeto").assertHeightWithin(56f)
        compose.onNodeWithText("Taxi").assertIsDisplayed()
        compose.onNodeWithText("Opcional").assertDoesNotExist()
    }

    @Test
    fun emptyField_showsThePlaceholderInsteadOfTheValue() {
        show { FormField(label = "Nota", value = "", placeholder = "Opcional") }

        compose.onNodeWithText("Nota").assertIsDisplayed()
        compose.onNodeWithText("Opcional").assertIsDisplayed()
    }

    @Test
    fun readOnlyField_isClickableOnlyWhenOnClickIsGiven() {
        var clicks = 0
        show {
            Column {
                FormField(label = "Fecha", value = "Hoy", onClick = { clicks++ })
                FormField(label = "Fijo", value = "x")
            }
        }

        compose.onNodeWithText("Fecha").assertHasClickAction().performClick()
        compose.onNodeWithText("Fijo").assertHasNoClickAction()
        assertEquals(1, clicks)
    }

    @Test
    fun field_drawsA1dpDividerAtTheBottom() {
        val p = palette(true)
        show { FormField(label = "Sujeto", value = "Taxi") }

        // Penpot: la línea está a 42 dp del borde superior y mide 1 dp.
        compose.onNodeWithText("Sujeto").assertColorAt(p.divider, 50f, 42.5f, "línea divisoria")
    }

    @Test
    fun fieldWithIcon_is56dpTall_withTheDividerAt44dp() {
        val p = palette(false)
        show(dark = false) { FormField(label = "Fecha", value = "Hoy", trailingIcon = R.drawable.ic_calendar, onClick = {}) }

        compose.onNodeWithText("Fecha").assertHeightWithin(56f)
        compose.onNodeWithText("Fecha").assertColorAt(p.divider, 50f, 44.5f, "línea divisoria")
    }

    @Test
    fun fieldWithIcon_drawsTheIconAtTheRightEnd() {
        val p = palette(true)
        show { FormField(label = "Fecha", value = "Hoy", trailingIcon = R.drawable.ic_calendar, onClick = {}) }

        // Calendario de 24 dp pegado a la derecha (355-24 = 331..355), fila centrada en y=12+12=24.
        // Su línea horizontal de y=9 del viewport cruza todo el ancho útil del ícono.
        compose.onNodeWithText("Fecha").assertColorAt(p.textSecondary, 343f, 12f + 9f, "línea del calendario")
    }

    // --- Accesibilidad: con la fuente muy grande el campo crece, no recorta ---

    @Test
    fun fieldGrowsWithALargeFont_insteadOfClippingTheText() {
        show(fontScale = 2f) { FormField(label = "Sujeto", value = "Taxi") }

        val node = compose.onNodeWithText("Sujeto").fetchSemanticsNode()
        // Con la fuente al doble la fila de texto supera los 20 dp de Penpot: el alto debe superar 56 dp.
        assertEquals(true, node.size.height / context.resources.displayMetrics.density > 56f + 10f)
    }

    // --- Campo editable ---

    @Test
    fun editableField_showsPlaceholderUntilTheUserTypes() {
        lateinit var state: TextFieldState
        show {
            state = rememberTextFieldState()
            FormField(label = "Nota", state = state, placeholder = "Opcional")
        }
        compose.onNodeWithText("Opcional").assertIsDisplayed()

        compose.onNode(hasSetTextAction()).performTextInput("hola")

        compose.runOnIdle { assertEquals("hola", state.text.toString()) }
        compose.onNodeWithText("Opcional").assertDoesNotExist()
    }

    @Test
    fun editableField_appliesTheInputTransformation() {
        lateinit var state: TextFieldState
        val digitsOnly = InputTransformation {
            if (!asCharSequence().all { it.isDigit() || it == '.' }) revertAllChanges()
        }
        show {
            state = rememberTextFieldState()
            FormField(label = "Monto", state = state, inputTransformation = digitsOnly)
        }

        compose.onNode(hasSetTextAction()).performTextInput("12.50")
        compose.onNode(hasSetTextAction()).performTextInput("abc")

        compose.runOnIdle { assertEquals("12.50", state.text.toString()) }
    }

    @Test
    fun editableField_isAlso56dpTall_andKeepsItsLabelVisible() {
        show { FormField(label = "Monto", state = remember { TextFieldState("100") }) }

        compose.onNodeWithText("Monto").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertHeightWithin(56f)
    }

    @Test
    fun editableField_withAnError_drawsItsDividerInRed() {
        val p = palette(true)
        show { FormField(label = "Nombre", state = remember { TextFieldState("account 1") }, isError = true) }

        // Penpot «nombre duplicado»: la línea del campo pasa a `color.expense`.
        compose.onNodeWithText("Nombre").assertColorAt(p.expense, 50f, 42.5f, "línea de error")
    }

    @Test
    fun editableField_withoutAnError_keepsTheNormalDivider() {
        val p = palette(true)
        show { FormField(label = "Nombre", state = remember { TextFieldState("account 1") }) }

        compose.onNodeWithText("Nombre").assertColorAt(p.divider, 50f, 42.5f, "línea normal")
    }
}
