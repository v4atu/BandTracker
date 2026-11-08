package com.v4atu.bandtracker.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/** El monto se muestra con separadores de miles mientras se escribe (SPEC §5.2); el texto guardado sigue siendo plano. */
class AmountFormatTest {

    private val us = Locale.US // miles «,» y decimal «.»
    private val es = Locale.forLanguageTag("es-CO") // miles «.» y decimal «,»

    // --- Dónde van los separadores de miles ---

    @Test
    fun groupSeparatorIndexes_areEveryThreeDigitsFromTheRight_inTheIntegerPart() {
        assertEquals(listOf(1, 4), AmountFormat.groupSeparatorIndexes("1234567"))
        assertEquals(listOf(3), AmountFormat.groupSeparatorIndexes("123456"))
        assertEquals(listOf(1), AmountFormat.groupSeparatorIndexes("1234"))
        assertEquals(listOf(2, 5), AmountFormat.groupSeparatorIndexes("12345678"))
    }

    @Test
    fun groupSeparatorIndexes_areEmpty_forThreeDigitsOrFewer() {
        assertEquals(emptyList<Int>(), AmountFormat.groupSeparatorIndexes(""))
        assertEquals(emptyList<Int>(), AmountFormat.groupSeparatorIndexes("7"))
        assertEquals(emptyList<Int>(), AmountFormat.groupSeparatorIndexes("123"))
    }

    @Test
    fun groupSeparatorIndexes_ignoreTheDecimalPart_whicheverSeparatorWasTyped() {
        assertEquals(listOf(1), AmountFormat.groupSeparatorIndexes("1234.56789"))
        assertEquals(listOf(1), AmountFormat.groupSeparatorIndexes("1234,56789"))
        assertEquals(emptyList<Int>(), AmountFormat.groupSeparatorIndexes("123.4567"))
        assertEquals(emptyList<Int>(), AmountFormat.groupSeparatorIndexes(".5"))
    }

    @Test
    fun decimalIndex_isThePositionOfTheTypedSeparator_orMinusOne() {
        assertEquals(4, AmountFormat.decimalIndex("1234.5"))
        assertEquals(4, AmountFormat.decimalIndex("1234,5"))
        assertEquals(0, AmountFormat.decimalIndex(".5"))
        assertEquals(-1, AmountFormat.decimalIndex("1234"))
        assertEquals(-1, AmountFormat.decimalIndex(""))
    }

    // --- El texto completo ---

    @Test
    fun display_groupsThousands_withTheSeparatorsOfTheRegion() {
        assertEquals("1,234,567", AmountFormat.display("1234567", us))
        assertEquals("1.234.567", AmountFormat.display("1234567", es))
    }

    @Test
    fun display_showsTheDecimalSeparatorOfTheRegion_whicheverWasTyped() {
        assertEquals("1,234,567.5", AmountFormat.display("1234567.5", us))
        assertEquals("1,234,567.5", AmountFormat.display("1234567,5", us))
        assertEquals("1.234.567,5", AmountFormat.display("1234567.5", es))
        assertEquals("1.234.567,5", AmountFormat.display("1234567,5", es))
    }

    @Test
    fun display_keepsWhatIsBeingTyped_atEveryStep() {
        assertEquals("", AmountFormat.display("", us))
        assertEquals(".", AmountFormat.display(".", us))
        assertEquals("0.", AmountFormat.display("0.", us))
        assertEquals("1,234.", AmountFormat.display("1234.", us))
        assertEquals(".5", AmountFormat.display(".5", us))
        assertEquals("123", AmountFormat.display("123", us))
    }

    @Test
    fun display_neverChangesTheDigits_onlyAddsSeparators() {
        listOf("0", "7", "1234", "9876543210", "1000000.05", "5000,5").forEach { raw ->
            val shown = AmountFormat.display(raw, us)
            assertEquals(raw.filter { it.isDigit() }, shown.filter { it.isDigit() })
        }
    }

    @Test
    fun theTextSavedIsStillParsedByMoney_regardlessOfTheDisplay() {
        // La conversión usa el texto plano: el formato de pantalla nunca llega a Money.parse.
        assertEquals(123_456_750L, Money.parse("1234567.5", "USD"))
        assertEquals(123_456_750L, Money.parse("1234567,5", "USD"))
    }
}
