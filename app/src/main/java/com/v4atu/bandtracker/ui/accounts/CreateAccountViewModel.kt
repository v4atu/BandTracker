package com.v4atu.bandtracker.ui.accounts

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.v4atu.bandtracker.AppContainer
import com.v4atu.bandtracker.data.AccountRepository
import com.v4atu.bandtracker.data.AccountSelection
import com.v4atu.bandtracker.data.NameResult
import com.v4atu.bandtracker.domain.AccountNames
import com.v4atu.bandtracker.domain.CurrencyOption
import com.v4atu.bandtracker.domain.CurrencySearch
import com.v4atu.bandtracker.domain.NameCheck
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado de «Nueva cuenta» (SPEC §5.4). [nameCheck] es `BLANK` mientras no hay nombre y `DUPLICATE`
 * si ya existe una cuenta con ese nombre (sin distinguir mayúsculas ni tildes). [results] son las
 * monedas que coinciden con la búsqueda. Solo se puede crear con un nombre válido y una moneda.
 */
data class CreateAccountUiState(
    val loaded: Boolean = false,
    val nameCheck: NameCheck = NameCheck.BLANK,
    val existingNames: List<String> = emptyList(),
    val results: List<CurrencyOption> = emptyList(),
    val selectedCurrency: String? = null,
    val finished: Boolean = false,
) {
    val canCreate: Boolean get() = nameCheck == NameCheck.OK && selectedCurrency != null
}

class CreateAccountViewModel(
    private val accounts: AccountRepository,
    private val selection: AccountSelection,
    private val locale: Locale,
) : ViewModel() {

    val name = TextFieldState()
    val query = TextFieldState()

    private val selectedCurrency = MutableStateFlow<String?>(null)
    private val finished = MutableStateFlow(false)
    private var busy = false

    val state: StateFlow<CreateAccountUiState> = combine(
        accounts.accounts, snapshotFlow { name.text.toString() }, snapshotFlow { query.text.toString() }, selectedCurrency, finished,
    ) { list, _, _, currency, done ->
        // Los textos se leen aquí: los flujos solo avisan del cambio (llega de forma asíncrona).
        val names = list.map { it.name }
        CreateAccountUiState(
            loaded = true,
            nameCheck = AccountNames.check(name.text.toString(), names),
            existingNames = names,
            results = CurrencySearch.search(query.text.toString(), locale),
            selectedCurrency = currency,
            finished = done,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CreateAccountUiState())

    /** Elige la moneda de la cuenta. Un código que no es una moneda ISO 4217 del sistema se ignora. */
    fun selectCurrency(code: String) {
        if (runCatching { Currency.getInstance(code) }.isSuccess) selectedCurrency.value = code
    }

    fun create() {
        if (busy) return
        busy = true
        viewModelScope.launch {
            val existing = accounts.list()
            val currency = selectedCurrency.value
            val candidate = name.text.toString()
            if (currency == null || AccountNames.check(candidate, existing.map { it.name }) != NameCheck.OK) {
                busy = false
                return@launch
            }
            when (val result = accounts.create(candidate, currency)) {
                is NameResult.Saved -> {
                    // La primera cuenta pasa a ser la seleccionada; las demás no cambian la selección.
                    if (existing.isEmpty()) selection.select(result.id)
                    finished.value = true
                }
                is NameResult.Rejected -> busy = false
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, locale: Locale): ViewModelProvider.Factory = viewModelFactory {
            initializer { CreateAccountViewModel(container.accounts, container.selection, locale) }
        }
    }
}
