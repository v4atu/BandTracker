package com.v4atu.bandtracker.ui.entry

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.v4atu.bandtracker.AppContainer
import com.v4atu.bandtracker.data.Account
import com.v4atu.bandtracker.data.AccountSelection
import com.v4atu.bandtracker.data.DeviceClock
import com.v4atu.bandtracker.data.EntryDraft
import com.v4atu.bandtracker.data.EntryRepository
import com.v4atu.bandtracker.data.EntryResult
import com.v4atu.bandtracker.domain.EntryDates
import com.v4atu.bandtracker.domain.EntryType
import com.v4atu.bandtracker.domain.Money
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Para qué se abre el formulario (SPEC §5.2 crear, §5.3 editar). */
sealed interface EntryFormMode {
    data class Create(val type: EntryType) : EntryFormMode
    data class Edit(val entryId: Long) : EntryFormMode
}

/** Cómo terminó el formulario; la pantalla vuelve atrás al ver un valor aquí. */
enum class FormOutcome { Saved, Deleted }

/**
 * Estado del formulario. [loaded] es `false` hasta conocer la cuenta (y, al editar, el registro).
 * [canSave] exige un monto mayor que 0 en la moneda de la cuenta (SPEC §5.2). El texto de los campos
 * vive en los `TextFieldState` del ViewModel, no aquí.
 */
data class EntryFormUiState(
    val loaded: Boolean = false,
    val isEdit: Boolean = false,
    val type: EntryType = EntryType.INCOME,
    val currencyCode: String = "",
    val date: LocalDate = LocalDate.EPOCH,
    val today: LocalDate = LocalDate.EPOCH,
    val canSave: Boolean = false,
    val suggestions: List<String> = emptyList(),
    val finished: FormOutcome? = null,
) {
    val isToday: Boolean get() = date == today
}

@OptIn(ExperimentalCoroutinesApi::class)
class EntryFormViewModel(
    private val selection: AccountSelection,
    private val entries: EntryRepository,
    private val clock: Clock,
    private val mode: EntryFormMode,
) : ViewModel() {

    val amount = TextFieldState()
    val subject = TextFieldState()
    val note = TextFieldState()

    /** Lo que se conoce una vez cargado: la cuenta del registro y su tipo. */
    private class Form(val account: Account, val type: EntryType)

    private val form = MutableStateFlow<Form?>(null)
    private val date = MutableStateFlow(EntryDates.today(clock))
    private val finished = MutableStateFlow<FormOutcome?>(null)
    private var busy = false

    init {
        viewModelScope.launch {
            when (mode) {
                is EntryFormMode.Create -> selection.selected.filterNotNull().collect { form.value = Form(it, mode.type) }
                is EntryFormMode.Edit -> load(mode.entryId)
            }
        }
    }

    /** Carga el registro en los campos. Si ya no existe (o su cuenta tampoco), no hay nada que editar. */
    private suspend fun load(entryId: Long) {
        val entry = entries.find(entryId)
        val account = entry?.let { e -> selection.state.first().accounts.firstOrNull { it.id == e.accountId } }
        if (entry == null || account == null) {
            finished.value = FormOutcome.Deleted
            return
        }
        amount.setTextAndPlaceCursorAtEnd(Money.toInput(entry.amountMinor, account.currency))
        subject.setTextAndPlaceCursorAtEnd(entry.subject.orEmpty())
        note.setTextAndPlaceCursorAtEnd(entry.note.orEmpty())
        date.value = entry.date
        form.value = Form(account, entry.type)
    }

    private val amountText = snapshotFlow { amount.text.toString() }

    private val suggestions = form.filterNotNull()
        .combine(snapshotFlow { subject.text.toString() }) { form, text -> form.account.id to text }
        .mapLatest { (accountId, text) -> entries.suggestSubjects(accountId, text) }
        .onStart { emit(emptyList()) }

    val state: StateFlow<EntryFormUiState> = combine(
        form, date, amountText, suggestions, finished,
    ) { form, date, _, suggestions, finished ->
        // `amountText` solo avisa de que el texto cambió; se lee el valor actual aquí porque el aviso
        // llega de forma asíncrona y, al cargar un registro, podría traer todavía el texto anterior.
        val amountText = amount.text.toString()
        if (form == null) {
            EntryFormUiState(finished = finished)
        } else {
            EntryFormUiState(
                loaded = true,
                isEdit = mode is EntryFormMode.Edit,
                type = form.type,
                currencyCode = form.account.currency,
                date = date,
                today = EntryDates.today(clock),
                canSave = Money.parse(amountText, form.account.currency)?.let(Money::isValidAmount) == true,
                suggestions = suggestions,
                finished = finished,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntryFormUiState())

    /** Los días posteriores a hoy no se admiten (SPEC §4): el valor se ignora. */
    fun setDate(newDate: LocalDate) {
        if (EntryDates.isAllowed(newDate, EntryDates.today(clock))) date.value = newDate
    }

    fun save() {
        val form = form.value ?: return
        if (busy) return
        busy = true
        viewModelScope.launch {
            val amountMinor = Money.parse(amount.text.toString(), form.account.currency)
            if (amountMinor == null || !Money.isValidAmount(amountMinor)) {
                busy = false
                return@launch
            }
            val draft = EntryDraft(
                type = form.type,
                amountMinor = amountMinor,
                date = date.value,
                subject = subject.text.toString(),
                note = note.text.toString(),
            )
            val result = when (mode) {
                is EntryFormMode.Create -> entries.add(form.account.id, draft)
                is EntryFormMode.Edit -> entries.update(mode.entryId, draft)
            }
            when (result) {
                is EntryResult.Saved -> finished.value = FormOutcome.Saved
                // La interfaz no deja llegar aquí; si ocurre, el formulario sigue abierto para corregir.
                EntryResult.InvalidAmount, EntryResult.FutureDate -> busy = false
            }
        }
    }

    /** Borra el registro que se está editando (la pantalla ya pidió confirmación). Solo al editar. */
    fun delete() {
        val edit = mode as? EntryFormMode.Edit ?: return
        if (busy) return
        busy = true
        viewModelScope.launch {
            entries.delete(edit.entryId)
            finished.value = FormOutcome.Deleted
        }
    }

    companion object {
        fun factory(container: AppContainer, mode: EntryFormMode): ViewModelProvider.Factory = viewModelFactory {
            initializer { EntryFormViewModel(container.selection, container.entries, DeviceClock, mode) }
        }
    }
}
