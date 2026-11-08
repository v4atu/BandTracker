package com.v4atu.bandtracker.domain

import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Cómo se muestra el monto mientras se escribe (SPEC §5.2): con separador de miles y el separador
 * decimal de la región. Es solo presentación: el texto guardado en el campo sigue siendo plano
 * (dígitos y un `.` o `,`), que es lo que lee [Money.parse].
 */
object AmountFormat {

    private const val GROUP = 3

    /** Posición del separador decimal escrito (`.` o `,`) en [raw], o −1 si no hay. */
    fun decimalIndex(raw: String): Int = raw.indexOfFirst { it == '.' || it == ',' }

    /**
     * Posiciones de [raw] antes de las cuales va un separador de miles: cada tres dígitos contando
     * desde el final de la parte entera. La parte decimal no se agrupa.
     */
    fun groupSeparatorIndexes(raw: String): List<Int> {
        val integerLength = decimalIndex(raw).let { if (it < 0) raw.length else it }
        return (integerLength - GROUP downTo 1 step GROUP).toList().reversed()
    }

    /** [raw] tal como se ve: con los separadores de [locale] (miles y decimal). */
    fun display(raw: String, locale: Locale): String {
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val decimal = decimalIndex(raw)
        val groups = groupSeparatorIndexes(raw).toSet()
        return buildString {
            raw.forEachIndexed { index, char ->
                if (index in groups) append(symbols.groupingSeparator)
                append(if (index == decimal) symbols.decimalSeparator else char)
            }
        }
    }
}
