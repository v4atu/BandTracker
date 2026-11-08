package com.v4atu.bandtracker.domain

import java.text.Normalizer
import java.util.Locale

/**
 * Clave de comparación de texto (SPEC §4): ignora mayúsculas y tildes, pero conserva la `ñ`
 * (en español es otra letra, no una `n` con tilde). SQLite solo ignora mayúsculas ASCII, por eso
 * la clave se calcula aquí y se guarda en una columna indexada.
 */
object TextKey {

    private const val COMBINING_TILDE = '̃'

    fun of(text: String): String {
        // Locale.ROOT: el resultado no debe depender del idioma del dispositivo (p. ej. turco).
        val decomposed = Normalizer.normalize(text.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)

        val kept = StringBuilder(decomposed.length)
        for (c in decomposed) {
            val isMark = Character.getType(c) == Character.NON_SPACING_MARK.toInt()
            val isEnyeTilde = c == COMBINING_TILDE && kept.isNotEmpty() && kept.last() == 'n'
            if (!isMark || isEnyeTilde) kept.append(c)
        }
        // NFC recompone "n" + U+0303 en el único carácter "ñ" (U+00F1) que produce un teclado.
        return Normalizer.normalize(kept, Normalizer.Form.NFC)
    }
}
