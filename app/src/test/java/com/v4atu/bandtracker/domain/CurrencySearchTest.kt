package com.v4atu.bandtracker.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC §5.4: buscador sobre todas las monedas ISO 4217 que ofrece el sistema (`java.util.Currency`). */
class CurrencySearchTest {

    private val en = Locale.US
    private val es = Locale.forLanguageTag("es-CO")

    private fun codes(query: String, locale: Locale = en) = CurrencySearch.search(query, locale).map { it.code }

    @Test
    fun aBlankQuery_listsEveryCurrencyTheSystemOffers_sortedByCode() {
        val all = CurrencySearch.search("", en)

        assertEquals(java.util.Currency.getAvailableCurrencies().size, all.size)
        assertEquals(all.map { it.code }.sorted(), all.map { it.code })
        assertEquals(CurrencySearch.search("   ", en), all)
    }

    @Test
    fun everyOptionHasItsCodeAndALocalizedName() {
        CurrencySearch.search("", en).forEach {
            assertEquals(3, it.code.length)
            assertTrue("${it.code} sin nombre", it.name.isNotBlank())
        }
    }

    @Test
    fun theNameIsInTheLanguageOfTheLocale() {
        val usdEn = CurrencySearch.search("usd", en).first { it.code == "USD" }
        val usdEs = CurrencySearch.search("usd", es).first { it.code == "USD" }

        assertTrue(usdEn.name, usdEn.name.contains("Dollar", ignoreCase = true))
        assertTrue(usdEs.name, usdEs.name.contains("dólar", ignoreCase = true))
    }

    @Test
    fun searchingByCode_ignoresCase_andMatchesThePrefix() {
        assertTrue("COP" in codes("cop"))
        assertTrue("COP" in codes("COP"))
        assertEquals("USD", codes("usd").first())
        assertTrue("«co» debe incluir COP", "COP" in codes("co"))
    }

    @Test
    fun aCodePrefixMatch_comesBeforeANameMatch() {
        val result = codes("eur")

        assertEquals("EUR", result.first())
    }

    @Test
    fun searchingByName_findsTheCurrency_ignoringCaseAndAccents() {
        assertTrue("COP" in codes("colombian"))
        assertTrue("USD" in codes("DOLAR", es))
        assertTrue("USD" in codes("dólar", es))
        assertTrue("COP" in codes("peso colombiano", es))
    }

    @Test
    fun theNameMatchCanBeAnywhereInTheName() {
        assertTrue("COP" in codes("bian", en)) // Colombian
    }

    @Test
    fun noMatch_givesNoResults() {
        assertTrue(codes("zzzzqx").isEmpty())
    }

    @Test
    fun resultsHaveNoDuplicates() {
        val result = codes("peso", es)

        assertEquals(result.size, result.toSet().size)
        assertFalse(result.isEmpty())
    }
}
