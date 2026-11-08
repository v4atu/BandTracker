package com.v4atu.bandtracker.ui.accounts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.v4atu.bandtracker.AppContainer
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.ui.components.InfoDialog
import kotlinx.coroutines.launch

/** «Nueva cuenta» con su ViewModel. [onFinished] se llama una vez que la cuenta quedó creada. */
@Composable
fun CreateAccountRoute(container: AppContainer, onFinished: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val viewModel: CreateAccountViewModel = viewModel(factory = CreateAccountViewModel.factory(container, locale))
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.finished) { if (state.finished) onFinished() }

    CreateAccountScreen(
        state = state,
        name = viewModel.name,
        query = viewModel.query,
        onSelectCurrency = viewModel::selectCurrency,
        onCreate = viewModel::create,
    )
}

/**
 * «Cuentas» con su ViewModel, el selector de archivos del sistema para exportar e importar
 * (`CreateDocument` / `OpenDocument`, sin permisos de almacenamiento, SPEC §7) y los diálogos.
 */
@Composable
fun AccountsRoute(container: AppContainer, onCreate: () -> Unit) {
    val viewModel: AccountsViewModel = viewModel(factory = AccountsViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    var exportFailed by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON)) { uri ->
        if (uri != null) {
            scope.launch {
                val written = BackupFiles.write(resolver, uri, viewModel.exportJson())
                if (!written) exportFailed = true
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = BackupFiles.read(resolver, uri)
                if (text == null) viewModel.importUnreadable() else viewModel.importJson(text)
            }
        }
    }

    AccountsScreen(
        state = state,
        onCreate = onCreate,
        onExport = { exportLauncher.launch(viewModel.suggestedFileName()) },
        onImport = { importLauncher.launch(IMPORT_TYPES) },
    )

    state.renaming?.let { account ->
        RenameAccountDialog(
            account = account,
            check = { state.renameCheck(account.id, it) },
            onConfirm = viewModel::confirmRename,
            onDismiss = viewModel::cancelRename,
        )
    }
    state.deleting?.let { request ->
        DeleteAccountDialog(request, onConfirm = viewModel::confirmDelete, onDismiss = viewModel::cancelDelete)
    }
    state.importOutcome?.let { outcome ->
        ImportOutcomeDialog(outcome, onDismiss = viewModel::dismissImportOutcome)
    }
    if (exportFailed) {
        InfoDialog(
            title = stringResource(R.string.export_error_title),
            message = stringResource(R.string.export_error_message),
            confirmText = stringResource(R.string.action_accept),
            onDismiss = { exportFailed = false },
        )
    }
}

private const val JSON = "application/json"

/** Algunos exploradores de archivos no reconocen `.json` y lo marcan como binario o texto: se admiten también. */
private val IMPORT_TYPES = arrayOf(JSON, "application/octet-stream", "text/plain")
