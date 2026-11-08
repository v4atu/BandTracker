package com.v4atu.bandtracker.domain

import java.util.Currency

/**
 * Decimales de cada moneda, **fijados por la app** según ISO 4217. No se usa
 * `Currency.defaultFractionDigits` porque lo definen los datos de Android, que cambian de una versión
 * a otra (en Android 17 `COP` pasó de 2 a 0 decimales): como el monto se guarda en unidad mínima, un
 * cambio así alteraría lo que significa cada monto ya guardado y cada respaldo (SPEC §4, §7).
 *
 * Solo se listan las monedas que no tienen 2 decimales; cualquier otra, incluidas las que ISO agregue
 * después, tiene 2.
 */
object CurrencyDigits {

    private val ZERO = setOf(
        "BIF", "CLP", "DJF", "GNF", "ISK", "JPY", "KMF", "KRW", "PYG", "RWF", "UGX", "UYI", "VND", "VUV", "XAF", "XOF", "XPF",
        // Retiradas de ISO 4217 pero que `java.util.Currency` aún conoce: sin decimales.
        "ADP", "BEF", "BYB", "BYR", "ESP", "GRD", "ITL", "LUF", "MGF", "PTE", "ROL", "TPE", "TRL",
    )
    private val THREE = setOf("BHD", "IQD", "JOD", "KWD", "LYD", "OMR", "TND")
    private val FOUR = setOf("CLF", "UYW")

    /**
     * Decimales de [currencyCode]. Lanza [IllegalArgumentException] si no es una moneda que el sistema
     * conoce. Los códigos sin unidad menor en ISO (oro, plata, «XXX»…) usan 2 para no dar nunca un valor negativo.
     */
    fun of(currencyCode: String): Int {
        Currency.getInstance(currencyCode) // valida el código, igual que antes
        return when (currencyCode) {
            in ZERO -> 0
            in THREE -> 3
            in FOUR -> 4
            else -> 2
        }
    }
}
