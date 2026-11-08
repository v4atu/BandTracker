package com.v4atu.bandtracker.domain

/**
 * Qué texto admite el campo Monto mientras se escribe (SPEC §5.2): solo dígitos 0-9 y, si la
 * moneda tiene decimales, un único separador (`.` o `,`) con no más decimales de los que admite.
 * Es un filtro de entrada; el valor final lo convierte [Money.parse].
 */
object AmountInput {

    fun accepts(text: String, fractionDigits: Int): Boolean {
        if (text.any { it !in '0'..'9' && it != '.' && it != ',' }) return false

        val separators = text.count { it == '.' || it == ',' }
        if (separators == 0) return true
        if (separators > 1 || fractionDigits == 0) return false

        return text.substringAfter(text.first { it == '.' || it == ',' }).length <= fractionDigits
    }
}
