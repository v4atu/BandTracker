package com.v4atu.bandtracker.domain

import java.util.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los decimales de cada moneda son los de ISO 4217 y los fija la app (no `java.util.Currency`): el
 * monto se guarda en unidad mínima, así que su significado no puede cambiar según la versión de
 * Android. Caso real: en Android 17 `COP` pasó a 0 decimales y en Android 16 tiene 2, de modo que un
 * respaldo exportado en un teléfono se habría importado multiplicado por 100 en el otro.
 */
class CurrencyDigitsTest {

    @Test
    fun theColombianPeso_hasTwoDecimals_perIso4217() {
        assertEquals(2, CurrencyDigits.of("COP"))
    }

    @Test
    fun mostCurrenciesHaveTwoDecimals() {
        listOf("USD", "EUR", "GBP", "MXN", "ARS", "BRL", "PEN", "HUF", "IDR", "COP", "CAD", "AUD", "CHF").forEach {
            assertEquals(it, 2, CurrencyDigits.of(it))
        }
    }

    @Test
    fun theZeroDecimalCurrencies() {
        listOf("JPY", "KRW", "CLP", "ISK", "VND", "PYG", "UGX", "RWF", "XAF", "XOF", "XPF", "BIF", "DJF", "GNF", "KMF", "VUV", "UYI").forEach {
            assertEquals(it, 0, CurrencyDigits.of(it))
        }
    }

    @Test
    fun theThreeDecimalCurrencies() {
        listOf("BHD", "IQD", "JOD", "KWD", "LYD", "OMR", "TND").forEach {
            assertEquals(it, 3, CurrencyDigits.of(it))
        }
    }

    @Test
    fun theFourDecimalCurrencies() {
        assertEquals(4, CurrencyDigits.of("CLF"))
        // `UYW` no existe en todas las versiones del sistema: si no lo conoce, se rechaza como cualquier código desconocido.
        if (runCatching { Currency.getInstance("UYW") }.isSuccess) assertEquals(4, CurrencyDigits.of("UYW"))
    }

    @Test
    fun everyCurrencyTheSystemOffers_getsBetweenZeroAndFourDecimals_neverANegativeNumber() {
        // `java.util.Currency` da −1 al oro, la plata, los DEG, «XXX»…: aquí nunca puede salir −1.
        Currency.getAvailableCurrencies().forEach {
            val digits = CurrencyDigits.of(it.currencyCode)
            assertTrue("${it.currencyCode} dio $digits", digits in 0..4)
        }
    }

    @Test
    fun withdrawnCurrenciesKeepTheirHistoricalZeroDecimals() {
        listOf("ITL", "ESP", "PTE", "GRD", "BEF", "LUF", "TRL").forEach { assertEquals(it, 0, CurrencyDigits.of(it)) }
    }

    @Test
    fun anUnknownCode_isRejected_likeBefore() {
        assertThrows(IllegalArgumentException::class.java) { CurrencyDigits.of("ZZZ") }
        assertThrows(IllegalArgumentException::class.java) { CurrencyDigits.of("cop1") }
    }

    @Test
    fun money_usesThePinnedDecimals_forParsingFormattingAndEditing() {
        // Con COP en 2: 5000 pesos son 500000 en unidad mínima, en cualquier teléfono.
        assertEquals(500_000L, Money.parse("5000", "COP"))
        assertEquals("5,000.00", Money.format(500_000, "COP", java.util.Locale.US))
        assertEquals("5000", Money.toInput(500_000, "COP"))
        assertEquals(2, Money.fractionDigits("COP"))
    }
}
