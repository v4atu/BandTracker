package com.v4atu.bandtracker.ui.entry

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

/** Captura de Ingreso y Egreso (SPEC §5.2): estado, validación, sugerencias y guardado. */
class EntryFormViewModelTest : DatabaseTestBase() {

    private val today = LocalDate.of(2026, 11, 6)
    private val clock = Clock.fixed(Instant.parse("2026-11-06T15:00:00Z"), ZoneOffset.UTC)

    private lateinit var prefs: SharedPreferences
    private lateinit var accounts: AccountRepository
    private lateinit var entries: EntryRepository
    private var counter = 0
    private var accountId = 0L

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        prefs = context.getSharedPreferences("test-${UUID.randomUUID()}", Context.MODE_PRIVATE)
        accounts = AccountRepository(db) { "uuid-${++counter}" }
        entries = EntryRepository(db, clock) { "entry-${++counter}" }
        accountId = (accounts.create("Efectivo", "USD") as NameResult.Saved).id
    }

    /** Los ViewModels creados a mano no tienen dueño que los limpie: se cancelan antes de cerrar la base. */
    private val created = mutableListOf<EntryFormViewModel>()

    @After
    fun stopViewModels() {
        created.forEach { it.viewModelScope.cancel() }
    }

    @After
    fun clearPrefs() {
        prefs.edit().clear().commit()
    }

    private fun viewModel(type: EntryType = EntryType.INCOME) = EntryFormViewModel(
        selection = AccountSelection(accounts, SelectedAccountStore(prefs)),
        entries = entries,
        clock = clock,
        mode = EntryFormMode.Create(type),
    ).also { created += it }

    private suspend fun EntryFormViewModel.await(predicate: (EntryFormUiState) -> Boolean = { true }): EntryFormUiState =
        withTimeout(5_000) { state.first { it.loaded && predicate(it) } }

    /**
     * Escribe en un campo y avisa del cambio. En la app, Compose UI envía las notificaciones del
     * snapshot global en cada cuadro; sin interfaz (una prueba de ViewModel) hay que enviarlas a mano,
     * o `snapshotFlow` no se entera del cambio.
     */
    private fun TextFieldState.put(text: String) {
        setTextAndPlaceCursorAtEnd(text)
        Snapshot.sendApplyNotifications()
    }

    private fun EntryFormViewModel.type(amount: String) = this.amount.put(amount)

    private suspend fun add(subject: String?, date: LocalDate = today) {
        entries.add(accountId, EntryDraft(EntryType.EXPENSE, 100, date, subject, null))
    }

    @Test
    fun beforeTheFirstRead_theStateIsNotLoaded() {
        assertFalse(EntryFormUiState().loaded)
        assertFalse(EntryFormUiState().canSave)
    }

    @Test
    fun itLoadsWithTheTypeTheCurrencyOfTheSelectedAccount_andTodayAsTheDate() = runBlocking {
        val state = viewModel(EntryType.EXPENSE).await()

        assertEquals(EntryType.EXPENSE, state.type)
        assertEquals("USD", state.currencyCode)
        assertEquals(today, state.date)
        assertEquals(today, state.today)
        assertTrue(state.isToday)
        assertFalse(state.canSave)
    }

    @Test
    fun anEmptyOrZeroAmount_blocksSaving() = runBlocking {
        val vm = viewModel()
        vm.await()

        listOf("", "0", "0.00", "0,0", ".").forEach { text ->
            vm.type(text)
            assertFalse("«$text» no debe permitir guardar", vm.await().canSave)
        }
    }

    @Test
    fun aPositiveAmount_allowsSaving() = runBlocking {
        val vm = viewModel()
        vm.await()

        vm.type("12.50")

        assertTrue(vm.await { it.canSave }.canSave)
    }

    @Test
    fun aFutureDate_isIgnored_andAPastDate_isAccepted() = runBlocking {
        val vm = viewModel()
        vm.await()

        vm.setDate(today.plusDays(1))
        assertEquals(today, vm.await().date)

        vm.setDate(today.minusDays(3))
        val state = vm.await { it.date == today.minusDays(3) }
        assertEquals(today.minusDays(3), state.date)
        assertFalse(state.isToday)
    }

    @Test
    fun save_storesTheEntry_withTheParsedAmount_trimmedTexts_andOnlyOnce() = runBlocking {
        val vm = viewModel(EntryType.INCOME)
        vm.await()
        vm.type("12,50")
        vm.subject.put("  Café  ")
        vm.note.put("   ")
        vm.setDate(today.minusDays(1))

        vm.save()
        val state = vm.await { it.finished != null }

        assertEquals(FormOutcome.Saved, state.finished)
        val saved = entries.page(accountId, 0, 10).single()
        assertEquals(EntryType.INCOME, saved.type)
        assertEquals(1250L, saved.amountMinor)
        assertEquals(today.minusDays(1), saved.date)
        assertEquals("Café", saved.subject)
        assertNull(saved.note)

        vm.save()
        assertEquals(1, entries.page(accountId, 0, 10).size)
    }

    @Test
    fun save_withoutAValidAmount_doesNothing() = runBlocking {
        val vm = viewModel()
        vm.await()
        vm.type("0")

        vm.save()

        assertNull(vm.await().finished)
        assertTrue(entries.page(accountId, 0, 10).isEmpty())
    }

    @Test
    fun withAnEmptySubject_suggestionsAreTheMostRecent_atMostFive() = runBlocking {
        listOf("A", "B", "C", "D", "E", "F").forEachIndexed { i, s -> add(s, today.minusDays(10L - i)) }
        val vm = viewModel()

        val state = vm.await { it.suggestions.isNotEmpty() }

        assertEquals(listOf("F", "E", "D", "C", "B"), state.suggestions)
    }

    @Test
    fun typingASubject_suggestsByPrefix_ignoringCaseAndAccents() = runBlocking {
        add("Café")
        add("Cafetería")
        add("Gasolina")
        val vm = viewModel()
        vm.await()

        vm.subject.put("CAF")

        val state = vm.await { s -> s.suggestions.isNotEmpty() && s.suggestions.all { it.startsWith("Caf") } }
        assertEquals(setOf("Café", "Cafetería"), state.suggestions.toSet())
    }

    @Test
    fun suggestionsOnlyComeFromTheSelectedAccount() = runBlocking {
        val other = (accounts.create("Viaje", "USD") as NameResult.Saved).id
        entries.add(other, EntryDraft(EntryType.EXPENSE, 100, today, "Hotel", null))
        add("Mercado")
        val vm = viewModel()

        val state = vm.await { it.suggestions.isNotEmpty() }

        assertEquals(listOf("Mercado"), state.suggestions)
    }

    // --- Modo editar (SPEC §5.3) ---

    private fun editViewModel(entryId: Long) = EntryFormViewModel(
        selection = AccountSelection(accounts, SelectedAccountStore(prefs)),
        entries = entries,
        clock = clock,
        mode = EntryFormMode.Edit(entryId),
    ).also { created += it }

    private suspend fun existingEntry(
        amountMinor: Long = 1250, subject: String? = "Taxi", note: String? = "Aeropuerto",
        date: LocalDate = today.minusDays(2), type: EntryType = EntryType.EXPENSE,
    ): Long = (entries.add(accountId, EntryDraft(type, amountMinor, date, subject, note)) as com.v4atu.bandtracker.data.EntryResult.Saved).id

    @Test
    fun edit_loadsTheEntryIntoTheForm() = runBlocking {
        val id = existingEntry()
        val vm = editViewModel(id)

        val state = vm.await()

        assertTrue(state.isEdit)
        assertEquals(EntryType.EXPENSE, state.type)
        assertEquals("USD", state.currencyCode)
        assertEquals(today.minusDays(2), state.date)
        assertFalse(state.isToday)
        assertTrue("el registro ya guardado es válido", state.canSave)
        assertEquals("12.5", vm.amount.text.toString())
        assertEquals("Taxi", vm.subject.text.toString())
        assertEquals("Aeropuerto", vm.note.text.toString())
    }

    @Test
    fun edit_keepsTheTypeOfTheEntry_whateverTheSelectedAccount() = runBlocking {
        val id = existingEntry(type = EntryType.INCOME)

        assertEquals(EntryType.INCOME, editViewModel(id).await().type)
    }

    @Test
    fun edit_save_updatesTheSameEntry_keepingItsIdentityAndAccount() = runBlocking {
        val id = existingEntry()
        val before = entries.find(id)!!
        val vm = editViewModel(id)
        vm.await()

        vm.type("20")
        vm.subject.put("  Bus  ")
        vm.note.put("")
        vm.setDate(today.minusDays(5))
        vm.save()

        assertEquals(FormOutcome.Saved, vm.await { it.finished != null }.finished)
        val after = entries.find(id)!!
        assertEquals(2000L, after.amountMinor)
        assertEquals("Bus", after.subject)
        assertNull(after.note)
        assertEquals(today.minusDays(5), after.date)
        assertEquals(before.uuid, after.uuid)
        assertEquals(before.accountId, after.accountId)
        assertEquals(before.type, after.type)
        assertEquals("no se crea otro registro", 1, entries.page(accountId, 0, 10).size)
    }

    @Test
    fun edit_save_withoutChanges_leavesTheEntryAsItWas() = runBlocking {
        val id = existingEntry()
        val before = entries.find(id)
        val vm = editViewModel(id)
        vm.await()

        vm.save()

        vm.await { it.finished != null }
        assertEquals(before, entries.find(id))
    }

    @Test
    fun edit_aZeroAmount_blocksSaving() = runBlocking {
        val vm = editViewModel(existingEntry())
        vm.await()

        vm.type("0")
        vm.save()

        assertFalse(vm.await { !it.canSave }.canSave)
        assertNull(vm.state.value.finished)
        assertEquals(1250L, entries.page(accountId, 0, 10).single().amountMinor)
    }

    @Test
    fun edit_aFutureDate_isIgnored() = runBlocking {
        val vm = editViewModel(existingEntry())
        vm.await()

        vm.setDate(today.plusDays(1))

        assertEquals(today.minusDays(2), vm.await().date)
    }

    @Test
    fun edit_delete_removesTheEntry_andFinishesAsDeleted() = runBlocking {
        val id = existingEntry()
        existingEntry(subject = "otro")
        val vm = editViewModel(id)
        vm.await()

        vm.delete()

        assertEquals(FormOutcome.Deleted, vm.await { it.finished != null }.finished)
        assertNull(entries.find(id))
        assertEquals("solo se borra ese registro", listOf("otro"), entries.page(accountId, 0, 10).map { it.subject })
    }

    @Test
    fun edit_ofAnEntryThatNoLongerExists_finishesAsDeleted_withoutCrashing() = runBlocking {
        val vm = editViewModel(entryId = 9_999)

        val state = withTimeout(5_000) { vm.state.first { it.finished != null } }

        assertEquals(FormOutcome.Deleted, state.finished)
    }

    @Test
    fun create_delete_doesNothing() = runBlocking {
        val vm = viewModel()
        vm.await()

        vm.delete()

        assertNull(vm.await().finished)
    }

    @Test
    fun edit_suggestionsComeFromTheAccountOfTheEntry_byThePrefixTyped() = runBlocking {
        val id = existingEntry(subject = "Taxi")
        add(subject = "Tapas")
        val viaje = (accounts.create("Viaje", "USD") as NameResult.Saved).id
        entries.add(viaje, EntryDraft(EntryType.EXPENSE, 100, today, "Taller", null))
        val vm = editViewModel(id)
        vm.await()

        vm.subject.put("Ta")

        val state = vm.await { it.suggestions.size == 2 }
        assertEquals(setOf("Taxi", "Tapas"), state.suggestions.toSet())
    }

    @Test
    fun edit_withTheSubjectAlreadyLoaded_suggestsByThatSubject() = runBlocking {
        val id = existingEntry(subject = "Taxi")
        add(subject = "Mercado")
        val vm = editViewModel(id)
        vm.await()
        Snapshot.sendApplyNotifications()

        val state = vm.await { it.suggestions == listOf("Taxi") }

        assertEquals(listOf("Taxi"), state.suggestions)
    }

    @Test
    fun edit_theAmountTextRoundTrips_withoutChangingTheValue() = runBlocking {
        val id = existingEntry(amountMinor = 1_500_000)
        val vm = editViewModel(id)
        vm.await()

        vm.save()

        vm.await { it.finished != null }
        assertEquals(1_500_000L, entries.find(id)!!.amountMinor)
    }
}
