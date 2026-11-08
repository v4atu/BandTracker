package com.v4atu.bandtracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.v4atu.bandtracker.domain.Money
import com.v4atu.bandtracker.domain.TextKey
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Las reglas de dominio dependen de datos de la plataforma (ICU de Android, `java.util.Currency`).
 * Esta prueba confirma en un dispositivo real lo que las pruebas JVM asumen (SPEC §4, criterio 6).
 */
@RunWith(AndroidJUnit4::class)
class PlatformDomainTest {

    @Test
    fun currencyFractionDigits_matchIso4217OnAndroid() {
        assertEquals(2, Money.fractionDigits("USD"))
        assertEquals(2, Money.fractionDigits("COP"))
        assertEquals(0, Money.fractionDigits("JPY"))
    }

    @Test
    fun moneyParsingAndFormatting_areExactOnAndroid() {
        assertEquals(1050L, Money.parse("10.50", "COP"))
        assertEquals("15,000.00", Money.format(1_500_000L, "USD", Locale.US))
        assertEquals("15.000,00", Money.format(1_500_000L, "COP", Locale.forLanguageTag("es-CO")))
    }

    @Test
    fun textKey_ignoresCaseAndAccentsButKeepsEnyeOnAndroid() {
        assertEquals(TextKey.of("Café"), TextKey.of("CAFE"))
        assertEquals("ñandu", TextKey.of("Ñandú"))
        assertNotEquals(TextKey.of("Año"), TextKey.of("Ano"))
        assertEquals(TextKey.of("Año"), TextKey.of("Año"))
    }

    @Test
    fun textKey_isIndependentOfDeviceLocaleOnAndroid() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("iris", TextKey.of("IRIS"))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
