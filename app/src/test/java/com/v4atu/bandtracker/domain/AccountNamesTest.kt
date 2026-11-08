package com.v4atu.bandtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC §4 y §5.4: nombre obligatorio, único sin distinguir mayúsculas. */
class AccountNamesTest {

    private val existing = listOf("Efectivo", "Viaje")

    @Test
    fun newUniqueName_isOk() {
        assertEquals(NameCheck.OK, AccountNames.check("Ahorros", existing))
    }

    @Test
    fun blankName_isRejected() {
        assertEquals(NameCheck.BLANK, AccountNames.check("", existing))
        assertEquals(NameCheck.BLANK, AccountNames.check("   ", existing))
    }

    @Test
    fun sameNameIgnoringCase_isDuplicate() {
        assertEquals(NameCheck.DUPLICATE, AccountNames.check("efectivo", existing))
        assertEquals(NameCheck.DUPLICATE, AccountNames.check("VIAJE", existing))
    }

    @Test
    fun surroundingWhitespaceIsIgnored() {
        assertEquals(NameCheck.DUPLICATE, AccountNames.check("  Viaje  ", existing))
    }

    // Contrato cambiado por decisión de V4atu: el nombre ignora mayúsculas Y tildes (SPEC §4).
    @Test
    fun accentsAreIgnored() {
        assertEquals(NameCheck.DUPLICATE, AccountNames.check("Café", listOf("Cafe")))
        assertEquals(NameCheck.DUPLICATE, AccountNames.check("cafe", listOf("CAFÉ")))
    }

    @Test
    fun enyeIsSignificant() {
        assertEquals(NameCheck.OK, AccountNames.check("Año", listOf("Ano")))
        assertEquals(NameCheck.DUPLICATE, AccountNames.check("AÑO", listOf("año")))
    }

    @Test
    fun renamingToItsOwnNameChangingOnlyAccents_isOk() {
        assertEquals(NameCheck.OK, AccountNames.check("Café", listOf("Cafe", "Viaje"), currentName = "Cafe"))
    }

    @Test
    fun renamingToItsOwnNameChangingOnlyCase_isOk() {
        assertEquals(NameCheck.OK, AccountNames.check("EFECTIVO", existing, currentName = "Efectivo"))
    }

    @Test
    fun renamingToAnotherExistingName_isDuplicate() {
        assertEquals(NameCheck.DUPLICATE, AccountNames.check("viaje", existing, currentName = "Efectivo"))
    }

    @Test
    fun normalized_trimsButKeepsUserCasing() {
        assertEquals("  Mi Cuenta ".trim(), AccountNames.normalized("  Mi Cuenta "))
    }
}
