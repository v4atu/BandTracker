package com.v4atu.bandtracker.ui.components

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** DESIGN §6: SavedSnackbar (aviso) y ConfirmDialog. SPEC §5.2 (aviso breve) y §5.3 (borrado definitivo). */
class FeedbackComponentsTest : ComponentTestBase() {

    // --- SavedSnackbar ---

    @Test
    fun savedSnackbar_showsItsMessage() {
        show { SavedSnackbar("Ingreso guardado") }

        compose.onNodeWithText("Ingreso guardado").assertIsDisplayed()
    }

    @Test
    fun savedSnackbar_usesInvertedColors_darkInkOnLightBackground() {
        val p = palette(true)
        show { SavedSnackbar("Egreso guardado") }
        val node = compose.onNodeWithText("Egreso guardado")

        // Fondo = color.text.primary (colores invertidos, DESIGN §6).
        node.assertColorAt(p.textPrimary, 177f, 3f, "fondo del aviso")
        // Marca de 24 dp a (16,14): su primer tramo pasa por (14.5, 11.5) del viewport, en color.bg.
        node.assertColorAt(p.bg, 16f + 14.5f, 14f + 11.5f, "marca del aviso")
    }

    @Test
    fun savedSnackbar_isFullWidthAndAboutFiftyDpTall() {
        show { SavedSnackbar("Ingreso guardado") }

        compose.onNodeWithText("Ingreso guardado").assertWidthIsEqualTo(355.dp).assertHeightIsAtLeast(48.dp)
    }

    // --- ConfirmDialog ---

    @Test
    fun confirmDialog_showsTitleMessageAndBothActions() {
        show {
            ConfirmDialog(
                title = "¿Borrar este registro?",
                message = "Esta acción es definitiva y no se puede deshacer.",
                confirmText = "Borrar",
                dismissText = "Cancelar",
                destructive = true,
                onConfirm = {},
                onDismiss = {},
            )
        }

        compose.onNodeWithText("¿Borrar este registro?").assertIsDisplayed()
        compose.onNodeWithText("Esta acción es definitiva y no se puede deshacer.").assertIsDisplayed()
        compose.onNodeWithText("Borrar").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").assertIsDisplayed()
    }

    @Test
    fun confirmDialog_confirmAndDismiss_callTheirOwnCallbacksOnly() {
        var confirmed = 0
        var dismissed = 0
        show {
            ConfirmDialog("Título", "Mensaje", "Aceptar", "Cancelar", destructive = false, onConfirm = { confirmed++ }, onDismiss = { dismissed++ })
        }

        compose.onNodeWithText("Aceptar").performClick()
        assertEquals(1 to 0, confirmed to dismissed)

        compose.onNodeWithText("Cancelar").performClick()
        assertEquals(1 to 1, confirmed to dismissed)
    }

    @Test
    fun confirmDialog_actionsMeetThe48dpTouchTarget() {
        show { ConfirmDialog("Título", "Mensaje", "Aceptar", "Cancelar", destructive = true, onConfirm = {}, onDismiss = {}) }

        compose.onNodeWithText("Aceptar").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Cancelar").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun confirmDialog_actionRowIs48dpTallAndCancelComesBeforeConfirm() {
        show { ConfirmDialog("Título", "Mensaje", "Aceptar", "Cancelar", destructive = false, onConfirm = {}, onDismiss = {}) }

        val cancel = compose.onNodeWithText("Cancelar").fetchSemanticsNode().boundsInRoot
        val confirm = compose.onNodeWithText("Aceptar").fetchSemanticsNode().boundsInRoot
        assertEquals(true, cancel.right <= confirm.left)
        compose.onNodeWithText("Aceptar").assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun confirmDialog_withAWarning_showsItBetweenTheMessageAndTheActions() {
        show {
            ConfirmDialog(
                "¿Borrar «Efectivo»?", "Se eliminarán también todos sus registros.", "Borrar cuenta", "Cancelar",
                destructive = true, onConfirm = {}, onDismiss = {}, warning = "Se perderán 128 registros",
            )
        }

        val message = compose.onNodeWithText("Se eliminarán también todos sus registros.")
        val warning = compose.onNodeWithText("Se perderán 128 registros")
        val cancel = compose.onNodeWithText("Cancelar")
        warning.assertIsDisplayed()
        assertEquals("la advertencia va bajo el mensaje", true, warning.fetchSemanticsNode().boundsInRoot.top >= message.fetchSemanticsNode().boundsInRoot.bottom - 1f)
        assertEquals("y sobre las acciones", true, warning.fetchSemanticsNode().boundsInRoot.bottom <= cancel.fetchSemanticsNode().boundsInRoot.top + 1f)
    }

    @Test
    fun confirmDialog_withoutAWarning_showsNoExtraLine() {
        show { ConfirmDialog("Título", "Mensaje", "Aceptar", "Cancelar", destructive = false, onConfirm = {}, onDismiss = {}) }

        compose.onNodeWithText("Se perderán 128 registros").assertDoesNotExist()
    }

    // --- InfoDialog (un solo botón: error de importación, resumen) ---

    @Test
    fun infoDialog_showsTitleMessageAndASingleAction() {
        show { InfoDialog("No se pudo importar", "El archivo no es válido.", "Aceptar", onDismiss = {}) }

        compose.onNodeWithText("No se pudo importar").assertIsDisplayed()
        compose.onNodeWithText("El archivo no es válido.").assertIsDisplayed()
        compose.onNodeWithText("Aceptar").assertIsDisplayed()
    }

    @Test
    fun infoDialog_theActionDismisses_andMeetsThe48dpTouchTarget() {
        var dismissed = 0
        show { InfoDialog("Título", "Mensaje", "Aceptar", onDismiss = { dismissed++ }) }

        compose.onNodeWithText("Aceptar").assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(1, dismissed)
    }
}
