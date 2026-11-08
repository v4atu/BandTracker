package com.v4atu.bandtracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** SPEC §5.1: la última cuenta seleccionada se recuerda; sin ninguna recordada (o si ya no existe), la primera. */
class ResolveSelectedAccountTest {

    private val a = Account(1, "ua", "Ahorros", "COP")
    private val b = Account(2, "ub", "Efectivo", "COP")
    private val c = Account(3, "uc", "Viaje", "USD")
    private val all = listOf(a, b, c) // ya ordenadas por nombre, como las entrega el repositorio

    @Test
    fun noAccounts_meansNoSelection() {
        assertNull(resolveSelectedAccount(emptyList(), preferredId = null))
        assertNull(resolveSelectedAccount(emptyList(), preferredId = 7))
    }

    @Test
    fun theRememberedAccount_isSelected() {
        assertEquals(c, resolveSelectedAccount(all, preferredId = 3))
        assertEquals(b, resolveSelectedAccount(all, preferredId = 2))
    }

    @Test
    fun withNothingRemembered_theFirstAccountIsSelected() {
        assertEquals(a, resolveSelectedAccount(all, preferredId = null))
    }

    @Test
    fun ifTheRememberedAccountNoLongerExists_theFirstAccountIsSelected() {
        assertEquals(a, resolveSelectedAccount(all, preferredId = 99))
    }

    @Test
    fun aSingleAccountIsAlwaysSelected() {
        assertEquals(b, resolveSelectedAccount(listOf(b), preferredId = null))
        assertEquals(b, resolveSelectedAccount(listOf(b), preferredId = 42))
    }
}
