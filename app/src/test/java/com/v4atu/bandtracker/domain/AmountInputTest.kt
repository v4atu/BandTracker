package com.v4atu.bandtracker.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Qué texto admite el campo Monto mientras se escribe (SPEC §5.2): decimales según la moneda. */
class AmountInputTest {

    @Test
    fun emptyText_isAccepted_soTheFieldCanBeCleared() {
        assertTrue(AmountInput.accepts("", fractionDigits = 2))
    }

    @Test
    fun digitsOnly_areAccepted() {
        assertTrue(AmountInput.accepts("1500", fractionDigits = 2))
        assertTrue(AmountInput.accepts("1500", fractionDigits = 0))
    }

    @Test
    fun oneSeparator_dotOrComma_isAccepted_whenTheCurrencyHasDecimals() {
        assertTrue(AmountInput.accepts("12.5", fractionDigits = 2))
        assertTrue(AmountInput.accepts("12,5", fractionDigits = 2))
        assertTrue(AmountInput.accepts("12.", fractionDigits = 2))
        assertTrue(AmountInput.accepts(".5", fractionDigits = 2))
    }

    @Test
    fun moreDecimalsThanTheCurrencyAllows_isRejected() {
        assertTrue(AmountInput.accepts("12.50", fractionDigits = 2))
        assertFalse(AmountInput.accepts("12.505", fractionDigits = 2))
        assertFalse(AmountInput.accepts("0.001", fractionDigits = 2))
    }

    @Test
    fun aCurrencyWithoutDecimals_rejectsAnySeparator() {
        assertFalse(AmountInput.accepts("100.", fractionDigits = 0))
        assertFalse(AmountInput.accepts("100,5", fractionDigits = 0))
    }

    @Test
    fun twoSeparators_areRejected() {
        assertFalse(AmountInput.accepts("1.2.3", fractionDigits = 2))
        assertFalse(AmountInput.accepts("1,2.3", fractionDigits = 3))
    }

    @Test
    fun lettersSignsAndSpaces_areRejected() {
        assertFalse(AmountInput.accepts("12a", fractionDigits = 2))
        assertFalse(AmountInput.accepts("-5", fractionDigits = 2))
        assertFalse(AmountInput.accepts("+5", fractionDigits = 2))
        assertFalse(AmountInput.accepts("1 000", fractionDigits = 2))
    }

    @Test
    fun nonAsciiDigits_areRejected() {
        assertFalse(AmountInput.accepts("١٢٣", fractionDigits = 2))
    }

    @Test
    fun everyTextItAccepts_andIsNotJustASeparator_canBeParsedByMoney() {
        listOf("0", "12", "12.5", "12,50", ".5", "12.").forEach { text ->
            assertTrue("«$text» debe aceptarse", AmountInput.accepts(text, 2))
            assertTrue("«$text» debe poder convertirse", Money.parse(text, "USD") != null)
        }
    }
}
