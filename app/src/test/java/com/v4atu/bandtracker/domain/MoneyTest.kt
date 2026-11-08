package com.v4atu.bandtracker.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC §4 (amountMinor entero, decimales por moneda) y §8 criterio 6. */
class MoneyTest {

    // --- Decimales por moneda (java.util.Currency, ISO 4217) ---

    @Test
    fun fractionDigits_followsIso4217() {
        assertEquals(2, Money.fractionDigits("USD"))
        assertEquals(2, Money.fractionDigits("COP"))
        assertEquals(0, Money.fractionDigits("JPY"))
    }

    // --- Parseo: texto del campo -> unidad mínima ---

    @Test
    fun parse_usdWithDecimals() {
        assertEquals(1050L, Money.parse("10.50", "USD"))
        assertEquals(1050L, Money.parse("10,50", "USD"))
    }

    @Test
    fun parse_integerWithoutSeparatorScalesByCurrency() {
        assertEquals(1000L, Money.parse("10", "USD"))
        assertEquals(10L, Money.parse("10", "JPY"))
    }

    @Test
    fun parse_fewerDecimalsThanAllowedIsPadded() {
        assertEquals(1050L, Money.parse("10.5", "USD"))
    }

    @Test
    fun parse_moreDecimalsThanCurrencyAllowsIsRejected() {
        assertNull(Money.parse("10.505", "USD"))
        assertNull(Money.parse("10.5", "JPY"))
    }

    @Test
    fun parse_trailingSeparatorIsAcceptedWhileTyping() {
        assertEquals(1000L, Money.parse("10.", "USD"))
        assertEquals(10L, Money.parse("10.", "JPY"))
    }

    @Test
    fun parse_noFloatRoundingErrors() {
        // 0.1 + 0.2 y 1.15 son los casos clásicos en que float/double falla.
        assertEquals(10L, Money.parse("0.10", "USD"))
        assertEquals(115L, Money.parse("1.15", "USD"))
        assertEquals(1999999999999L, Money.parse("19999999999.99", "USD"))
    }

    @Test
    fun parse_zeroIsParsedButNotAValidAmount() {
        assertEquals(0L, Money.parse("0", "USD"))
        assertEquals(false, Money.isValidAmount(0L))
        assertEquals(true, Money.isValidAmount(1L))
    }

    @Test
    fun parse_rejectsEmptyBlankAndGarbage() {
        assertNull(Money.parse("", "USD"))
        assertNull(Money.parse("   ", "USD"))
        assertNull(Money.parse("abc", "USD"))
        assertNull(Money.parse("1.2.3", "USD"))
        assertNull(Money.parse("1,2,3", "USD"))
        assertNull(Money.parse("1e5", "USD"))
        assertNull(Money.parse(".", "USD"))
    }

    @Test
    fun parse_rejectsNegativeAndSignedInput() {
        assertNull(Money.parse("-5", "USD"))
        assertNull(Money.parse("+5", "USD"))
    }

    @Test
    fun parse_rejectsOverflowInsteadOfWrapping() {
        assertNull(Money.parse("99999999999999999999", "USD"))
    }

    @Test
    fun parse_leadingSeparatorMeansZeroUnits() {
        assertEquals(50L, Money.parse(".50", "USD"))
    }

    // --- Formato según locale (SPEC §6) ---

    @Test
    fun format_usesLocaleGroupingAndCurrencyDecimals() {
        assertEquals("15,000.00", Money.format(1_500_000L, "USD", Locale.US))
        assertEquals("1,500", Money.format(1_500L, "JPY", Locale.US))
    }

    @Test
    fun format_isExactForLargeAndSmallValues() {
        assertEquals("0.01", Money.format(1L, "USD", Locale.US))
        assertEquals("19,999,999,999.99", Money.format(1_999_999_999_999L, "USD", Locale.US))
    }

    @Test
    fun format_respectsLocaleSeparators() {
        assertEquals("15.000,00", Money.format(1_500_000L, "USD", Locale.forLanguageTag("es-CO")))
    }

    // --- Monto con signo explícito (SPEC §5.3: no depender solo del color) ---

    @Test
    fun formatSigned_incomeUsesPlus() {
        assertEquals("+15,000.00", Money.formatSigned(EntryType.INCOME, 1_500_000L, "USD", Locale.US))
    }

    @Test
    fun formatSigned_expenseUsesTrueMinusSignU2212() {
        val text = Money.formatSigned(EntryType.EXPENSE, 1_500_000L, "USD", Locale.US)
        assertEquals("−15,000.00", text)
        assertEquals(false, text.startsWith("-"))
    }

    // --- toInput: el monto guardado como texto editable (formulario de edición) ---

    @Test
    fun toInput_isThePlainDecimalWithoutTrailingZeros() {
        assertEquals("12.5", Money.toInput(1250, "USD"))
        assertEquals("15", Money.toInput(1500, "USD"))
        assertEquals("0.01", Money.toInput(1, "USD"))
        assertEquals("15000", Money.toInput(1_500_000, "USD"))
        assertEquals("1", Money.toInput(100, "USD"))
    }

    @Test
    fun toInput_forACurrencyWithoutDecimals_isTheInteger() {
        assertEquals("5", Money.toInput(5, "JPY"))
        assertEquals("1200", Money.toInput(1200, "JPY"))
    }

    @Test
    fun toInput_neverUsesScientificNotation_forBigAmounts() {
        assertEquals("1000000000000", Money.toInput(1_000_000_000_000L, "JPY"))
        assertEquals("10000000000", Money.toInput(1_000_000_000_000L, "USD"))
        assertEquals("92233720368547758.07", Money.toInput(Long.MAX_VALUE, "USD"))
    }

    @Test
    fun toInput_roundTripsThroughParse() {
        listOf(1L, 99L, 100L, 1250L, 1_500_000L, 123_456_789L, Long.MAX_VALUE).forEach { minor ->
            assertEquals("USD $minor", minor, Money.parse(Money.toInput(minor, "USD"), "USD"))
        }
        listOf(1L, 5L, 1200L, Long.MAX_VALUE).forEach { minor ->
            assertEquals("JPY $minor", minor, Money.parse(Money.toInput(minor, "JPY"), "JPY"))
        }
    }

    @Test
    fun toInput_isAcceptedByTheAmountFilter() {
        listOf(1L, 1250L, 1_500_000L).forEach { minor ->
            assertTrue(AmountInput.accepts(Money.toInput(minor, "USD"), 2))
        }
    }
}
