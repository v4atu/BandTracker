package com.v4atu.bandtracker.domain

enum class NameCheck { OK, BLANK, DUPLICATE }

/** Nombre de cuenta obligatorio y único sin distinguir mayúsculas ni tildes (SPEC §4, §5.4). */
object AccountNames {

    fun normalized(name: String): String = name.trim()

    /**
     * @param currentName nombre actual al renombrar: se excluye de la comparación para
     * permitir cambiar solo mayúsculas o tildes de la propia cuenta.
     */
    fun check(candidate: String, existingNames: List<String>, currentName: String? = null): NameCheck {
        val key = TextKey.of(candidate)
        if (key.isEmpty()) return NameCheck.BLANK

        val currentKey = currentName?.let(TextKey::of)
        val clash = existingNames.any { other ->
            val otherKey = TextKey.of(other)
            otherKey == key && otherKey != currentKey
        }
        return if (clash) NameCheck.DUPLICATE else NameCheck.OK
    }
}
