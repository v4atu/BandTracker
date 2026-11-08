package com.v4atu.bandtracker.ui.history

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import androidx.test.platform.app.InstrumentationRegistry
import com.v4atu.bandtracker.data.AccountRepository
import com.v4atu.bandtracker.data.AccountSelection
import com.v4atu.bandtracker.data.DatabaseTestBase
import com.v4atu.bandtracker.data.EntryDraft
import com.v4atu.bandtracker.data.EntryRepository
import com.v4atu.bandtracker.data.NameResult
import com.v4atu.bandtracker.data.SelectedAccountStore
import com.v4atu.bandtracker.domain.EntryType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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

/** Historial (SPEC §5.3): cuenta seleccionada, orden, consultas acotadas por páginas y recarga. */
class HistoryViewModelTest : DatabaseTestBase() {

    private val today = LocalDate.of(2026, 11, 6)
    private val clock = Clock.fixed(Instant.parse("2026-11-06T15:00:00Z"), ZoneOffset.UTC)

    private lateinit var prefs: SharedPreferences
    private lateinit var accounts: AccountRepository
    private lateinit var entries: EntryRepository
    private lateinit var selection: AccountSelection
    private var counter = 0
    private var efectivo = 0L

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        prefs = context.getSharedPreferences("test-${UUID.randomUUID()}", Context.MODE_PRIVATE)
        accounts = AccountRepository(db) { "uuid-${++counter}" }
        entries = EntryRepository(db, clock) { "entry-${++counter}" }
        selection = AccountSelection(accounts, SelectedAccountStore(prefs))
        efectivo = (accounts.create("Efectivo", "USD") as NameResult.Saved).id
    }

    /** Los ViewModels creados a mano no tienen dueño que los limpie: se cancelan antes de cerrar la base. */
    private val created = mutableListOf<HistoryViewModel>()

    @After
    fun stopViewModels() {
        created.forEach { it.viewModelScope.cancel() }
    }

    @After
    fun clearPrefs() {
        prefs.edit().clear().commit()
    }

    private fun viewModel(pageSize: Int = 50) = HistoryViewModel(selection, entries, pageSize).also { created += it }

    private suspend fun HistoryViewModel.await(predicate: (HistoryUiState) -> Boolean = { true }): HistoryUiState =
        withTimeout(5_000) { state.first { it.loaded && predicate(it) } }

    private suspend fun add(accountId: Long = efectivo, subject: String?, date: LocalDate = today, type: EntryType = EntryType.EXPENSE): Long {
        val result = entries.add(accountId, EntryDraft(type, 100, date, subject, null))
        return (result as com.v4atu.bandtracker.data.EntryResult.Saved).id
    }

    private fun HistoryUiState.subjects() = entries.map { it.subject }

    @Test
    fun beforeTheFirstRead_theStateIsNotLoaded() {
        assertFalse(HistoryUiState().loaded)
        assertTrue(HistoryUiState().items.isEmpty())
    }

    @Test
    fun withoutAccounts_itLoadsEmpty() = runBlocking {
        accounts.delete(efectivo)

        val state = viewModel().await()

        assertNull(state.account)
        assertTrue(state.entries.isEmpty())
    }

    @Test
    fun anAccountWithoutEntries_loadsEmpty_andCannotLoadMore() = runBlocking {
        val state = viewModel().await()

        assertEquals("Efectivo", state.account?.name)
        assertTrue(state.entries.isEmpty())
        assertFalse(state.canLoadMore)
    }

    @Test
    fun entriesComeNewestFirst_withTheirHeaders() = runBlocking {
        add(subject = "viejo", date = today.minusDays(40))
        add(subject = "ayer", date = today.minusDays(1))
        add(subject = "hoy", date = today)

        val state = viewModel().await { it.entries.size == 3 }

        assertEquals(listOf("hoy", "ayer", "viejo"), state.subjects())
        assertEquals(3, state.items.count { it is HistoryItem.Day })
        assertEquals(2, state.items.count { it is HistoryItem.Month })
    }

    @Test
    fun entriesOfTheSameDay_areOrderedByIdDescending() = runBlocking {
        add(subject = "primero")
        add(subject = "segundo")
        add(subject = "tercero")

        val state = viewModel().await { it.entries.size == 3 }

        assertEquals(listOf("tercero", "segundo", "primero"), state.subjects())
    }

    @Test
    fun onlyTheSelectedAccountsEntriesAreShown() = runBlocking {
        val viaje = (accounts.create("Viaje", "USD") as NameResult.Saved).id
        add(viaje, subject = "hotel")
        add(subject = "mercado")

        val state = viewModel().await { it.entries.isNotEmpty() }

        assertEquals(listOf("mercado"), state.subjects())
    }

    @Test
    fun theFirstPageIsBounded_andLoadMoreAppendsTheNext() = runBlocking {
        (1..5).forEach { add(subject = "s$it", date = today.minusDays(it.toLong())) }
        val vm = viewModel(pageSize = 2)

        val first = vm.await { it.entries.isNotEmpty() }
        assertEquals(listOf("s1", "s2"), first.subjects())
        assertTrue(first.canLoadMore)

        vm.loadMore()
        assertEquals(listOf("s1", "s2", "s3", "s4"), vm.await { it.entries.size == 4 }.subjects())

        vm.loadMore()
        val last = vm.await { it.entries.size == 5 }
        assertEquals(listOf("s1", "s2", "s3", "s4", "s5"), last.subjects())
        assertFalse("la última página llega incompleta", last.canLoadMore)
    }

    @Test
    fun whenTheTotalIsAMultipleOfThePage_theNextLoadFindsNothingAndStops() = runBlocking {
        (1..4).forEach { add(subject = "s$it", date = today.minusDays(it.toLong())) }
        val vm = viewModel(pageSize = 2)
        vm.await { it.entries.size == 2 }

        vm.loadMore()
        assertTrue(vm.await { it.entries.size == 4 }.canLoadMore)
        vm.loadMore()

        assertFalse(vm.await { !it.canLoadMore }.canLoadMore)
        assertEquals(4, vm.state.value.entries.size)
    }

    @Test
    fun loadMore_calledTheInstantEachPageArrives_loadsEverythingOnce_withoutDroppingOrDuplicating() = runBlocking {
        (1..30).forEach { add(subject = "s$it", date = today.minusDays(it.toLong())) }
        val vm = viewModel(pageSize = 3)
        var current = vm.await { it.entries.size == 3 }

        // Pide la siguiente página en cuanto se ve la anterior, como hace la lista al llegar al final.
        while (current.canLoadMore) {
            val before = current.entries.size
            vm.loadMore()
            current = vm.await { it.entries.size > before || !it.canLoadMore }
        }

        val ids = current.entries.map { it.id }
        assertEquals("ninguno se pierde", 30, ids.size)
        assertEquals("ninguno se repite", 30, ids.toSet().size)
        assertEquals((1..30).map { "s$it" }, current.subjects())
    }

    @Test
    fun loadMore_withNothingLeft_doesNothing() = runBlocking {
        add(subject = "uno")
        val vm = viewModel(pageSize = 5)
        vm.await { it.entries.size == 1 }

        vm.loadMore()
        vm.loadMore()

        assertEquals(1, vm.await().entries.size)
    }

    @Test
    fun reload_picksUpNewEdited_andDeletedEntries_keepingWhatWasLoaded() = runBlocking {
        val a = add(subject = "a", date = today.minusDays(3))
        val b = add(subject = "b", date = today.minusDays(2))
        val vm = viewModel(pageSize = 2)
        assertEquals(listOf("b", "a"), vm.await { it.entries.size == 2 }.subjects())

        add(subject = "nuevo", date = today)
        entries.update(a, EntryDraft(EntryType.EXPENSE, 100, today.minusDays(3), "a editado", null))
        entries.delete(b)
        vm.reload()

        assertEquals(listOf("nuevo", "a editado"), vm.await { it.subjects() == listOf("nuevo", "a editado") }.subjects())
    }

    @Test
    fun reload_afterLoadingMorePages_stillShowsEverythingLoaded() = runBlocking {
        (1..5).forEach { add(subject = "s$it", date = today.minusDays(it.toLong())) }
        val vm = viewModel(pageSize = 2)
        vm.await { it.entries.size == 2 }
        vm.loadMore()
        vm.await { it.entries.size == 4 }

        vm.reload()

        assertEquals(4, vm.await { it.entries.size == 4 }.entries.size)
        assertTrue(vm.state.value.canLoadMore)
    }

    @Test
    fun changingTheSelectedAccount_showsThatAccountsEntriesFromTheStart() = runBlocking {
        val viaje = (accounts.create("Viaje", "USD") as NameResult.Saved).id
        add(subject = "mercado")
        add(viaje, subject = "hotel")
        val vm = viewModel()
        assertEquals(listOf("mercado"), vm.await { it.entries.isNotEmpty() }.subjects())

        selection.select(viaje)

        val state = vm.await { it.account?.id == viaje && it.entries.isNotEmpty() }
        assertEquals(listOf("hotel"), state.subjects())
    }

    @Test
    fun renamingTheSelectedAccount_updatesTheTitle_withoutLosingTheEntries() = runBlocking {
        add(subject = "mercado")
        val vm = viewModel()
        vm.await { it.entries.size == 1 }

        accounts.rename(efectivo, "Caja")

        val state = vm.await { it.account?.name == "Caja" }
        assertEquals(listOf("mercado"), state.subjects())
    }

    @Test
    fun deletingTheSelectedAccount_leavesNoAccountAndNoEntries() = runBlocking {
        add(subject = "mercado")
        val vm = viewModel()
        vm.await { it.entries.size == 1 }

        accounts.delete(efectivo)

        val state = vm.await { it.account == null }
        assertTrue(state.entries.isEmpty())
    }
}
