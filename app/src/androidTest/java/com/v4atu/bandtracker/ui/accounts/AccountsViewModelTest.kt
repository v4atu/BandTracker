package com.v4atu.bandtracker.ui.accounts

import androidx.lifecycle.viewModelScope
import com.v4atu.bandtracker.backup.BackupError
import com.v4atu.bandtracker.backup.BackupService
import com.v4atu.bandtracker.data.AccountRepository
import com.v4atu.bandtracker.data.DatabaseTestBase
import com.v4atu.bandtracker.data.EntryDraft
import com.v4atu.bandtracker.data.EntryRepository
import com.v4atu.bandtracker.data.NameResult
import com.v4atu.bandtracker.domain.EntryType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** SPEC §5.4 y §7: lista de cuentas, renombrar, borrar con conteo, exportar e importar. */
class AccountsViewModelTest : DatabaseTestBase() {

    private val today = LocalDate.of(2026, 11, 6)
    private val clock = Clock.fixed(Instant.parse("2026-11-06T15:00:00Z"), ZoneOffset.UTC)

    private lateinit var accounts: AccountRepository
    private lateinit var entries: EntryRepository
    private lateinit var backup: BackupService
    private var counter = 0
    private val created = mutableListOf<AccountsViewModel>()

    @Before
    fun setUp() {
        accounts = AccountRepository(db) { "acc-${++counter}" }
        entries = EntryRepository(db, clock) { "ent-${++counter}" }
        backup = BackupService(db, clock)
    }

    @After
    fun stopViewModels() {
        created.forEach { it.viewModelScope.cancel() }
    }

    private fun viewModel() = AccountsViewModel(accounts, backup, clock).also { created += it }

    private suspend fun AccountsViewModel.await(predicate: (AccountsUiState) -> Boolean = { true }) =
        withTimeout(5_000) { state.first { it.loaded && predicate(it) } }

    private suspend fun account(name: String, currency: String = "COP") = (accounts.create(name, currency) as NameResult.Saved).id

    private suspend fun entry(accountId: Long, subject: String) {
        entries.add(accountId, EntryDraft(EntryType.EXPENSE, 1000, today, subject, null))
    }

    // --- Lista ---

    @Test
    fun beforeTheFirstRead_theStateIsNotLoaded() {
        assertTrue(!AccountsUiState().loaded)
    }

    @Test
    fun listsTheAccountsByName_andFollowsChanges() = runBlocking {
        account("Viaje", "USD")
        account("Efectivo")
        val vm = viewModel()

        assertEquals(listOf("Efectivo", "Viaje"), vm.await { it.accounts.size == 2 }.accounts.map { it.name })

        account("Ahorros")
        assertEquals(listOf("Ahorros", "Efectivo", "Viaje"), vm.await { it.accounts.size == 3 }.accounts.map { it.name })
    }

    @Test
    fun withoutAccounts_theListIsEmpty() = runBlocking {
        assertTrue(viewModel().await().accounts.isEmpty())
    }

    // --- Renombrar ---

    @Test
    fun requestRename_opensTheDialogForThatAccount_andCancelClosesIt() = runBlocking {
        val id = account("Efectivo")
        val vm = viewModel()
        vm.await { it.accounts.isNotEmpty() }

        vm.requestRename(id)
        assertEquals("Efectivo", vm.await { it.renaming != null }.renaming?.name)

        vm.cancelRename()
        assertNull(vm.await { it.renaming == null }.renaming)
    }

    @Test
    fun confirmRename_changesTheName_andClosesTheDialog() = runBlocking {
        val id = account("Efectivo")
        val vm = viewModel()
        vm.await { it.accounts.isNotEmpty() }
        vm.requestRename(id)
        vm.await { it.renaming != null }

        vm.confirmRename("  Caja  ")

        vm.await { it.renaming == null && it.accounts.single().name == "Caja" }
        assertEquals("Caja", accounts.list().single().name)
    }

    @Test
    fun confirmRename_withANameThatExists_changesNothing_andKeepsTheDialogOpen() = runBlocking {
        val id = account("Efectivo")
        account("Viaje", "USD")
        val vm = viewModel()
        vm.await { it.accounts.size == 2 }
        vm.requestRename(id)
        vm.await { it.renaming != null }

        vm.confirmRename("viaje")

        assertEquals(setOf("Efectivo", "Viaje"), accounts.list().map { it.name }.toSet())
        assertEquals("Efectivo", vm.await { it.renaming != null }.renaming?.name)
    }

    @Test
    fun confirmRename_toTheSameNameWithOtherCase_isAllowed() = runBlocking {
        val id = account("Efectivo")
        val vm = viewModel()
        vm.await { it.accounts.isNotEmpty() }
        vm.requestRename(id)
        vm.await { it.renaming != null }

        vm.confirmRename("EFECTIVO")

        val state = vm.await { it.renaming == null && it.accounts.single().name == "EFECTIVO" }
        assertEquals("EFECTIVO", state.accounts.single().name)
    }

    @Test
    fun confirmRename_withABlankName_changesNothing() = runBlocking {
        val id = account("Efectivo")
        val vm = viewModel()
        vm.await { it.accounts.isNotEmpty() }
        vm.requestRename(id)
        vm.await { it.renaming != null }

        vm.confirmRename("   ")

        assertEquals("Efectivo", accounts.list().single().name)
    }

    @Test
    fun renameCheck_reportsBlankDuplicateAndOk_ignoringTheAccountsOwnName() = runBlocking {
        val efectivo = account("Efectivo")
        account("Viaje", "USD")
        val vm = viewModel()
        val state = vm.await { it.accounts.size == 2 }

        assertEquals(com.v4atu.bandtracker.domain.NameCheck.BLANK, state.renameCheck(efectivo, " "))
        assertEquals(com.v4atu.bandtracker.domain.NameCheck.DUPLICATE, state.renameCheck(efectivo, "VIAJE"))
        assertEquals(com.v4atu.bandtracker.domain.NameCheck.OK, state.renameCheck(efectivo, "efectivo"))
        assertEquals(com.v4atu.bandtracker.domain.NameCheck.OK, state.renameCheck(efectivo, "Caja"))
    }

    // --- Borrar ---

    @Test
    fun requestDelete_showsHowManyEntriesWouldBeLost() = runBlocking {
        val id = account("Efectivo")
        entry(id, "a"); entry(id, "b"); entry(id, "c")
        val vm = viewModel()
        vm.await { it.accounts.isNotEmpty() }

        vm.requestDelete(id)

        val request = vm.await { it.deleting != null }.deleting!!
        assertEquals("Efectivo", request.account.name)
        assertEquals(3, request.entryCount)
    }

    @Test
    fun cancelDelete_changesNothing() = runBlocking {
        val id = account("Efectivo")
        entry(id, "a")
        val vm = viewModel()
        vm.await { it.accounts.isNotEmpty() }
        vm.requestDelete(id)
        vm.await { it.deleting != null }

        vm.cancelDelete()

        assertNull(vm.await { it.deleting == null }.deleting)
        assertEquals(1, accounts.list().size)
        assertEquals(1, accounts.entryCount(id))
    }

    @Test
    fun confirmDelete_removesTheAccountAndItsEntries_butNotOthers() = runBlocking {
        val efectivo = account("Efectivo")
        val viaje = account("Viaje", "USD")
        entry(efectivo, "a"); entry(viaje, "z")
        val vm = viewModel()
        vm.await { it.accounts.size == 2 }
        vm.requestDelete(efectivo)
        vm.await { it.deleting != null }

        vm.confirmDelete()

        vm.await { it.deleting == null && it.accounts.size == 1 }
        assertEquals(listOf("Viaje"), accounts.list().map { it.name })
        assertEquals(1, accounts.entryCount(viaje))
        assertEquals(0, accounts.entryCount(efectivo))
    }

    // --- Exportar ---

    @Test
    fun exportJson_hasEverythingTheBackupServiceExports() = runBlocking {
        val id = account("Efectivo")
        entry(id, "taxi")
        val vm = viewModel()

        val json = vm.exportJson()

        assertTrue(json.contains("Efectivo") && json.contains("taxi") && json.contains("\"version\""))
        assertEquals(backup.exportJson(), json)
    }

    @Test
    fun theSuggestedFileName_carriesTodaysDate() {
        assertEquals("bandtracker-2026-11-06.json", viewModel().suggestedFileName())
    }

    // --- Importar ---

    @Test
    fun importingAValidBackup_mergesIt_andReportsASummary() = runBlocking {
        val id = account("Efectivo")
        entry(id, "taxi"); entry(id, "bus")
        val vm = viewModel()
        val json = vm.exportJson()
        accounts.delete(id)
        vm.await { it.accounts.isEmpty() }

        vm.importJson(json)

        val outcome = vm.await { it.importOutcome != null }.importOutcome as ImportOutcome.Success
        assertEquals(1, outcome.summary.accountsAdded)
        assertEquals(2, outcome.summary.entriesAdded)
        assertEquals(listOf("Efectivo"), accounts.list().map { it.name })
        assertEquals(2, accounts.entryCount(accounts.list().single().id))
    }

    @Test
    fun importingTheSameBackupTwice_addsNothingTheSecondTime() = runBlocking {
        val id = account("Efectivo")
        entry(id, "taxi")
        val vm = viewModel()
        val json = vm.exportJson()

        vm.importJson(json)

        val outcome = vm.await { it.importOutcome != null }.importOutcome as ImportOutcome.Success
        assertEquals(0, outcome.summary.accountsAdded)
        assertEquals(0, outcome.summary.entriesAdded)
        assertEquals(1, accounts.list().size)
        assertEquals(1, accounts.entryCount(id))
    }

    @Test
    fun importingAnInvalidFile_isRejected_andChangesNothing() = runBlocking {
        val id = account("Efectivo")
        entry(id, "taxi")
        val vm = viewModel()
        vm.await { it.accounts.isNotEmpty() }

        vm.importJson("esto no es json")

        val outcome = vm.await { it.importOutcome != null }.importOutcome as ImportOutcome.Rejected
        assertEquals(BackupError.NotValidJson, outcome.error)
        assertEquals(1, accounts.list().size)
        assertEquals(1, accounts.entryCount(id))
    }

    @Test
    fun aFileThatCouldNotBeRead_isReportedAsUnreadable() = runBlocking {
        val vm = viewModel()
        vm.await()

        vm.importUnreadable()

        assertEquals(ImportOutcome.Unreadable, vm.await { it.importOutcome != null }.importOutcome)
    }

    @Test
    fun dismissingTheOutcome_clearsIt() = runBlocking {
        val vm = viewModel()
        vm.await()
        vm.importJson("no")
        vm.await { it.importOutcome != null }

        vm.dismissImportOutcome()

        assertNull(vm.await { it.importOutcome == null }.importOutcome)
    }
}
