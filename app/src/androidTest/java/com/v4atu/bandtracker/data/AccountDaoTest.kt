package com.v4atu.bandtracker.data

import androidx.sqlite.SQLiteException
import com.v4atu.bandtracker.domain.TextKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/** SPEC §4 (Account) y §5.4. */
class AccountDaoTest : DatabaseTestBase() {

    private val accounts get() = db.accountDao()
    private val entries get() = db.entryDao()

    @Test
    fun insert_assignsAutoincrementIdAndPersistsFields() = runTest {
        val id = accounts.insert(account(uuid = "u1", name = "Efectivo", currency = "COP"))

        val stored = accounts.findById(id)!!
        assertEquals(id, stored.id)
        assertEquals("u1", stored.uuid)
        assertEquals("Efectivo", stored.name)
        assertEquals("COP", stored.currency)
    }

    @Test
    fun findById_unknownIsNull() = runTest {
        assertNull(accounts.findById(999))
    }

    @Test
    fun findByUuid_returnsTheAccount() = runTest {
        val id = accounts.insert(account(uuid = "u1"))

        assertEquals(id, accounts.findByUuid("u1")!!.id)
        assertNull(accounts.findByUuid("nope"))
    }

    @Test
    fun duplicateUuid_isRejected() = runTest {
        accounts.insert(account(uuid = "u1", name = "A"))

        assertThrows(SQLiteException::class.java) {
            kotlinx.coroutines.runBlocking { accounts.insert(account(uuid = "u1", name = "B")) }
        }
    }

    @Test
    fun create_storesTheNormalizedNameKey() = runTest {
        val id = accounts.insert(account(uuid = "u1", name = "  Café Ñandú "))

        assertEquals("cafe ñandu", accounts.findById(id)!!.nameKey)
    }

    @Test
    fun nameKeyIsUnique_ignoringCase_asBackstopForTheDomainRule() = runTest {
        accounts.insert(account(uuid = "u1", name = "Efectivo"))

        assertThrows(SQLiteException::class.java) {
            kotlinx.coroutines.runBlocking { accounts.insert(account(uuid = "u2", name = "efectivo")) }
        }
    }

    @Test
    fun nameKeyIsUnique_ignoringAccentsAndNonAsciiCase() = runTest {
        accounts.insert(account(uuid = "u1", name = "Café"))
        accounts.insert(account(uuid = "u2", name = "ÑANDÚ"))

        assertThrows(SQLiteException::class.java) {
            kotlinx.coroutines.runBlocking { accounts.insert(account(uuid = "u3", name = "cafe")) }
        }
        assertThrows(SQLiteException::class.java) {
            kotlinx.coroutines.runBlocking { accounts.insert(account(uuid = "u4", name = "ñandú")) }
        }
    }

    @Test
    fun enyeAndNAreDifferentNames() = runTest {
        accounts.insert(account(uuid = "u1", name = "Año"))
        accounts.insert(account(uuid = "u2", name = "Ano"))

        assertEquals(2, accounts.getAll().size)
    }

    @Test
    fun getAll_isOrderedByNameIgnoringCase() = runTest {
        accounts.insert(account(uuid = "u1", name = "viaje"))
        accounts.insert(account(uuid = "u2", name = "Efectivo"))
        accounts.insert(account(uuid = "u3", name = "Ahorros"))

        assertEquals(listOf("Ahorros", "Efectivo", "viaje"), accounts.getAll().map { it.name })
    }

    @Test
    fun names_returnsAllNamesForTheDomainCheck() = runTest {
        accounts.insert(account(uuid = "u1", name = "Efectivo"))
        accounts.insert(account(uuid = "u2", name = "Viaje"))

        assertEquals(setOf("Efectivo", "Viaje"), accounts.names().toSet())
    }

    @Test
    fun rename_changesNameAndKey_currencyIsImmutable() = runTest {
        val id = accounts.insert(account(uuid = "u1", name = "Efectivo", currency = "COP"))

        accounts.rename(id, "Caja Ñ", TextKey.of("Caja Ñ"))

        val stored = accounts.findById(id)!!
        assertEquals("Caja Ñ", stored.name)
        assertEquals("caja ñ", stored.nameKey)
        assertEquals("COP", stored.currency)
        assertEquals("u1", stored.uuid)
    }

    @Test
    fun rename_toItsOwnNameWithDifferentCase_isAllowed() = runTest {
        val id = accounts.insert(account(uuid = "u1", name = "Efectivo"))

        accounts.rename(id, "EFECTIVO", TextKey.of("EFECTIVO"))

        assertEquals("EFECTIVO", accounts.findById(id)!!.name)
    }

    @Test
    fun delete_removesTheAccount() = runTest {
        val id = accounts.insert(account(uuid = "u1"))

        accounts.delete(id)

        assertNull(accounts.findById(id))
    }

    @Test
    fun delete_cascadesToItsEntriesOnly() = runTest {
        val a = accounts.insert(account(uuid = "a", name = "A"))
        val b = accounts.insert(account(uuid = "b", name = "B"))
        entries.insert(entry(a, "e1"))
        entries.insert(entry(a, "e2"))
        entries.insert(entry(b, "e3"))

        accounts.delete(a)

        assertEquals(0, entries.count(a))
        assertEquals(1, entries.count(b))
    }
}
