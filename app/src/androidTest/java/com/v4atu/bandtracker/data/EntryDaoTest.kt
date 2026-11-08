package com.v4atu.bandtracker.data

import androidx.sqlite.SQLiteException
import com.v4atu.bandtracker.domain.TextKey
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/** SPEC §4 (Entry), §5.2 (sugerencias) y §5.3 (orden e historial acotado). */
class EntryDaoTest : DatabaseTestBase() {

    private val accounts get() = db.accountDao()
    private val entries get() = db.entryDao()
    private var accountId = 0L

    @Before
    fun seedAccount() = runBlocking {
        accountId = accounts.insert(account(uuid = "acc", name = "Efectivo"))
    }

    // --- CRUD y restricciones ---

    @Test
    fun insert_persistsAllFields() = runTest {
        val id = entries.insert(
            entry(accountId, "e1", date = "2026-11-07", type = "EXPENSE", amountMinor = 15000, subject = "Taxi", note = "Aeropuerto"),
        )

        val stored = entries.findById(id)!!
        assertEquals("e1", stored.uuid)
        assertEquals(accountId, stored.accountId)
        assertEquals("EXPENSE", stored.type)
        assertEquals(15000L, stored.amountMinor)
        assertEquals("2026-11-07", stored.date)
        assertEquals("Taxi", stored.subject)
        assertEquals("Aeropuerto", stored.note)
    }

    @Test
    fun subjectAndNote_areOptional() = runTest {
        val id = entries.insert(entry(accountId, "e1", subject = null, note = null))

        val stored = entries.findById(id)!!
        assertNull(stored.subject)
        assertNull(stored.note)
    }

    @Test
    fun entryWithUnknownAccount_isRejectedByForeignKey() {
        assertThrows(SQLiteException::class.java) {
            runBlocking { entries.insert(entry(accountId = 9999, uuid = "e1")) }
        }
    }

    @Test
    fun duplicateUuid_isRejected() = runTest {
        entries.insert(entry(accountId, "e1"))

        assertThrows(SQLiteException::class.java) {
            runBlocking { entries.insert(entry(accountId, "e1")) }
        }
    }

    @Test
    fun update_changesFieldsKeepingIdentity() = runTest {
        val id = entries.insert(entry(accountId, "e1", amountMinor = 1000, subject = "A"))
        val original = entries.findById(id)!!

        entries.update(
            original.copy(
                amountMinor = 2500, subject = "B", subjectKey = TextKey.of("B"),
                note = "n", date = "2026-11-01", type = "EXPENSE",
            ),
        )

        val stored = entries.findById(id)!!
        assertEquals(2500L, stored.amountMinor)
        assertEquals("B", stored.subject)
        assertEquals("n", stored.note)
        assertEquals("2026-11-01", stored.date)
        assertEquals("EXPENSE", stored.type)
        assertEquals("e1", stored.uuid)
        assertEquals(accountId, stored.accountId)
    }

    @Test
    fun deleteById_removesOnlyThatEntry() = runTest {
        val keep = entries.insert(entry(accountId, "keep"))
        val gone = entries.insert(entry(accountId, "gone"))

        entries.deleteById(gone)

        assertNull(entries.findById(gone))
        assertEquals(keep, entries.findById(keep)!!.id)
    }

    @Test
    fun count_isPerAccount() = runTest {
        val other = accounts.insert(account(uuid = "o", name = "Otra"))
        entries.insert(entry(accountId, "e1"))
        entries.insert(entry(accountId, "e2"))
        entries.insert(entry(other, "e3"))

        assertEquals(2, entries.count(accountId))
        assertEquals(1, entries.count(other))
    }

    // --- Historial: orden global date DESC, id DESC, y páginas acotadas ---

    @Test
    fun page_ordersByDateDescThenIdDesc() = runTest {
        entries.insert(entry(accountId, "old", date = "2026-11-01"))
        val firstToday = entries.insert(entry(accountId, "today-1", date = "2026-11-07"))
        val secondToday = entries.insert(entry(accountId, "today-2", date = "2026-11-07"))
        entries.insert(entry(accountId, "mid", date = "2026-11-05"))

        val ids = entries.page(accountId, limit = 10, offset = 0).map { it.uuid }

        assertEquals(listOf("today-2", "today-1", "mid", "old"), ids)
        assertEquals(true, secondToday > firstToday)
    }

    @Test
    fun page_isBoundedByLimitAndContinuesWithOffset() = runTest {
        (1..7).forEach { entries.insert(entry(accountId, "e$it", date = "2026-11-0$it")) }

        val first = entries.page(accountId, limit = 3, offset = 0).map { it.uuid }
        val second = entries.page(accountId, limit = 3, offset = 3).map { it.uuid }
        val last = entries.page(accountId, limit = 3, offset = 6).map { it.uuid }

        assertEquals(listOf("e7", "e6", "e5"), first)
        assertEquals(listOf("e4", "e3", "e2"), second)
        assertEquals(listOf("e1"), last)
        assertEquals(emptyList<String>(), entries.page(accountId, limit = 3, offset = 9).map { it.uuid })
    }

    @Test
    fun page_neverMixesAccounts() = runTest {
        val other = accounts.insert(account(uuid = "o", name = "Otra", currency = "USD"))
        entries.insert(entry(accountId, "mine"))
        entries.insert(entry(other, "theirs"))

        assertEquals(listOf("mine"), entries.page(accountId, 10, 0).map { it.uuid })
        assertEquals(listOf("theirs"), entries.page(other, 10, 0).map { it.uuid })
    }

    // --- Sugerencias de sujeto (SPEC §5.2): prefijo sobre subjectKey, sin mayúsculas ni tildes ---

    private fun key(text: String) = TextKey.of(text)

    @Test
    fun suggestions_areDistinctAndOrderedByKeyByPrefix() = runTest {
        entries.insert(entry(accountId, "e1", subject = "Taxi"))
        entries.insert(entry(accountId, "e2", subject = "Taxi"))
        entries.insert(entry(accountId, "e3", subject = "tarjeta"))
        entries.insert(entry(accountId, "e4", subject = "Mercado"))

        val result = entries.suggestSubjects(accountId, keyPrefix = key("TA"), limit = 5)

        // Distintos, ordenados por clave ("tar" < "tax").
        assertEquals(listOf("tarjeta", "Taxi"), result)
    }

    @Test
    fun suggestions_collapseVariantsAndReturnTheMostRecentlyTyped() = runTest {
        entries.insert(entry(accountId, "e1", subject = "Cafe"))
        entries.insert(entry(accountId, "e2", subject = "Café"))

        assertEquals(listOf("Café"), entries.suggestSubjects(accountId, keyPrefix = key("ca"), limit = 5))
    }

    @Test
    fun suggestions_ignoreAccentsAndCase() = runTest {
        entries.insert(entry(accountId, "e1", subject = "Café con leche"))
        entries.insert(entry(accountId, "e2", subject = "ÉXITO"))

        assertEquals(listOf("Café con leche"), entries.suggestSubjects(accountId, key("CAFE"), 5))
        assertEquals(listOf("ÉXITO"), entries.suggestSubjects(accountId, key("exi"), 5))
    }

    @Test
    fun suggestions_treatEnyeAsADifferentLetterThanN() = runTest {
        entries.insert(entry(accountId, "e1", subject = "Ñandú"))
        entries.insert(entry(accountId, "e2", subject = "Nandina"))

        assertEquals(listOf("Ñandú"), entries.suggestSubjects(accountId, key("ÑAN"), 5))
        assertEquals(listOf("Nandina"), entries.suggestSubjects(accountId, key("nan"), 5))
    }

    @Test
    fun suggestions_matchByPrefixNotBySubstring() = runTest {
        entries.insert(entry(accountId, "e1", subject = "Pasta"))

        assertEquals(emptyList<String>(), entries.suggestSubjects(accountId, key("ta"), 5))
    }

    @Test
    fun suggestions_areCappedAtLimit() = runTest {
        (1..8).forEach { entries.insert(entry(accountId, "e$it", subject = "Sujeto $it")) }

        assertEquals(5, entries.suggestSubjects(accountId, key("Suj"), limit = 5).size)
    }

    @Test
    fun suggestions_onlyComeFromTheGivenAccount() = runTest {
        val other = accounts.insert(account(uuid = "o", name = "Otra"))
        entries.insert(entry(accountId, "e1", subject = "Taxi"))
        entries.insert(entry(other, "e2", subject = "Tienda"))

        assertEquals(listOf("Taxi"), entries.suggestSubjects(accountId, key("T"), 5))
    }

    @Test
    fun suggestions_ignoreEntriesWithoutSubject() = runTest {
        entries.insert(entry(accountId, "e1", subject = null))
        entries.insert(entry(accountId, "e2", subject = ""))
        entries.insert(entry(accountId, "e3", subject = "   "))

        assertEquals(emptyList<String>(), entries.suggestSubjects(accountId, key(""), 5))
    }

    @Test
    fun recentSubjects_areDistinctByKeyNewestFirstAndPerAccount() = runTest {
        val other = accounts.insert(account(uuid = "o", name = "Otra"))
        entries.insert(entry(accountId, "e1", subject = "Taxi"))
        entries.insert(entry(other, "e2", subject = "Ajeno"))
        entries.insert(entry(accountId, "e3", subject = "Mercado"))
        entries.insert(entry(accountId, "e4", subject = null))
        entries.insert(entry(accountId, "e5", subject = "TAXI"))

        assertEquals(listOf("TAXI", "Mercado"), entries.recentSubjects(accountId, limit = 5))
        assertEquals(listOf("TAXI"), entries.recentSubjects(accountId, limit = 1))
    }

    @Test
    fun suggestions_treatLikeWildcardsAsLiteralText() = runTest {
        entries.insert(entry(accountId, "e1", subject = "100% efectivo"))
        entries.insert(entry(accountId, "e2", subject = "Taxi"))
        entries.insert(entry(accountId, "e3", subject = "a_b"))
        entries.insert(entry(accountId, "e4", subject = "axb"))

        // "%" y "_" son literales: "%" no coincide con todo y "a_" no coincide con "axb".
        assertEquals(listOf("100% efectivo"), entries.suggestSubjects(accountId, key("100%"), 5))
        assertEquals(emptyList<String>(), entries.suggestSubjects(accountId, key("%"), 5))
        assertEquals(listOf("a_b"), entries.suggestSubjects(accountId, key("a_"), 5))
    }
}
