package com.v4atu.bandtracker.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Símbolo de la moneda que acompaña al monto en captura (Penpot: «$ 00.00»). */
class MoneySymbolTest {

    @Test
    fun symbol_usesTheLocalSymbolWhenTheLocaleHasOne() {
        assertEquals("$", Money.symbol("USD", Locale.US))
        assertEquals("€", Money.symbol("EUR", Locale.US))
    }

    @Test
    fun symbol_neverReturnsEmptyText_forAnyCurrencyTheSystemOffers() {
        java.util.Currency.getAvailableCurrencies().forEach { currency ->
            assertTrue(
                "${currency.currencyCode} no debe quedar sin símbolo",
                Money.symbol(currency.currencyCode, Locale.US).isNotBlank(),
            )
        }
    }

    @Test
    fun zero_isFormattedWithTheDecimalsAndSeparatorOfTheLocale() {
        assertEquals("0.00", Money.format(0, "USD", Locale.US))
        assertEquals("0,00", Money.format(0, "USD", Locale.forLanguageTag("es-CO")))
        assertEquals("0", Money.format(0, "JPY", Locale.JAPAN))
    }
}
