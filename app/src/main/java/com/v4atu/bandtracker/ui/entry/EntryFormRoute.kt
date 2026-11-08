package com.v4atu.bandtracker.ui.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.v4atu.bandtracker.AppContainer
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.ui.components.ConfirmDialog

/**
 * El formulario de captura o edición con su ViewModel, el selector de fecha y la confirmación de
 * borrado ya conectados. [onFinished] se llama una vez cuando el registro se guardó o se borró.
 */
@Composable
fun EntryFormRoute(
    container: AppContainer,
    mode: EntryFormMode,
    onFinished: (FormOutcome) -> Unit,
) {
    val viewModel: EntryFormViewModel = viewModel(factory = EntryFormViewModel.factory(container, mode))
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.finished) { state.finished?.let(onFinished) }

    EntryFormScreen(
        state = state,
        amount = viewModel.amount,
        subject = viewModel.subject,
        note = viewModel.note,
        onDateClick = { pickingDate = true },
        onSave = viewModel::save,
        onDelete = { confirmingDelete = true },
    )

    if (pickingDate) {
        DatePickerDialog(
            initial = state.date,
            today = state.today,
            onConfirm = {
                viewModel.setDate(it)
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }

    if (confirmingDelete) {
        ConfirmDialog(
            title = stringResource(R.string.entry_delete_title),
            message = stringResource(R.string.entry_delete_message),
            confirmText = stringResource(R.string.entry_delete),
            dismissText = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = {
                confirmingDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}
