package com.v4atu.bandtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.domain.EntryType
import org.junit.Assert.assertEquals
import org.junit.Test

/** DESIGN §6: ActionRow, IconAction, AccountSelector, PrimaryButton y TextButton. Medidas de §4 / Penpot. */
class ActionComponentsTest : ComponentTestBase() {

    // --- ActionRow ---

    @Test
    fun actionRow_is64dpTall_showsItsLabel_andIsAButton() {
        show { ActionRow(R.drawable.ic_plus, "Ingreso", Tone.Income, onClick = {}) }

        compose.onNodeWithText("Ingreso")
            .assertIsDisplayed()
            .assertHeightIsEqualTo(64.dp)
            .assertHasClickAction()
            .assert(hasRole(Role.Button))
    }

    @Test
    fun actionRow_click_invokesCallbackOnce() {
        var clicks = 0
        show { ActionRow(R.drawable.ic_minus, "Egreso", Tone.Expense, onClick = { clicks++ }) }

        compose.onNodeWithText("Egreso").performClick()

        assertEquals(1, clicks)
    }

    private fun checkActionRowColors(dark: Boolean) {
        val p = palette(dark)
        show(dark) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionRow(R.drawable.ic_plus, "Ingreso", Tone.Income, onClick = {})
                ActionRow(R.drawable.ic_minus, "Egreso", Tone.Expense, onClick = {})
                ActionRow(R.drawable.ic_list, "Historial", Tone.Neutral, onClick = {})
            }
        }
        for ((label, expectedIcon) in listOf("Ingreso" to p.income, "Egreso" to p.expense, "Historial" to p.textPrimary)) {
            val row = compose.onNodeWithText(label)
            row.assertColorAt(p.surface, 177f, 4f, "fondo de la fila $label (dark=$dark)")
            // Círculo tonal de 40 dp (a 16,12): su borde izquierdo, lejos del ícono, es color.bg.
            row.assertColorAt(p.bg, 18f, 32f, "círculo tonal $label (dark=$dark)")
            // Centro del ícono de 24 dp, que coincide con el centro del círculo (36, 32).
            row.assertColorAt(expectedIcon, 36f, 32f, "ícono $label (dark=$dark)")
        }
    }

    @Test
    fun actionRow_paintsSurfaceBgCircleAndToneIcon_inDarkTheme() = checkActionRowColors(dark = true)

    @Test
    fun actionRow_paintsSurfaceBgCircleAndToneIcon_inLightTheme() = checkActionRowColors(dark = false)

    // --- IconAction ---

    @Test
    fun iconAction_is48dpCircleButton_withContentDescription() {
        var clicks = 0
        show { IconAction(R.drawable.ic_circle_question_mark, "Ayuda", onClick = { clicks++ }) }

        compose.onNodeWithContentDescription("Ayuda")
            .assertIsDisplayed()
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
            .assert(hasRole(Role.Button))
            .performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun iconAction_paintsSurfaceCircle_withSecondaryIcon() {
        val p = palette(true)
        show { IconAction(R.drawable.ic_circle_question_mark, "Ayuda", onClick = {}) }
        val node = compose.onNodeWithContentDescription("Ayuda")

        node.assertColorAt(p.surface, 24f, 4f, "fondo del botón")
        // Aro izquierdo del ícono: x=2 del viewport + 12 dp de margen = 14 dp, a media altura.
        node.assertColorAt(p.textSecondary, 14f, 24f, "aro del ícono")
    }

    // --- AccountSelector ---

    @Test
    fun accountSelector_is48dpTall_showsNameAndClicks() {
        var clicks = 0
        show { AccountSelector("Efectivo", onClick = { clicks++ }) }

        compose.onNodeWithText("Efectivo")
            .assertIsDisplayed()
            .assertHeightIsEqualTo(48.dp)
            .assertHasClickAction()
            .performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun accountSelector_paintsSurface() {
        val p = palette(false)
        show(dark = false) { AccountSelector("Efectivo", onClick = {}) }

        compose.onNodeWithText("Efectivo").assertColorAt(p.surface, 177f, 3f, "fondo del selector")
    }

    // --- PrimaryButton ---

    private fun checkPrimaryButtonColors(dark: Boolean) {
        val p = palette(dark)
        show(dark) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Ingresar", EntryType.INCOME, onClick = {})
                PrimaryButton("Egresar", EntryType.EXPENSE, onClick = {})
            }
        }

        compose.onNodeWithText("Ingresar").assertIsEnabled().assertColorAt(p.income, 20f, 26f, "ingreso dark=$dark")
        compose.onNodeWithText("Egresar").assertColorAt(p.expense, 20f, 26f, "egreso dark=$dark")
    }

    @Test
    fun primaryButton_enabled_isFilledWithItsTypeColor_inDarkTheme() = checkPrimaryButtonColors(dark = true)

    @Test
    fun primaryButton_enabled_isFilledWithItsTypeColor_inLightTheme() = checkPrimaryButtonColors(dark = false)

    @Test
    fun primaryButton_disabled_usesSurface_andIgnoresClicks() {
        var clicks = 0
        val p = palette(true)
        show { PrimaryButton("Ingresar", EntryType.INCOME, enabled = false, onClick = { clicks++ }) }

        val button = compose.onNodeWithText("Ingresar")
        button.assertIsNotEnabled()
        button.assertColorAt(p.surface, 20f, 26f, "botón deshabilitado")
        button.performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun primaryButton_isFullWidth_andAtLeast48dpTall() {
        show { PrimaryButton("Ingresar", EntryType.INCOME, onClick = {}) }

        compose.onNodeWithText("Ingresar").assertWidthIsEqualTo(355.dp).assertHeightIsAtLeast(48.dp).assert(hasRole(Role.Button))
    }

    @Test
    fun primaryButton_enabled_click_invokesCallback() {
        var clicks = 0
        show { PrimaryButton("Egresar", EntryType.EXPENSE, onClick = { clicks++ }) }

        compose.onNodeWithText("Egresar").performClick()

        assertEquals(1, clicks)
    }

    // --- TextButton ---

    @Test
    fun textButton_meets48dpTouchTarget_inBothDimensions() {
        show { TextButton("Ok", onClick = {}) }

        compose.onNodeWithText("Ok").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).assert(hasRole(Role.Button))
    }

    @Test
    fun textButton_withIcon_isExactly32dpWiderThanWithout() {
        show {
            Column {
                TextButton("Borrar", Tone.Expense, onClick = {}, modifier = Modifier.testTag("sin"))
                TextButton("Borrar", Tone.Expense, icon = R.drawable.ic_trash, onClick = {}, modifier = Modifier.testTag("con"))
            }
        }
        val without = compose.onNodeWithTag("sin").fetchSemanticsNode().size.width
        val with = compose.onNodeWithTag("con").fetchSemanticsNode().size.width

        // Mismo texto: el ícono de 24 dp más 8 dp de separación (DESIGN §6, variante «Ícono: Sí»).
        assertEquals(32 * context.resources.displayMetrics.density, (with - without).toFloat(), 1.5f)
    }

    @Test
    fun textButton_click_invokesCallback() {
        var clicks = 0
        show { TextButton("Cancelar", Tone.Neutral, onClick = { clicks++ }) }

        compose.onNodeWithText("Cancelar").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun textButton_disabled_isNotEnabled_andDoesNotCallOnClick() {
        var clicks = 0
        show { TextButton("Guardar", Tone.Income, onClick = { clicks++ }, enabled = false) }

        compose.onNodeWithText("Guardar").assertIsNotEnabled().performClick()

        assertEquals(0, clicks)
    }

    @Test
    fun textButton_enabledByDefault() {
        show { TextButton("Guardar", Tone.Income, onClick = {}) }

        compose.onNodeWithText("Guardar").assertIsEnabled()
    }

    @Test
    fun textButton_disabled_keepsThe48dpTouchTarget_andIsDimmed() {
        val p = palette(true)
        show { TextButton("Guardar", Tone.Income, onClick = {}, enabled = false) }

        compose.onNodeWithText("Guardar").assertHeightIsAtLeast(48.dp)
        // Deshabilitado deja de ser verde: no puede parecer una acción disponible.
        val node = compose.onNodeWithText("Guardar")
        val green = androidx.compose.ui.graphics.Color(p.income.red, p.income.green, p.income.blue)
        val pixels = node.captureToImage().toPixelMap()
        val hasFullGreen = (0 until pixels.width).any { x ->
            (0 until pixels.height).any { y ->
                val c = pixels[x, y]
                kotlin.math.abs(c.red - green.red) < 0.02f && kotlin.math.abs(c.green - green.green) < 0.02f && kotlin.math.abs(c.blue - green.blue) < 0.02f
            }
        }
        assertEquals("un botón deshabilitado no se dibuja en el verde pleno", false, hasFullGreen)
    }
}
