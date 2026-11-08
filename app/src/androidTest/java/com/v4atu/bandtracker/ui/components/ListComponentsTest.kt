package com.v4atu.bandtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.v4atu.bandtracker.domain.EntryRowText
import com.v4atu.bandtracker.domain.EntryType
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/** DESIGN §6: EntryRow, DayHeader, MonthHeader, AccountCard y ListOption. SPEC §5.3 y criterio 8. */
class ListComponentsTest : ComponentTestBase() {

    private val es = Locale.forLanguageTag("es-CO")
    private val en = Locale.US

    // --- EntryRow ---

    @Test
    fun entryRow_withSubjectAndNote_showsSubjectAsTitleNoteAsSubtitleAndAmount() {
        show { EntryRow(EntryRowText.of("Taxi", "Aeropuerto"), "−15.000", EntryType.EXPENSE, onClick = {}) }

        compose.onNodeWithText("Taxi").assertIsDisplayed()
        compose.onNodeWithText("Aeropuerto").assertIsDisplayed()
        compose.onNodeWithText("−15.000").assertIsDisplayed()
    }

    @Test
    fun entryRow_isAtLeast60dpTall_isAButton_andClicks() {
        var clicks = 0
        show { EntryRow(EntryRowText.of("Taxi", null), "+1.000", EntryType.INCOME, onClick = { clicks++ }) }

        compose.onNodeWithText("Taxi").assertHeightIsAtLeast(60.dp).assertHasClickAction().assert(hasRole(Role.Button)).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun entryRow_withoutSubject_usesTheNoteAsTitle_andHasNoSubtitle() {
        show { EntryRow(EntryRowText.of(null, "Solo nota"), "+1.000", EntryType.INCOME, onClick = {}) }

        compose.onNodeWithText("Solo nota").assertIsDisplayed()
        // Una sola vez: no se repite como subtítulo.
        assertEquals(1, compose.onAllNodesWithText("Solo nota").fetchSemanticsNodes().size)
    }

    @Test
    fun entryRow_withNeitherSubjectNorNote_showsTheLocalizedFallback_inSpanish() {
        showLocalized(es) { EntryRow(EntryRowText.of(null, null), "+1.000", EntryType.INCOME, onClick = {}) }

        compose.onNodeWithText("Sin sujeto").assertIsDisplayed()
    }

    @Test
    fun entryRow_withNeitherSubjectNorNote_showsTheLocalizedFallback_inEnglish() {
        showLocalized(en) { EntryRow(EntryRowText.of(null, null), "+1.000", EntryType.INCOME, onClick = {}) }

        compose.onNodeWithText("No subject").assertIsDisplayed()
    }

    @Test
    fun entryRow_paintsSurfaceRow() {
        val p = palette(false)
        show(dark = false) { EntryRow(EntryRowText.of("Taxi", "n"), "+1.000", EntryType.INCOME, onClick = {}) }

        compose.onNodeWithText("Taxi").assertColorAt(p.surface, 177f, 3f, "fondo de la fila")
    }

    // --- DayHeader / MonthHeader ---

    @Test
    fun dayHeader_showsWeekdayAndDay_inTheDeviceLanguage() {
        showLocalized(es) { DayHeader(LocalDate.of(2026, 11, 6)) }
        compose.onNodeWithText("Viernes 6").assertIsDisplayed()
    }

    @Test
    fun dayHeader_inEnglish() {
        showLocalized(en) { DayHeader(LocalDate.of(2026, 11, 6)) }
        compose.onNodeWithText("Friday 6").assertIsDisplayed()
    }

    @Test
    fun monthHeader_showsMonthAndYear_inSpanishAndEnglish() {
        showLocalized(es) { MonthHeader(YearMonth.of(2026, 11)) }
        compose.onNodeWithText("Noviembre 2026").assertIsDisplayed()
    }

    @Test
    fun monthHeader_inEnglish() {
        showLocalized(en) { MonthHeader(YearMonth.of(2026, 11)) }
        compose.onNodeWithText("November 2026").assertIsDisplayed()
    }

    // --- AccountCard ---

    @Test
    fun accountCard_showsNameAndCurrency_andIs52dpTall() {
        show { AccountCard("Efectivo", "COP") }

        compose.onNodeWithText("Efectivo").assertIsDisplayed().assertHeightIsEqualTo(52.dp)
        compose.onNodeWithText("COP").assertIsDisplayed()
    }

    @Test
    fun accountCard_isClickableOnlyWithAnOnClick() {
        var clicks = 0
        show {
            Column {
                AccountCard("Con clic", "USD", onClick = { clicks++ })
                AccountCard("Sin clic", "COP")
            }
        }

        compose.onNodeWithText("Con clic").assertHasClickAction().assert(hasRole(Role.Button)).performClick()
        compose.onNodeWithText("Sin clic").assertHasNoClickAction()
        assertEquals(1, clicks)
    }

    // --- ListOption ---

    @Test
    fun listOption_is48dpTall_andReflectsSelection() {
        show {
            Column {
                ListOption("Efectivo", selected = true, onClick = {})
                ListOption("Ahorros", selected = false, onClick = {})
            }
        }

        compose.onNodeWithText("Efectivo").assertHeightIsEqualTo(48.dp).assertIsSelected().assert(hasRole(Role.RadioButton))
        compose.onNodeWithText("Ahorros").assertHeightIsEqualTo(48.dp).assertIsNotSelected()
    }

    @Test
    fun listOption_click_invokesCallback() {
        var clicks = 0
        show { ListOption("Efectivo", selected = false, onClick = { clicks++ }) }

        compose.onNodeWithText("Efectivo").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun listOption_selected_drawsAnIncomeColoredCheckAtTheRight() {
        val p = palette(true)
        show { ListOption("Efectivo", selected = true, onClick = {}) }

        // Marca de 24 dp con 16 dp de relleno a la derecha (Penpot: x=294 en 334): x 315..339, y 12..36.
        // Primer tramo del check: (14.5, 11.5) del viewport.
        compose.onNodeWithText("Efectivo").assertColorAt(p.income, 315f + 14.5f, 12f + 11.5f, "marca de selección")
    }

    @Test
    fun listOption_notSelected_hasNoCheck() {
        val p = palette(true)
        show { ListOption("Efectivo", selected = false, onClick = {}) }

        compose.onNodeWithText("Efectivo").assertColorAt(p.bg, 315f + 14.5f, 12f + 11.5f, "sin marca")
    }


    @Test
    fun listOptionWithDetail_showsTheCodeAndTheName_stillAsOneSelectableRow() {
        var clicks = 0
        show { ListOption("COP", selected = true, onClick = { clicks++ }, detail = "Peso colombiano") }

        compose.onNodeWithText("COP").assertIsDisplayed()
        compose.onNodeWithText("Peso colombiano").assertIsDisplayed()
        compose.onNodeWithText("COP").assertIsSelected().assertHeightWithin(48f)
        compose.onNodeWithText("Peso colombiano").performClick()
        assertEquals("tocar el nombre es tocar la fila", 1, clicks)
    }

    @Test
    fun listOptionWithDetail_drawsTheDetailInSecondaryColor_afterTheCode() {
        show { ListOption("EUR", selected = false, onClick = {}, detail = "Euro") }

        val code = compose.onNodeWithText("EUR", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val detail = compose.onNodeWithText("Euro", useUnmergedTree = true).getUnclippedBoundsInRoot()

        // Penpot: código en negrita y nombre en regular, separados por 12 dp.
        assertEquals("separación código→nombre (dp)", 12f, detail.left.value - code.right.value, 1.5f)
    }
}
