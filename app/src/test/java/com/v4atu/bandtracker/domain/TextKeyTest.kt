package com.v4atu.bandtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** SPEC §4: se ignoran mayúsculas y tildes; la ñ se distingue de la n. */
class TextKeyTest {

    @Test
    fun ignoresCase() {
        assertEquals(TextKey.of("Taxi"), TextKey.of("TAXI"))
        assertEquals(TextKey.of("Taxi"), TextKey.of("taxi"))
    }

    @Test
    fun ignoresAcutesAndDiaeresis() {
        assertEquals(TextKey.of("Café"), TextKey.of("cafe"))
        assertEquals(TextKey.of("CAFÉ"), TextKey.of("cafe"))
        assertEquals(TextKey.of("Pingüino"), TextKey.of("pinguino"))
        assertEquals("aeiou", TextKey.of("ÁÉÍÓÚ"))
    }

    @Test
    fun enyeIsDistinctFromN() {
        assertNotEquals(TextKey.of("Año"), TextKey.of("Ano"))
        assertNotEquals(TextKey.of("Ñandú"), TextKey.of("Nandu"))
    }

    @Test
    fun enyeIgnoresCaseAndOtherAccents() {
        assertEquals("ñandu", TextKey.of("Ñandú"))
        assertEquals(TextKey.of("ÑANDÚ"), TextKey.of("ñandu"))
    }

    @Test
    fun decomposedInputGivesTheSameKeyAsComposed() {
        // "é" y "ñ" escritos como letra base + marca combinante (NFD).
        assertEquals(TextKey.of("Café"), TextKey.of("Café"))
        assertEquals(TextKey.of("Año"), TextKey.of("Año"))
    }

    @Test
    fun enyeKeyIsInComposedFormSoPrefixesMatch() {
        // La clave de "ñ" debe ser un solo carácter U+00F1, igual al que produce un teclado.
        assertEquals("ñ", TextKey.of("Ñ"))
        assertEquals("ñ", TextKey.of("Ñ"))
    }

    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals(TextKey.of("Viaje"), TextKey.of("  viaje \n"))
    }

    @Test
    fun keepsInnerSpacesAndDigits() {
        assertEquals("pago 2 de 3", TextKey.of("Pago 2 de 3"))
    }

    @Test
    fun emptyAndBlankGiveEmptyKey() {
        assertEquals("", TextKey.of(""))
        assertEquals("", TextKey.of("   "))
    }

    @Test
    fun isIndependentOfTheDeviceLocale() {
        // Con locale turco, "I".lowercase() sería "ı"; la clave debe usar reglas neutras.
        val previous = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"))
            assertEquals("iris", TextKey.of("IRIS"))
        } finally {
            java.util.Locale.setDefault(previous)
        }
    }
}
