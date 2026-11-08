package com.v4atu.bandtracker.data

import android.content.Context
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** SPEC §5.1: cuentas observables y cuenta seleccionada, recordada entre ejecuciones. */
class SelectedAccountTest : DatabaseTestBase() {

    private lateinit var prefs: SharedPreferences
    private lateinit var accounts: AccountRepository
    private var counter = 0

    @Before
    fun setUp() {
        prefs = context().getSharedPreferences("test-${UUID.randomUUID()}", Context.MODE_PRIVATE)
        accounts = AccountRepository(db) { "uuid-${++counter}" }
    }

    @After
    fun clearPrefs() {
        prefs.edit().clear().commit()
    }

    private fun context() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun selection(store: SelectedAccountStore = SelectedAccountStore(prefs)) = AccountSelection(accounts, store)

    private suspend fun <T> within(block: suspend () -> T): T = withTimeout(5_000) { block() }

    // --- AccountRepository.accounts (observable) ---

    @Test
    fun accounts_emitsTheCurrentListAndThenEveryChange() = runBlocking {
        val seen = mutableListOf<List<String>>()
        val job = launch { accounts.accounts.collect { list -> seen += list.map { it.name } } }

        within { while (seen.isEmpty()) delay(20) }
        assertEquals(listOf(emptyList<String>()), seen.toList())

        val id = (accounts.create("Efectivo", "COP") as NameResult.Saved).id
        within { while (seen.last() != listOf("Efectivo")) delay(20) }

        accounts.rename(id, "Caja")
        within { while (seen.last() != listOf("Caja")) delay(20) }

        accounts.delete(id)
        within { while (seen.last() != emptyList<String>()) delay(20) }
        job.cancel()
    }

    @Test
    fun accounts_areOrderedByNameIgnoringCaseAndAccents() = runBlocking {
        accounts.create("viaje", "COP")
        accounts.create("Éxito", "COP")
        accounts.create("Ahorros", "COP")

        assertEquals(listOf("Ahorros", "Éxito", "viaje"), within { accounts.accounts.first().map { it.name } })
    }

    // --- SelectedAccountStore ---

    @Test
    fun store_startsEmpty_andRemembersTheIdAcrossInstances() {
        assertNull(SelectedAccountStore(prefs).id.value)

        SelectedAccountStore(prefs).set(7)

        assertEquals(7L, SelectedAccountStore(prefs).id.value)
    }

    @Test
    fun store_publishesChanges_andCanBeCleared() {
        val store = SelectedAccountStore(prefs)

        store.set(3)
        assertEquals(3L, store.id.value)

        store.set(null)
        assertNull(store.id.value)
        assertNull(SelectedAccountStore(prefs).id.value)
    }

    // --- AccountSelection ---

    @Test
    fun selection_isNullWithoutAccounts() = runBlocking {
        assertNull(within { selection().selected.first() })
    }

    @Test
    fun selection_defaultsToTheFirstAccountByName() = runBlocking {
        accounts.create("Viaje", "USD")
        accounts.create("Efectivo", "COP")

        assertEquals("Efectivo", within { selection().selected.first()!!.name })
    }

    @Test
    fun select_changesTheSelectedAccount_andIsRememberedByANewInstance() = runBlocking {
        accounts.create("Efectivo", "COP")
        val viaje = (accounts.create("Viaje", "USD") as NameResult.Saved).id

        selection().select(viaje)

        // Otra instancia con las mismas preferencias = «otra ejecución».
        assertEquals("Viaje", within { selection(SelectedAccountStore(prefs)).selected.first()!!.name })
    }

    @Test
    fun ifTheRememberedAccountIsDeleted_theFirstOneTakesOver() = runBlocking {
        accounts.create("Efectivo", "COP")
        val viaje = (accounts.create("Viaje", "USD") as NameResult.Saved).id
        val sel = selection()
        sel.select(viaje)

        accounts.delete(viaje)

        assertEquals("Efectivo", within { sel.selected.first { it?.name == "Efectivo" }!!.name })
    }

    @Test
    fun creatingAnotherAccount_doesNotChangeTheSelection() = runBlocking {
        val efectivo = (accounts.create("Efectivo", "COP") as NameResult.Saved).id
        val sel = selection()
        sel.select(efectivo)

        accounts.create("Ahorros", "COP") // sería la primera por nombre, pero ya hay una elegida

        assertEquals("Efectivo", within { sel.selected.first()!!.name })
    }

    @Test
    fun selection_followsRenames() = runBlocking {
        val id = (accounts.create("Efectivo", "COP") as NameResult.Saved).id
        val sel = selection()
        sel.select(id)

        accounts.rename(id, "Caja")

        assertEquals("Caja", within { sel.selected.first { it?.name == "Caja" }!!.name })
    }
}
