package com.v4atu.bandtracker.ui.home

import android.content.Context
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import com.v4atu.bandtracker.data.AccountRepository
import com.v4atu.bandtracker.data.AccountSelection
import com.v4atu.bandtracker.data.DatabaseTestBase
import com.v4atu.bandtracker.data.NameResult
import com.v4atu.bandtracker.data.SelectedAccountStore
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Estado de Inicio (SPEC §5.1): cuentas, cuenta seleccionada y «cargado» para no parpadear con una pantalla vacía. */
class HomeViewModelTest : DatabaseTestBase() {

    private lateinit var prefs: SharedPreferences
    private lateinit var accounts: AccountRepository
    private var counter = 0

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        prefs = context.getSharedPreferences("test-${UUID.randomUUID()}", Context.MODE_PRIVATE)
        accounts = AccountRepository(db) { "uuid-${++counter}" }
    }

    @After
    fun clearPrefs() {
        prefs.edit().clear().commit()
    }

    private fun viewModel() = HomeViewModel(AccountSelection(accounts, SelectedAccountStore(prefs)))

    private suspend fun HomeViewModel.loaded(predicate: (HomeUiState) -> Boolean = { true }): HomeUiState =
        withTimeout(5_000) { state.first { it.loaded && predicate(it) } }

    @Test
    fun beforeTheFirstReadFromTheDatabase_theStateIsNotLoaded() {
        assertFalse(HomeUiState().loaded)
        assertTrue(HomeUiState().accounts.isEmpty())
        assertNull(HomeUiState().selected)
    }

    @Test
    fun withoutAccounts_itLoadsEmpty() = runBlocking {
        val state = viewModel().loaded()

        assertTrue(state.accounts.isEmpty())
        assertNull(state.selected)
    }

    @Test
    fun withAccounts_theFirstByNameIsSelected_andAllAreListed() = runBlocking {
        accounts.create("Viaje", "USD")
        accounts.create("Efectivo", "COP")

        val state = viewModel().loaded { it.accounts.size == 2 }

        assertEquals(listOf("Efectivo", "Viaje"), state.accounts.map { it.name })
        assertEquals("Efectivo", state.selected?.name)
    }

    @Test
    fun select_updatesTheState_andIsRemembered() = runBlocking {
        accounts.create("Efectivo", "COP")
        val viaje = (accounts.create("Viaje", "USD") as NameResult.Saved).id
        val vm = viewModel()
        vm.loaded { it.accounts.size == 2 }

        vm.select(viaje)

        assertEquals("Viaje", vm.loaded { it.selected?.name == "Viaje" }.selected?.name)
        assertEquals("Viaje", viewModel().loaded { it.selected != null }.selected?.name)
    }

    @Test
    fun theStateFollowsAccountsCreatedAndDeletedElsewhere() = runBlocking {
        val vm = viewModel()
        assertTrue(vm.loaded().accounts.isEmpty())

        val id = (accounts.create("Efectivo", "COP") as NameResult.Saved).id
        assertEquals("Efectivo", vm.loaded { it.selected?.name == "Efectivo" }.selected?.name)

        accounts.delete(id)
        assertNull(vm.loaded { it.accounts.isEmpty() }.selected)
    }
}
