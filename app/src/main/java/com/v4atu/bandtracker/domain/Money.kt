package com.v4atu.bandtracker.domain

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Montos como enteros en la unidad mínima de la moneda (SPEC §4). Nunca float/double:
 * todo el cálculo pasa por [BigDecimal] y el resultado se valida contra [Long].
 */
object Money {

    private const val MINUS = '−'

    /** Decimales de la moneda según ISO 4217, fijados por la app (ver [CurrencyDigits]), sin red. */
    fun fractionDigits(currencyCode: String): Int = CurrencyDigits.of(currencyCode)

    /** Un monto válido es un entero estrictamente positivo (SPEC §4). */
    fun isValidAmount(amountMinor: Long): Boolean = amountMinor > 0

    /**
     * Convierte el texto del campo a unidad mínima. Acepta solo dígitos y un único
     * separador decimal (`.` o `,`). Devuelve `null` si el texto está vacío, tiene signo,
     * más decimales de los que admite la moneda, o no cabe en un [Long].
     */
    fun parse(input: String, currencyCode: String): Long? {
        val text = input.trim()
        if (text.isEmpty()) return null

        val separators = text.count { it == '.' || it == ',' }
        if (separators > 1) return null
        if (text.any { !it.isDigit() && it != '.' && it != ',' }) return null
        // `isDigit` acepta dígitos Unicode no ASCII; aquí solo se admite 0-9.
        if (text.any { it.isDigit() && it !in '0'..'9' }) return null

        val parts = text.split('.', ',')
        val whole = parts[0]
        val fraction = parts.getOrElse(1) { "" }
        if (whole.isEmpty() && fraction.isEmpty()) return null

        val digits = fractionDigits(currencyCode)
        if (fraction.length > digits) return null

        val normalized = (whole.ifEmpty { "0" }) + "." + fraction.padEnd(digits, '0')
        return try {
            BigDecimal(normalized).movePointRight(digits).longValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }

    /**
     * El monto guardado como texto editable (formulario de edición): decimal plano con `.`, sin ceros
     * sobrantes (`1250` USD → `12.5`) y sin notación científica. Es lo inverso de [parse].
     */
    fun toInput(amountMinor: Long, currencyCode: String): String =
        BigDecimal.valueOf(amountMinor, fractionDigits(currencyCode)).stripTrailingZeros().toPlainString()

    /**
     * Símbolo de la moneda para la región [locale] (`$`, `€`). Si la región no tiene uno propio,
     * `java.util.Currency` devuelve el código ISO, que sirve igual para identificar la moneda.
     */
    fun symbol(currencyCode: String, locale: Locale): String =
        Currency.getInstance(currencyCode).getSymbol(locale).ifBlank { currencyCode }

    /** Monto sin signo con separadores y decimales de la moneda y la región. */
    fun format(amountMinor: Long, currencyCode: String, locale: Locale): String {
        val digits = fractionDigits(currencyCode)
        val formatter = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
        return formatter.format(BigDecimal.valueOf(amountMinor, digits))
    }

    /** Monto con `+` (ingreso) o `−` U+2212 (egreso), para no depender solo del color (SPEC §5.3). */
    fun formatSigned(type: EntryType, amountMinor: Long, currencyCode: String, locale: Locale): String {
        val sign = if (type == EntryType.INCOME) '+' else MINUS
        return sign + format(amountMinor, currencyCode, locale)
    }
}
