package com.v4atu.bandtracker.ui.accounts

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.domain.CurrencyOption
import com.v4atu.bandtracker.domain.NameCheck
import com.v4atu.bandtracker.ui.ScreenTestBase
import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC §5.4 y DESIGN §7: tableros «5.4 Crear cuenta» y «5.4 Crear cuenta · nombre duplicado». */
class CreateAccountScreenTest : ScreenTestBase() {

    private val results = listOf(
        CurrencyOption("COP", "Peso colombiano"),
        CurrencyOption("USD", "Dólar estadounidense"),
        CurrencyOption("EUR", "Euro"),
    )

    private fun state(
        nameCheck: NameCheck = NameCheck.BLANK,
        selected: String? = null,
        results: List<CurrencyOption> = this.results,
    ) = CreateAccountUiState(loaded = true, nameCheck = nameCheck, results = results, selectedCurrency = selected)

    private class Calls {
        val currencies = mutableListOf<String>()
        var create = 0
    }

    private lateinit var name: TextFieldState
    private lateinit var query: TextFieldState
    private var topInsetDp = 0f
    private var bottomInsetDp = 0f

    private fun show(state: CreateAccountUiState, calls: Calls = Calls()) {
        showScreen {
            val metrics = LocalDensity.current
            topInsetDp = WindowInsets.safeDrawing.getTop(metrics) / metrics.density
            bottomInsetDp = WindowInsets.safeDrawing.getBottom(metrics) / metrics.density
            name = rememberTextFieldState()
            query = rememberTextFieldState()
            CreateAccountScreen(
                state = state, name = name, query = query,
                onSelectCurrency = { calls.currencies += it }, onCreate = { calls.create++ },
            )
        }
    }

    private val create get() = string(R.string.account_create)

    // --- Contenido ---

    @Test
    fun showsTheTitle_theFields_theNote_andTheButton() {
        show(state())

        compose.onNodeWithText(string(R.string.account_new_title)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.account_name_label)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.account_name_placeholder)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.account_currency_label)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.account_currency_search)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.account_currency_note)).assertIsDisplayed()
        compose.onNodeWithText(create).assertIsDisplayed()
    }

    @Test
    fun theCurrencyResults_showCodeAndName() {
        show(state())

        results.forEach {
            compose.onNodeWithText(it.code).assertIsDisplayed()
            compose.onNodeWithText(it.name).assertIsDisplayed()
        }
    }

    @Test
    fun tappingACurrency_asksToSelectIt() {
        val calls = Calls()
        show(state(), calls)

        compose.onNodeWithText("USD").performClick()

        assertEquals(listOf("USD"), calls.currencies)
    }

    @Test
    fun theSelectedCurrency_isMarked_theOthersAreNot() {
        show(state(selected = "COP"))

        compose.onNodeWithText("COP").assertIsSelected()
        compose.onNodeWithText("USD").assertIsNotSelected()
    }

    @Test
    fun whatIsTypedInTheFields_goesToTheTextStates() {
        show(state())

        compose.onNodeWithText(string(R.string.account_name_label)).performTextInput("Efectivo")
        compose.onNodeWithText(string(R.string.account_currency_label)).performTextInput("cop")

        compose.runOnIdle {
            assertEquals("Efectivo", name.text.toString())
            assertEquals("cop", query.text.toString())
        }
    }

    // --- Nombre duplicado ---

    @Test
    fun aDuplicateName_showsTheErrorMessage() {
        show(state(nameCheck = NameCheck.DUPLICATE))

        compose.onNodeWithText(string(R.string.account_name_duplicate)).assertIsDisplayed()
    }

    @Test
    fun aBlankOrValidName_showsNoErrorMessage() {
        show(state(nameCheck = NameCheck.BLANK))
        compose.onNodeWithText(string(R.string.account_name_duplicate)).assertDoesNotExist()
    }

    @Test
    fun aValidName_showsNoErrorMessage() {
        show(state(nameCheck = NameCheck.OK))
        compose.onNodeWithText(string(R.string.account_name_duplicate)).assertDoesNotExist()
    }

    // --- Botón Crear ---

    @Test
    fun createIsDisabled_untilNameAndCurrencyAreValid() {
        val calls = Calls()
        show(state(nameCheck = NameCheck.OK, selected = null), calls)

        compose.onNodeWithText(create).assertIsNotEnabled().performClick()

        assertEquals(0, calls.create)
    }

    @Test
    fun createIsDisabled_withADuplicateName() {
        show(state(nameCheck = NameCheck.DUPLICATE, selected = "COP"))

        compose.onNodeWithText(create).assertIsNotEnabled()
    }

    @Test
    fun createIsEnabled_withAValidNameAndACurrency_andAsksToCreate() {
        val calls = Calls()
        show(state(nameCheck = NameCheck.OK, selected = "COP"), calls)

        compose.onNodeWithText(create).assertIsEnabled().performClick()

        assertEquals(1, calls.create)
    }

    // --- Medidas de Penpot ---

    @Test
    fun fieldsAre56dpTall_with16dpBetweenThem() {
        show(state())

        val nameField = compose.onNodeWithText(string(R.string.account_name_label))
        val currencyField = compose.onNodeWithText(string(R.string.account_currency_label))

        assertDp("alto de Nombre", 56f, nameField.heightDp())
        assertDp("alto de Moneda", 56f, currencyField.heightDp())
        assertDp("hueco Nombre→Moneda", 16f, currencyField.topDp() - nameField.bottomDp())
    }

    @Test
    fun theErrorMessage_sits16dpBelowTheName_andPushesTheCurrencyDown() {
        show(state(nameCheck = NameCheck.DUPLICATE))

        val nameField = compose.onNodeWithText(string(R.string.account_name_label))
        val message = compose.onNodeWithText(string(R.string.account_name_duplicate))
        val currencyField = compose.onNodeWithText(string(R.string.account_currency_label))

        // Penpot: mensaje de 12 sp (caja de 15 dp) a 16 dp del campo; «Moneda» a 16 dp del mensaje.
        assertDp("campo → centro del mensaje", 16f + 7.5f, message.centerYDp() - nameField.bottomDp(), tolerance = 2f)
        assertDp("centro del mensaje → Moneda", 7.5f + 16f, currencyField.topDp() - message.centerYDp(), tolerance = 2f)
    }

    @Test
    fun theResultsPanel_rowsAre48dp_startingBelowTheCurrencyField_andTheNoteFollows() {
        show(state())

        val currencyField = compose.onNodeWithText(string(R.string.account_currency_label))
        val first = compose.onNodeWithText("COP")
        val last = compose.onNodeWithText("EUR")
        val note = compose.onNodeWithText(string(R.string.account_currency_note))

        // Penpot: panel a 16 dp del campo, con 4 dp de margen; filas de 48 dp; nota a 16 dp del panel.
        assertDp("alto de una fila", 48f, first.heightDp())
        assertDp("Moneda → primera fila", 16f + 4f, first.topDp() - currencyField.bottomDp())
        assertDp("última fila → centro de la nota", 4f + 16f + 7.5f, note.centerYDp() - last.bottomDp(), tolerance = 2f)
    }

    @Test
    fun theButtonIs53dpTall_andSitsAtTheBottom() {
        show(state(nameCheck = NameCheck.OK, selected = "COP"))

        val button = compose.onNodeWithText(create)
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

        assertDp("alto del botón", 53f, button.heightDp())
        assertDp("margen inferior (32 + barra del sistema)", 32f + bottomInsetDp, root.bottom / density - button.bottomDp())
    }

    @Test
    fun theTitleIsCenteredAtTheTop() {
        show(state())

        val title = compose.onNodeWithText(string(R.string.account_new_title))
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

        assertDp("centro vertical del título", 38f + topInsetDp + 22f, title.centerYDp())
        assertDp("centrado", root.width / density / 2, (title.leftDp() + title.rightDp()) / 2)
    }
}
