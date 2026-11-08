package com.v4atu.bandtracker.domain

import java.util.Currency
import java.util.Locale

/** Una moneda del buscador: su código ISO 4217 y su nombre en el idioma de la región. */
data class CurrencyOption(val code: String, val name: String)

/**
 * Buscador de monedas de «Nueva cuenta» (SPEC §5.4) sobre todas las que ofrece el sistema
 * (`java.util.Currency`, sin red). Compara sin distinguir mayúsculas ni tildes ([TextKey]).
 */
object CurrencySearch {

    /**
     * Las monedas que coinciden con [query] por código (prefijo) o por nombre (en cualquier parte),
     * primero las de código y luego las de nombre, cada grupo ordenado por código. Una consulta
     * vacía lista todas.
     */
    fun search(query: String, locale: Locale): List<CurrencyOption> {
        val all = Currency.getAvailableCurrencies()
            .map { CurrencyOption(it.currencyCode, it.getDisplayName(locale)) }
            .sortedBy { it.code }
        val key = TextKey.of(query.trim())
        if (key.isEmpty()) return all

        val byCode = all.filter { TextKey.of(it.code).startsWith(key) }
        val byName = all.filter { it !in byCode && TextKey.of(it.name).contains(key) }
        return byCode + byName
    }
}
