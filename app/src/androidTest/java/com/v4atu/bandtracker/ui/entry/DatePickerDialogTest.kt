package com.v4atu.bandtracker.ui.entry

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertCountEquals
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.domain.DateLabels
import com.v4atu.bandtracker.ui.ScreenTestBase
import com.v4atu.bandtracker.ui.theme.DarkPalette
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** DESIGN §7 «5.2 Selector de fecha»: cuadrícula propia, días futuros deshabilitados, 48 dp por celda. */
class DatePickerDialogTest : ScreenTestBase() {

    private val today = LocalDate.of(2026, 11, 6)
    private val locale = Locale.US

    private var confirmed: LocalDate? = null
    private var dismissed = 0

    private fun show(initial: LocalDate = today, firstDay: DayOfWeek = DayOfWeek.MONDAY) {
        confirmed = null
        dismissed = 0
        showScreen {
            DatePickerContent(
                initial = initial, today = today, locale = locale, firstDayOfWeek = firstDay,
                onConfirm = { confirmed = it }, onDismiss = { dismissed++ },
            )
        }
    }

    private val previousMonth get() = string(R.string.date_previous_month)
    private val nextMonth get() = string(R.string.date_next_month)
    private val accept get() = string(R.string.action_accept)
    private val cancel get() = string(R.string.action_cancel)

    private fun monthLabel(year: Int, month: Int) = DateLabels.month(YearMonth.of(year, month), locale)

    // --- Contenido ---

    @Test
    fun showsTheTitleTheMonthAndTheDaysOfNovember() {
        show()

        compose.onNodeWithText(string(R.string.entry_date)).assertIsDisplayed()
        compose.onNodeWithText(monthLabel(2026, 11)).assertIsDisplayed()
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.onNodeWithText("30").assertIsDisplayed()
        compose.onNodeWithText("31").assertDoesNotExist()
    }

    @Test
    fun showsTheWeekdayInitials_startingOnTheGivenDay() {
        show(firstDay = DayOfWeek.MONDAY)

        // M T W T F S S: dos martes/jueves → «T» aparece dos veces; lunes y miércoles no se repiten con «S».
        compose.onAllNodesWithText("T").assertCountEquals(2)
        compose.onAllNodesWithText("S").assertCountEquals(2)
        compose.onAllNodesWithText("M").assertCountEquals(1)
    }

    @Test
    fun theInitialDateIsTheSelectedOne() {
        show()

        compose.onNodeWithText("6").assertIsSelected()
        compose.onNodeWithText("5").assertIsNotSelected()
    }

    @Test
    fun anInitialDateInAnotherMonth_opensThatMonth() {
        show(initial = LocalDate.of(2026, 8, 14))

        compose.onNodeWithText(monthLabel(2026, 8)).assertIsDisplayed()
        compose.onNodeWithText("14").assertIsSelected()
        compose.onNodeWithText("31").assertIsDisplayed()
    }

    // --- Días futuros ---

    @Test
    fun futureDaysAreDisabled_todayAndThePastAreNot() {
        show()

        compose.onNodeWithText("7").assertIsNotEnabled()
        compose.onNodeWithText("30").assertIsNotEnabled()
        compose.onNodeWithText("6").assertIsEnabled()
        compose.onNodeWithText("1").assertIsEnabled()
    }

    @Test
    fun tappingAFutureDay_changesNothing() {
        show()

        compose.onNodeWithText("20").performClick()

        compose.onNodeWithText("6").assertIsSelected()
        compose.onNodeWithText("20").assertIsNotSelected()
    }

    @Test
    fun tappingAPastDay_selectsIt() {
        show()

        compose.onNodeWithText("3").performClick()

        compose.onNodeWithText("3").assertIsSelected()
        compose.onNodeWithText("6").assertIsNotSelected()
    }

    @Test
    fun everyDayHasTheFullDateAsItsDescription() {
        show()

        compose.onNodeWithContentDescription(DateLabels.full(LocalDate.of(2026, 11, 6), locale)).assertIsDisplayed()
        compose.onNodeWithContentDescription(DateLabels.full(LocalDate.of(2026, 11, 1), locale)).assertIsDisplayed()
    }

    // --- Navegación entre meses ---

    @Test
    fun inTheMonthOfToday_theNextMonthButtonIsDisabled_andThePreviousOneIsNot() {
        show()

        compose.onNodeWithContentDescription(nextMonth).assertIsNotEnabled()
        compose.onNodeWithContentDescription(previousMonth).assertIsEnabled()
    }

    @Test
    fun goingBack_showsThePreviousMonth_andEnablesNext() {
        show()

        compose.onNodeWithContentDescription(previousMonth).performClick()

        compose.onNodeWithText(monthLabel(2026, 10)).assertIsDisplayed()
        compose.onNodeWithText("31").assertIsDisplayed()
        compose.onNodeWithText("31").assertIsEnabled()
        compose.onNodeWithContentDescription(nextMonth).assertIsEnabled()
    }

    @Test
    fun goingBackAndForward_returnsToTheMonthOfToday() {
        show()

        compose.onNodeWithContentDescription(previousMonth).performClick()
        compose.onNodeWithContentDescription(nextMonth).performClick()

        compose.onNodeWithText(monthLabel(2026, 11)).assertIsDisplayed()
        compose.onNodeWithContentDescription(nextMonth).assertIsNotEnabled()
    }

    @Test
    fun theSelectionSurvivesNavigatingToAnotherMonth() {
        show()
        compose.onNodeWithText("3").performClick()

        compose.onNodeWithContentDescription(previousMonth).performClick()
        compose.onNodeWithContentDescription(nextMonth).performClick()

        compose.onNodeWithText("3").assertIsSelected()
    }

    // --- Aceptar y Cancelar ---

    @Test
    fun accept_returnsTheSelectedDate() {
        show()
        compose.onNodeWithText("3").performClick()

        compose.onNodeWithText(accept).performClick()

        assertEquals(LocalDate.of(2026, 11, 3), confirmed)
        assertEquals(0, dismissed)
    }

    @Test
    fun accept_canReturnADayFromAnotherMonth() {
        show()
        compose.onNodeWithContentDescription(previousMonth).performClick()
        compose.onNodeWithText("15").performClick()

        compose.onNodeWithText(accept).performClick()

        assertEquals(LocalDate.of(2026, 10, 15), confirmed)
    }

    @Test
    fun accept_withoutChangingAnything_returnsTheInitialDate() {
        show(initial = LocalDate.of(2026, 11, 2))

        compose.onNodeWithText(accept).performClick()

        assertEquals(LocalDate.of(2026, 11, 2), confirmed)
    }

    @Test
    fun cancel_dismissesWithoutReturningADate() {
        show()
        compose.onNodeWithText("3").performClick()

        compose.onNodeWithText(cancel).performClick()

        assertNull(confirmed)
        assertEquals(1, dismissed)
    }

    // --- Medidas de Penpot ---

    @Test
    fun dayCellsAre48dpSquare_withRowsAndColumnsEvery48dp() {
        show()

        val five = compose.onNodeWithText("5")
        val twelve = compose.onNodeWithText("12")
        val six = compose.onNodeWithText("6")

        assertDp("ancho de celda", 48f, (five.rightDp() - five.leftDp()))
        assertDp("alto de celda", 48f, five.heightDp())
        assertDp("paso entre semanas", 48f, twelve.topDp() - five.topDp())
        assertDp("paso entre columnas", 48f, six.leftDp() - five.leftDp())
    }

    @Test
    fun withMondayFirst_november1stIsInTheLastColumn() {
        show(firstDay = DayOfWeek.MONDAY)

        val distance = compose.onNodeWithText("1").leftDp() - compose.onNodeWithText("2").leftDp()

        assertDp("el 1 (domingo) queda 6 columnas a la derecha del 2 (lunes)", 6 * 48f, distance)
    }

    @Test
    fun withSundayFirst_november1stIsInTheFirstColumn() {
        show(firstDay = DayOfWeek.SUNDAY)

        val distance = compose.onNodeWithText("1").leftDp() - compose.onNodeWithText("2").leftDp()

        assertDp("el 1 (domingo) queda 1 columna a la izquierda del 2 (lunes)", -48f, distance)
    }

    @Test
    fun theSelectedDay_isAGreenCircleOf40dp_inTheMiddleOfItsCell() {
        show()
        val p = DarkPalette

        val cell = compose.onNodeWithText("6")
        // Penpot: círculo de 40 dp en la celda de 48 (4 dp de margen). Se mira por encima del número.
        assertColor("dentro del círculo", p.income, cell.pixelDp(24f, 8f))
        assertColor("fuera del círculo (esquina)", p.surface, cell.pixelDp(1.5f, 1.5f))
        assertColor("fuera del círculo (borde)", p.surface, cell.pixelDp(2f, 24f))
    }

    @Test
    fun theDialogIsAtMost379dpWide_withA16dpMarginPerSide() {
        show()
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText(string(R.string.entry_date))

        // Penpot: el diálogo mide ancho − 32 y el contenido está a 16 dp del borde del diálogo.
        assertDp("título a 16 + 16 dp del borde", 32f, title.leftDp() - root.left / density)
    }

    // --- Utilidades ---

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.pixelDp(xDp: Float, yDp: Float): Color {
        val pixels = captureToImage().toPixelMap()
        return pixels[(xDp * density).toInt().coerceIn(0, pixels.width - 1), (yDp * density).toInt().coerceIn(0, pixels.height - 1)]
    }

    private fun assertColor(label: String, expected: Color, actual: Color) {
        val ok = kotlin.math.abs(actual.red - expected.red) < 0.02f &&
            kotlin.math.abs(actual.green - expected.green) < 0.02f &&
            kotlin.math.abs(actual.blue - expected.blue) < 0.02f
        assertEquals("$label: esperado $expected, hay $actual", true, ok)
    }
}
