package com.v4atu.bandtracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.v4atu.bandtracker.AppContainer
import com.v4atu.bandtracker.data.Account
import com.v4atu.bandtracker.data.AccountSelection
import com.v4atu.bandtracker.data.Entry
import com.v4atu.bandtracker.data.EntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado del Historial. [loaded] es `false` hasta la primera lectura. [entries] son los registros
 * cargados hasta ahora (más reciente primero); [items] los intercala con separadores de mes y
 * encabezados de día. [canLoadMore] indica si la última página llegó completa (puede haber más).
 * [loadingMore] evita pedir dos veces la misma página.
 */
data class HistoryUiState(
    val loaded: Boolean = false,
    val account: Account? = null,
    val entries: List<Entry> = emptyList(),
    val canLoadMore: Boolean = false,
    /** Hay una página pidiéndose. Va dentro del estado para cambiar a la vez que las entradas, sin ventana intermedia. */
    val loadingMore: Boolean = false,
) {
    val items: List<HistoryItem> = HistoryItems.of(entries)
}

/**
 * Historial de la cuenta seleccionada (SPEC §5.3). Consultas acotadas: se cargan páginas de
 * [pageSize] registros a medida que se avanza por la lista, sin Paging (decisión del proyecto).
 */
class HistoryViewModel(
    private val selection: AccountSelection,
    private val entries: EntryRepository,
    private val pageSize: Int = PAGE_SIZE,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private var loadedPages = 0

    init {
        viewModelScope.launch {
            var currentId: Long? = null
            selection.selected.collect { account ->
                when {
                    account == null -> {
                        currentId = null
                        loadedPages = 0
                        _state.value = HistoryUiState(loaded = true)
                    }
                    account.id != currentId -> {
                        currentId = account.id
                        loadedPages = 0
                        _state.value = HistoryUiState(loaded = false, account = account)
                        loadFirstPage(account)
                    }
                    // Mismo id con otro nombre: solo cambia el título.
                    else -> _state.update { it.copy(account = account) }
                }
            }
        }
    }

    private suspend fun loadFirstPage(account: Account) {
        val page = entries.page(account.id, 0, pageSize)
        loadedPages = 1
        _state.value = HistoryUiState(loaded = true, account = account, entries = page, canLoadMore = page.size == pageSize)
    }

    /** Agrega la página siguiente. Sin efecto si no hay más o ya hay una carga en curso. */
    fun loadMore() {
        // «Cargando» se marca en el mismo paso atómico que decide si hay algo que cargar: dos llamadas
        // seguidas no piden la misma página, y quien reacciona al estado nuevo nunca se topa con un «ocupado» viejo.
        var account: Account? = null
        _state.update { current ->
            val candidate = current.account
            if (candidate != null && current.canLoadMore && !current.loadingMore) {
                account = candidate
                current.copy(loadingMore = true)
            } else {
                account = null
                current
            }
        }
        val target = account ?: return

        viewModelScope.launch {
            val page = try {
                entries.page(target.id, loadedPages, pageSize)
            } catch (e: Throwable) {
                _state.update { it.copy(loadingMore = false) }
                throw e
            }
            // Si mientras tanto cambió la cuenta, el estado ya se reinició y esta página no corresponde.
            if (_state.value.account?.id == target.id) {
                // El contador sube antes de publicar: quien reaccione al estado nuevo pedirá la página que sigue.
                loadedPages++
                _state.update { it.copy(entries = it.entries + page, canLoadMore = page.size == pageSize, loadingMore = false) }
            }
        }
    }

    /**
     * Vuelve a leer todo lo cargado, p. ej. al regresar de editar o borrar un registro. Pide la misma
     * cantidad de registros que había (como mínimo una página) para que la lista no cambie de largo.
     */
    fun reload() {
        val account = _state.value.account ?: return
        viewModelScope.launch {
            val pages = loadedPages.coerceAtLeast(1)
            val page = entries.page(account.id, 0, pages * pageSize)
            if (_state.value.account?.id == account.id) {
                _state.update { it.copy(entries = page, canLoadMore = page.size == pages * pageSize) }
            }
        }
    }

    companion object {
        /** Cuántos registros se piden por consulta. */
        const val PAGE_SIZE = 50

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { HistoryViewModel(container.selection, container.entries) }
        }
    }
}
