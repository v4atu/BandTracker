package com.v4atu.bandtracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.v4atu.bandtracker.AppContainer
import com.v4atu.bandtracker.data.Account
import com.v4atu.bandtracker.data.AccountSelection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Estado de Inicio. [loaded] es `false` hasta la primera lectura de la base de datos: así no se
 * muestra por un instante el estado «sin cuentas» a quien sí tiene cuentas.
 */
data class HomeUiState(
    val loaded: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val selected: Account? = null,
)

class HomeViewModel(private val selection: AccountSelection) : ViewModel() {

    val state: StateFlow<HomeUiState> = selection.state
        .map { HomeUiState(loaded = true, accounts = it.accounts, selected = it.selected) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun select(accountId: Long) = selection.select(accountId)

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(container.selection) }
        }
    }
}
