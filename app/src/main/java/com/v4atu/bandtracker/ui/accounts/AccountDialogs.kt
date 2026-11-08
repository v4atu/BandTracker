package com.v4atu.bandtracker.ui.accounts

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.backup.ImportSummary
import com.v4atu.bandtracker.data.Account
import com.v4atu.bandtracker.domain.NameCheck
import com.v4atu.bandtracker.ui.components.BandText
import com.v4atu.bandtracker.ui.components.ConfirmDialog
import com.v4atu.bandtracker.ui.components.DialogShell
import com.v4atu.bandtracker.ui.components.FormField
import com.v4atu.bandtracker.ui.components.InfoDialog
import com.v4atu.bandtracker.ui.components.TextButton
import com.v4atu.bandtracker.ui.components.Tone
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme

/**
 * Renombrar una cuenta (SPEC §5.4, Penpot «5.4 Renombrar cuenta»): el campo viene con el nombre actual.
 * [check] valida lo escrito; si el nombre ya existe se explica y «Guardar» queda deshabilitado.
 */
@Composable
fun RenameAccountDialog(
    account: Account,
    check: (String) -> NameCheck,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val name = rememberTextFieldState(account.name)
    val result = check(name.text.toString())

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogShell(
            title = stringResource(R.string.account_rename_title),
            actions = {
                TextButton(text = stringResource(R.string.action_cancel), tone = Tone.Neutral, onClick = onDismiss)
                TextButton(
                    text = stringResource(R.string.action_save),
                    tone = Tone.Income,
                    enabled = result == NameCheck.OK,
                    onClick = { onConfirm(name.text.toString()) },
                )
            },
        ) {
            FormField(
                label = stringResource(R.string.account_name_label),
                state = name,
                isError = result == NameCheck.DUPLICATE,
            )
            if (result == NameCheck.DUPLICATE) {
                BandText(
                    text = stringResource(R.string.account_name_duplicate),
                    style = BandTrackerTheme.text.note,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * Borrar una cuenta (SPEC §5.4, Penpot «5.4 Borrar cuenta»): confirmación fuerte que dice cuántos
 * registros se perderían, porque se borran junto con la cuenta y no hay deshacer.
 */
@Composable
fun DeleteAccountDialog(request: DeleteRequest, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = stringResource(R.string.account_delete_title, request.account.name),
        message = stringResource(R.string.account_delete_message),
        confirmText = stringResource(R.string.account_delete_confirm),
        dismissText = stringResource(R.string.action_cancel),
        destructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        warning = pluralStringResource(R.plurals.account_delete_count, request.entryCount, request.entryCount),
    )
}

/**
 * Resultado de una importación (SPEC §7): si se rechazó, por qué y que no se cambió ningún dato
 * (Penpot «5.4 Error de importación»); si salió bien, un resumen de lo agregado y lo que ya existía.
 */
@Composable
fun ImportOutcomeDialog(outcome: ImportOutcome, onDismiss: () -> Unit) {
    val title: String
    val message: String
    when (outcome) {
        is ImportOutcome.Success -> {
            title = stringResource(R.string.import_done_title)
            message = summaryMessage(outcome.summary)
        }
        is ImportOutcome.Rejected -> {
            title = stringResource(R.string.import_error_title)
            val location = ImportMessages.location(outcome.error)?.let { stringResource(R.string.import_error_location, it) }
            message = listOfNotNull(
                stringResource(ImportMessages.message(outcome.error)),
                location,
                stringResource(R.string.import_error_no_changes),
            ).joinToString(" ")
        }
        ImportOutcome.Unreadable -> {
            title = stringResource(R.string.import_error_title)
            message = stringResource(R.string.import_error_unreadable) + " " + stringResource(R.string.import_error_no_changes)
        }
    }
    InfoDialog(title = title, message = message, confirmText = stringResource(R.string.action_accept), onDismiss = onDismiss)
}

@Composable
private fun summaryMessage(summary: ImportSummary): String {
    val added = stringResource(
        R.string.import_done_added,
        pluralStringResource(R.plurals.import_accounts, summary.accountsAdded, summary.accountsAdded),
        pluralStringResource(R.plurals.import_entries, summary.entriesAdded, summary.entriesAdded),
    )
    val skipped = if (summary.accountsSkipped > 0 || summary.entriesSkipped > 0) {
        stringResource(
            R.string.import_done_skipped,
            pluralStringResource(R.plurals.import_accounts, summary.accountsSkipped, summary.accountsSkipped),
            pluralStringResource(R.plurals.import_entries, summary.entriesSkipped, summary.entriesSkipped),
        )
    } else {
        null
    }
    val renamed = summary.renamedAccounts.map { stringResource(R.string.import_done_renamed, it.from, it.to) }
    return (listOfNotNull(added, skipped) + renamed).joinToString("\n")
}
