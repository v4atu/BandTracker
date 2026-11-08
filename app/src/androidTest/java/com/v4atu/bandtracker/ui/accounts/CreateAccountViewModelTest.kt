package com.v4atu.bandtracker.ui.accounts

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.viewModelScope
import androidx.test.platform.app.InstrumentationRegistry
import com.v4atu.bandtracker.data.AccountRepository
import com.v4atu.bandtracker.data.AccountSelection
import com.v4atu.bandtracker.data.DatabaseTestBase
import com.v4atu.bandtracker.data.SelectedAccountStore
import com.v4atu.bandtracker.domain.NameCheck
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.cancel
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

/** SPEC §5.4: crear una cuenta (nombre + moneda) con nombre único y moneda elegida con buscador. */
class CreateAccountViewModelTest : DatabaseTestBase() {

    private lateinit var prefs: SharedPreferences
    private lateinit var accounts: AccountRepository
    private lateinit var selection: AccountSelection
    private var counter = 0
    private val created = mutableListOf<CreateAccountViewModel>()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        prefs = context.getSharedPreferences("test-${UUID.randomUUID()}", Context.MODE_PRIVATE)
        accounts = AccountRepository(db) { "uuid-${++counter}" }
        selection = AccountSelection(accounts, SelectedAccountStore(prefs))
    }

    @After
    fun stopViewModels() {
        created.forEach { it.viewModelScope.cancel() }
    }

    @After
    fun clearPrefs() {
        prefs.edit().clear().commit()
    }

    private fun viewModel() = CreateAccountViewModel(accounts, selection, Locale.US).also { created += it }

    private suspend fun CreateAccountViewModel.await(predicate: (CreateAccountUiState) -> Boolean = { true }) =
        withTimeout(5_000) { state.first { it.loaded && predicate(it) } }

    /** Sin interfaz (prueba de ViewModel) hay que enviar a mano las notificaciones del snapshot. */
    private fun TextFieldState.put(text: String) {
        setTextAndPlaceCursorAtEnd(text)
        Snapshot.sendApplyNotifications()
    }

    @Test
    fun beforeTheFirstRead_theStateIsNotLoaded() {
        assertFalse(CreateAccountUiState().loaded)
        assertFalse(CreateAccountUiState().canCreate)
    }

    @Test
    fun initially_nothingIsChosen_andEveryCurrencyIsListed() = runBlocking {
        val state = viewModel().await()

        assertFalse(state.canCreate)
        assertNull(state.selectedCurrency)
        assertEquals(NameCheck.BLANK, state.nameCheck)
        assertTrue("lista todas las monedas", state.results.size > 100)
    }

    @Test
    fun aNameAndACurrency_allowCreating() = runBlocking {
        val vm = viewModel()
        vm.await()

        vm.name.put("Efectivo")
        vm.selectCurrency("COP")

        val state = vm.await { it.canCreate }
        assertEquals(NameCheck.OK, state.nameCheck)
        assertEquals("COP", state.selectedCurrency)
    }

    @Test
    fun aNameWithoutACurrency_doesNotAllowCreating() = runBlocking {
        val vm = viewModel()
        vm.await()

        vm.name.put("Efectivo")

        assertFalse(vm.await { it.nameCheck == NameCheck.OK }.canCreate)
    }

    @Test
    fun aCurrencyWithoutAName_doesNotAllowCreating() = runBlocking {
        val vm = viewModel()
        vm.await()

        vm.selectCurrency("USD")

        assertFalse(vm.await { it.selectedCurrency == "USD" }.canCreate)
    }

    @Test
    fun aBlankName_isBlank_notADuplicate() = runBlocking {
        val vm = viewModel()
        vm.await()

        vm.name.put("   ")
        vm.selectCurrency("USD")

        val state = vm.await { it.selectedCurrency == "USD" }
        assertEquals(NameCheck.BLANK, state.nameCheck)
        assertFalse(state.canCreate)
    }

    @Test
    fun anExistingName_isADuplicate_ignoringCaseAndAccents_andBlocksCreating() = runBlocking {
        accounts.create("Café", "COP")
        val vm = viewModel()
        vm.await { it.existingNames == listOf("Café") }

        vm.name.put("cafe")
        vm.selectCurrency("USD")

        val state = vm.await { it.nameCheck == NameCheck.DUPLICATE }
        assertFalse(state.canCreate)
    }

    @Test
    fun theLetterEnye_isNotTheLetterEne() = runBlocking {
        accounts.create("Ano", "COP")
        val vm = viewModel()
        vm.await { it.existingNames == listOf("Ano") }

        vm.name.put("Año")
        vm.selectCurrency("USD")

        assertEquals(NameCheck.OK, vm.await { it.selectedCurrency == "USD" && it.nameCheck != NameCheck.BLANK }.nameCheck)
    }

    @Test
    fun searchingFiltersTheCurrencies_codeFirst() = runBlocking {
        val vm = viewModel()
        vm.await()

        vm.query.put("cop")

        val state = vm.await { it.results.size < 20 }
        assertEquals("COP", state.results.first().code)
    }

    @Test
    fun selectingAnotherCurrency_replacesTheChoice() = runBlocking {
        val vm = viewModel()
        vm.await()

        vm.selectCurrency("COP")
        vm.selectCurrency("EUR")

        assertEquals("EUR", vm.await { it.selectedCurrency == "EUR" }.selectedCurrency)
    }

    @Test
    fun aCodeThatIsNotAnIsoCurrency_isIgnored() = runBlocking {
        val vm = viewModel()
        vm.await()

        vm.selectCurrency("XXQ")

        assertNull(vm.await().selectedCurrency)
    }

    @Test
    fun create_savesTheAccountWithATrimmedName_andItsCurrency() = runBlocking {
        val vm = viewModel()
        vm.await()
        vm.name.put("  Efectivo  ")
        vm.selectCurrency("COP")
        vm.await { it.canCreate }

        vm.create()

        assertTrue(vm.await { it.finished }.finished)
        val saved = accounts.list().single()
        assertEquals("Efectivo", saved.name)
        assertEquals("COP", saved.currency)
    }

    @Test
    fun create_theFirstAccountBecomesTheSelectedOne() = runBlocking {
        val vm = viewModel()
        vm.await()
        vm.name.put("Efectivo")
        vm.selectCurrency("COP")
        vm.await { it.canCreate }

        vm.create()

        vm.await { it.finished }
        assertEquals("Efectivo", withTimeout(5_000) { selection.selected.first { it != null } }?.name)
        assertEquals(accounts.list().single().id, prefs.getLong("selected_account_id", -1))
    }

    @Test
    fun create_aSecondAccount_doesNotChangeTheSelection() = runBlocking {
        accounts.create("Efectivo", "COP")
        selection.select(accounts.list().single().id)
        val vm = viewModel()
        vm.await()
        vm.name.put("Viaje")
        vm.selectCurrency("USD")
        vm.await { it.canCreate }

        vm.create()

        vm.await { it.finished }
        assertEquals("Efectivo", withTimeout(5_000) { selection.selected.first { it != null } }?.name)
    }

    @Test
    fun create_withoutWhatIsNeeded_doesNothing() = runBlocking {
        val vm = viewModel()
        vm.await()
        vm.name.put("Efectivo")

        vm.create()

        assertFalse(vm.await().finished)
        assertTrue(accounts.list().isEmpty())
    }

    @Test
    fun create_twiceInARow_createsOneAccount() = runBlocking {
        val vm = viewModel()
        vm.await()
        vm.name.put("Efectivo")
        vm.selectCurrency("COP")
        vm.await { it.canCreate }

        vm.create()
        vm.create()

        vm.await { it.finished }
        assertEquals(1, accounts.list().size)
    }
}
