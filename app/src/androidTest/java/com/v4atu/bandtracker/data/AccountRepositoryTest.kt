package com.v4atu.bandtracker.data

import com.v4atu.bandtracker.domain.NameCheck
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** SPEC §4 y §5.4: crear/renombrar con nombre único (sin mayúsculas ni tildes, ñ distinta), borrar. */
class AccountRepositoryTest : DatabaseTestBase() {

    private lateinit var repo: AccountRepository
    private var counter = 0

    @Before
    fun setUpRepos() {
        repo = AccountRepository(db) { "uuid-${++counter}" }
    }

    private suspend fun created(name: String, currency: String = "COP"): Long =
        (repo.create(name, currency) as NameResult.Saved).id

    @Test
    fun create_savesTrimmedNameWithCurrencyAndGeneratedUuid() = runTest {
        val id = created("  Efectivo  ", "USD")

        val account = repo.list().single()
        assertEquals(id, account.id)
        assertEquals("Efectivo", account.name)
        assertEquals("USD", account.currency)
        assertEquals("uuid-1", account.uuid)
    }

    @Test
    fun create_rejectsBlankName() = runTest {
        assertEquals(NameResult.Rejected(NameCheck.BLANK), repo.create("   ", "COP"))
        assertEquals(0, repo.list().size)
    }

    @Test
    fun create_rejectsDuplicateIgnoringCaseAndAccents() = runTest {
        created("Café")

        assertEquals(NameResult.Rejected(NameCheck.DUPLICATE), repo.create("cafe", "COP"))
        assertEquals(NameResult.Rejected(NameCheck.DUPLICATE), repo.create("CAFÉ", "USD"))
        assertEquals(1, repo.list().size)
    }

    @Test
    fun create_allowsEnyeAndNAsDifferentNames() = runTest {
        created("Año")

        assertTrue(repo.create("Ano", "COP") is NameResult.Saved)
        assertEquals(2, repo.list().size)
    }

    @Test
    fun create_rejectsUnknownCurrencyCode() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repo.create("Rara", "ZZZ-NOPE") }
        }
    }

    @Test
    fun rename_changesNameKeepingCurrencyAndUuid() = runTest {
        val id = created("Efectivo", "USD")

        assertEquals(NameResult.Saved(id), repo.rename(id, " Caja "))

        val account = repo.list().single()
        assertEquals("Caja", account.name)
        assertEquals("USD", account.currency)
        assertEquals("uuid-1", account.uuid)
    }

    @Test
    fun rename_rejectsAnotherAccountsNameIgnoringAccents() = runTest {
        created("Viaje")
        val id = created("Efectivo")

        assertEquals(NameResult.Rejected(NameCheck.DUPLICATE), repo.rename(id, "VIAJE"))
        assertEquals("Efectivo", repo.list().first { it.id == id }.name)
    }

    @Test
    fun rename_allowsChangingOnlyCaseOrAccentsOfItsOwnName() = runTest {
        val id = created("Cafe")

        assertEquals(NameResult.Saved(id), repo.rename(id, "Café"))
        assertEquals("Café", repo.list().single().name)
    }

    @Test
    fun rename_updatesTheKeySoLaterDuplicatesAreDetected() = runTest {
        val id = created("Efectivo")
        repo.rename(id, "Caja")

        // "Efectivo" quedó libre; "caja" ya no.
        assertTrue(repo.create("efectivo", "COP") is NameResult.Saved)
        assertEquals(NameResult.Rejected(NameCheck.DUPLICATE), repo.create("CAJA", "COP"))
    }

    @Test
    fun rename_rejectsBlank() = runTest {
        val id = created("Efectivo")

        assertEquals(NameResult.Rejected(NameCheck.BLANK), repo.rename(id, "  "))
        assertEquals("Efectivo", repo.list().single().name)
    }

    @Test
    fun list_isOrderedByNameIgnoringCaseAndAccents() = runTest {
        created("viaje")
        created("Éxito")
        created("Ahorros")

        assertEquals(listOf("Ahorros", "Éxito", "viaje"), repo.list().map { it.name })
    }

    @Test
    fun entryCount_andDelete_cascadeEntries() = runTest {
        val id = created("Efectivo")
        val other = created("Otra")
        db.entryDao().insert(entry(id, "e1"))
        db.entryDao().insert(entry(id, "e2"))
        db.entryDao().insert(entry(other, "e3"))

        assertEquals(2, repo.entryCount(id))

        repo.delete(id)

        assertEquals(listOf("Otra"), repo.list().map { it.name })
        assertEquals(0, db.entryDao().count(id))
        assertEquals(1, db.entryDao().count(other))
        assertEquals(1, repo.entryCount(other))
    }
}
