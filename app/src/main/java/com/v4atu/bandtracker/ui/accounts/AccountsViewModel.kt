package com.v4atu.bandtracker.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.v4atu.bandtracker.AppContainer
import com.v4atu.bandtracker.backup.BackupError
import com.v4atu.bandtracker.backup.BackupService
import com.v4atu.bandtracker.backup.ImportResult
import com.v4atu.bandtracker.backup.ImportSummary
import com.v4atu.bandtracker.data.Account
import com.v4atu.bandtracker.data.AccountRepository
import com.v4atu.bandtracker.data.DeviceClock
import com.v4atu.bandtracker.domain.AccountNames
import com.v4atu.bandtracker.domain.BackupFileName
import com.v4atu.bandtracker.domain.EntryDates
import com.v4atu.bandtracker.domain.NameCheck
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Borrar una cuenta pide confirmación y dice cuántos registros se perderían (SPEC §5.4). */
data class DeleteRequest(val account: Account, val entryCount: Int)

/** Cómo terminó una importación (SPEC §7); la pantalla lo muestra en un diálogo. */
sealed interface ImportOutcome {
    data class Success(val summary: ImportSummary) : ImportOutcome
    data class Rejected(val error: BackupError) : ImportOutcome

    /** No se pudo ni leer el archivo elegido. */
    data object Unreadable : ImportOutcome
}

/**
 * Estado de «Cuentas». [renaming] y [deleting] son los diálogos abiertos (cuenta que se renombra o se
 * borra); [importOutcome] el resultado de la última importación, hasta que se descarta.
 */
data class AccountsUiState(
    val loaded: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val renaming: Account? = null,
    val deleting: DeleteRequest? = null,
    val importOutcome: ImportOutcome? = null,
) {
    /** Valida [candidate] como nuevo nombre de la cuenta [accountId]: su propio nombre no cuenta como repetido. */
    fun renameCheck(accountId: Long, candidate: String): NameCheck =
        AccountNames.check(candidate, accounts.map { it.name }, currentName = accounts.firstOrNull { it.id == accountId }?.name)
}

class AccountsViewModel(
    private val accounts: AccountRepository,
    private val backup: BackupService,
    private val clock: Clock,
) : ViewModel() {

    private val renamingId = MutableStateFlow<Long?>(null)
    private val deleting = MutableStateFlow<DeleteRequest?>(null)
    private val importOutcome = MutableStateFlow<ImportOutcome?>(null)

    val state: StateFlow<AccountsUiState> = combine(accounts.accounts, renamingId, deleting, importOutcome) { list, renaming, deleting, outcome ->
        AccountsUiState(
            loaded = true,
            accounts = list,
            renaming = list.firstOrNull { it.id == renaming },
            deleting = deleting,
            importOutcome = outcome,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    // --- Renombrar ---

    fun requestRename(accountId: Long) {
        renamingId.value = accountId
    }

    fun cancelRename() {
        renamingId.value = null
    }

    /** Cambia el nombre si es válido; si no (vacío o repetido) el diálogo sigue abierto. */
    fun confirmRename(newName: String) {
        val id = renamingId.value ?: return
        viewModelScope.launch {
            val existing = accounts.list()
            val current = existing.firstOrNull { it.id == id } ?: return@launch
            if (AccountNames.check(newName, existing.map { it.name }, currentName = current.name) != NameCheck.OK) return@launch
            accounts.rename(id, newName)
            renamingId.value = null
        }
    }

    // --- Borrar ---

    fun requestDelete(accountId: Long) {
        viewModelScope.launch {
            val account = accounts.list().firstOrNull { it.id == accountId } ?: return@launch
            deleting.value = DeleteRequest(account, accounts.entryCount(accountId))
        }
    }

    fun cancelDelete() {
        deleting.value = null
    }

    /** Borra la cuenta y, por cascada, todos sus registros (SPEC §5.4). */
    fun confirmDelete() {
        val request = deleting.value ?: return
        viewModelScope.launch {
            accounts.delete(request.account.id)
            deleting.value = null
        }
    }

    // --- Exportar e importar (SPEC §7) ---

    suspend fun exportJson(): String = backup.exportJson()

    /** Nombre sugerido para el archivo del selector del sistema. */
    fun suggestedFileName(): String = BackupFileName.of(EntryDates.today(clock))

    fun importJson(json: String) {
        viewModelScope.launch {
            importOutcome.value = when (val result = backup.importJson(json)) {
                is ImportResult.Success -> ImportOutcome.Success(result.summary)
                is ImportResult.Rejected -> ImportOutcome.Rejected(result.error)
            }
        }
    }

    fun importUnreadable() {
        importOutcome.value = ImportOutcome.Unreadable
    }

    fun dismissImportOutcome() {
        importOutcome.value = null
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { AccountsViewModel(container.accounts, container.backup, DeviceClock) }
        }
    }
}
