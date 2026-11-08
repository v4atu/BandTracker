package com.v4atu.bandtracker.ui.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.v4atu.bandtracker.R
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DESIGN §6 / Penpot «5.2 Ingreso»: el monto es el texto grande de 48 sp, centrado y del color del
 * tipo de registro, con el símbolo de la moneda a la izquierda. Se escribe directamente sobre él.
 */
class AmountFieldTest : ComponentTestBase() {

    private lateinit var state: TextFieldState

    private fun showField(
        currency: String = "USD", tone: Tone = Tone.Income, dark: Boolean = true,
        locale: Locale = Locale.US, initial: String = "",
    ) {
        show(dark) {
            state = rememberTextFieldState(initial)
            AmountField(state = state, currencyCode = currency, tone = tone, locale = locale)
        }
    }

    /** Pone el texto por código: sin foco no se dibuja el cursor (2 dp que parpadean y falsean la medida del centrado). */
    private fun setAmountWithoutFocus(text: String) {
        compose.runOnIdle { state.setTextAndPlaceCursorAtEnd(text) }
        compose.waitForIdle()
    }

    /** Lo que el usuario ve (y lee el lector de pantalla) en el campo: el texto ya con separadores. */
    private fun displayed(): String =
        compose.onNode(hasSetTextAction()).fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun typeAmount(text: String) = compose.onNode(hasSetTextAction()).performTextInput(text)

    private fun SemanticsNodeInteraction.hasPixelsNear(color: Color): Boolean {
        val pixels = captureToImage().toPixelMap()
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
            val p = pixels[x, y]
            if (kotlin.math.abs(p.red - color.red) < 0.02f && kotlin.math.abs(p.green - color.green) < 0.02f &&
                kotlin.math.abs(p.blue - color.blue) < 0.02f
            ) return true
        }
        return false
    }

    @Test
    fun empty_showsTheCurrencySymbolAndAZeroPlaceholder() {
        showField("USD")

        compose.onNodeWithText("$").assertIsDisplayed()
        compose.onNodeWithText("0.00").assertIsDisplayed()
    }

    @Test
    fun aCurrencyWithoutDecimals_showsAPlainZero() {
        showField("JPY")

        compose.onNodeWithText("0").assertIsDisplayed()
        compose.onNodeWithText("0.00").assertDoesNotExist()
    }

    @Test
    fun typing_replacesThePlaceholder_andUpdatesTheState() {
        showField()

        typeAmount("12.5")

        compose.runOnIdle { assertEquals("12.5", state.text.toString()) }
        compose.onNodeWithText("0.00").assertDoesNotExist()
        compose.onNodeWithText("$").assertIsDisplayed()
    }

    @Test
    fun lettersAndSignsAreRejected() {
        showField()

        typeAmount("12")
        typeAmount("abc")
        typeAmount("-")

        compose.runOnIdle { assertEquals("12", state.text.toString()) }
    }

    @Test
    fun moreDecimalsThanTheCurrencyAllows_areRejected() {
        showField("USD")

        typeAmount("1.25")
        typeAmount("9")

        compose.runOnIdle { assertEquals("1.25", state.text.toString()) }
    }

    @Test
    fun aCurrencyWithoutDecimals_rejectsTheSeparator() {
        showField("JPY")

        typeAmount("100")
        typeAmount(".")

        compose.runOnIdle { assertEquals("100", state.text.toString()) }
    }

    @Test
    fun heightIsOneLineOfThe48spAmountStyle() {
        showField()

        // Penpot: «Monto» mide 58 dp de alto (48 sp × 1.2).
        compose.onNode(hasSetTextAction()).assertHeightWithin(58f)
    }

    @Test
    fun theWholeWidthIsTheTouchTarget_notJustTheNumber() {
        showField()

        compose.onNode(hasSetTextAction()).assertWidthIsEqualTo(355.dp)
    }

    @Test
    fun theSymbolAndAmountAreCenteredInTheAvailableWidth() {
        showField()

        // Árbol sin fusionar: el campo de texto fusiona a sus hijos y «$» y «0.00» darían el mismo nodo.
        val symbol = compose.onNodeWithText("$", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val amount = compose.onNodeWithText("0.00", useUnmergedTree = true).getUnclippedBoundsInRoot()

        val leftGap = symbol.left.value
        val rightGap = 355f - amount.right.value
        assertEquals("margen izquierdo vs. derecho (dp)", leftGap, rightGap, 3f)
        assertTrue("el símbolo va a la izquierda del monto", symbol.right <= amount.left)
    }

    /** Márgenes visibles (dp) a izquierda y derecha: columnas de píxeles que difieren del fondo. */
    private fun SemanticsNodeInteraction.visibleMarginsDp(background: Color): Pair<Float, Float> {
        val pixels = captureToImage().toPixelMap()
        fun differs(x: Int) = (0 until pixels.height).any { y ->
            val p = pixels[x, y]
            kotlin.math.abs(p.red - background.red) > 0.05f || kotlin.math.abs(p.green - background.green) > 0.05f ||
                kotlin.math.abs(p.blue - background.blue) > 0.05f
        }
        val first = (0 until pixels.width).first { differs(it) }
        val last = (pixels.width - 1 downTo 0).first { differs(it) }
        val density = context.resources.displayMetrics.density
        return first / density to (pixels.width - 1 - last) / density
    }

    @Test
    fun withTypedText_theSymbolAndAmountStayCenteredTogether() {
        showField()
        setAmountWithoutFocus("1234.56")

        val (left, right) = compose.onNode(hasSetTextAction()).visibleMarginsDp(palette(true).bg)

        // Sobra espacio a los dos lados y es el mismo (±3 dp: el cursor ocupa ~2 dp al final del texto).
        assertTrue("debe sobrar espacio a la izquierda ($left dp)", left > 20f)
        assertEquals("margen izquierdo vs. derecho (dp)", left, right, 3f)
    }

    @Test
    fun incomeIsDrawnInGreen() {
        val p = palette(true)

        showField(tone = Tone.Income)
        assertTrue(compose.onNode(hasSetTextAction()).hasPixelsNear(p.income))
        assertFalse(compose.onNode(hasSetTextAction()).hasPixelsNear(p.expense))
    }

    @Test
    fun expenseIsDrawnInRed() {
        val p = palette(true)

        showField(tone = Tone.Expense)

        assertTrue(compose.onNode(hasSetTextAction()).hasPixelsNear(p.expense))
        assertFalse(compose.onNode(hasSetTextAction()).hasPixelsNear(p.income))
    }

    @Test
    fun theFieldIsLabelledForScreenReaders() {
        showField()

        compose.onNode(hasSetTextAction() and hasContentDescription(string(R.string.entry_amount))).assertIsDisplayed()
    }

    // --- Formato en vivo: separadores de miles y decimal de la región ---

    @Test
    fun typing_showsThousandsSeparators_whileTheSavedTextStaysPlain() {
        showField()

        typeAmount("1234567.5")
        compose.waitForIdle()

        assertEquals("1,234,567.5", displayed())
        compose.runOnIdle { assertEquals("1234567.5", state.text.toString()) }
    }

    @Test
    fun typing_oneDigitAtATime_regroupsAsTheNumberGrows() {
        showField()

        listOf("1" to "1", "2" to "12", "3" to "123", "4" to "1,234", "5" to "12,345", "6" to "123,456", "7" to "1,234,567").forEach { (digit, shown) ->
            typeAmount(digit)
            compose.waitForIdle()
            assertEquals(shown, displayed())
        }
    }

    @Test
    fun inARegionWithCommaDecimals_theSeparatorsAreSwapped() {
        showField(locale = Locale.forLanguageTag("es-CO"))

        typeAmount("1234567.5")
        compose.waitForIdle()

        assertEquals("1.234.567,5", displayed())
        compose.runOnIdle { assertEquals("1234567.5", state.text.toString()) }
    }

    @Test
    fun aValueLoadedIntoTheField_isShownGroupedFromTheStart() {
        showField(initial = "1500000")
        compose.waitForIdle()

        assertEquals("1,500,000", displayed())
    }

    @Test
    fun aCurrencyWithoutDecimals_alsoGroupsThousands() {
        showField("JPY", locale = Locale.JAPAN)

        typeAmount("1234567")
        compose.waitForIdle()

        assertEquals("1,234,567", displayed())
    }

    @Test
    fun theDecimalPartIsNeverGrouped() {
        showField()

        typeAmount("1234.56")
        compose.waitForIdle()

        assertEquals("1,234.56", displayed())
    }

    @Test
    fun formattingDoesNotBreakTheInputFilter_aThirdDecimalIsStillRejected() {
        showField()

        typeAmount("1234.56")
        typeAmount("7")
        compose.waitForIdle()

        compose.runOnIdle { assertEquals("1234.56", state.text.toString()) }
        assertEquals("1,234.56", displayed())
    }

    @Test
    fun theSymbolAndTheGroupedAmount_stayCenteredTogether() {
        showField()
        setAmountWithoutFocus("1234567.5")

        val (left, right) = compose.onNode(hasSetTextAction()).visibleMarginsDp(palette(true).bg)

        assertTrue("debe sobrar espacio a la izquierda ($left dp)", left > 5f)
        assertEquals("margen izquierdo vs. derecho (dp)", left, right, 3f)
    }
}
