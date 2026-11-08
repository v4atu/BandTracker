package com.v4atu.bandtracker.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.ui.components.AccountCard
import com.v4atu.bandtracker.ui.components.ActionRow
import com.v4atu.bandtracker.ui.components.BandText
import com.v4atu.bandtracker.ui.components.Tone
import com.v4atu.bandtracker.ui.theme.BandTrackerDimens
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme

/**
 * Cuentas (SPEC §5.4, DESIGN §7 «5.4 Cuentas»): la lista de cuentas con su moneda y, en la zona del
 * pulgar, las acciones Nueva cuenta, Exportar e Importar.
 */
@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onCreate: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(
                start = BandTrackerDimens.ScreenHorizontal,
                top = BandTrackerDimens.ScreenTop,
                end = BandTrackerDimens.ScreenHorizontal,
                bottom = BandTrackerDimens.ScreenBottom,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space24),
    ) {
        if (!state.loaded) return@Column

        BandText(
            text = stringResource(R.string.accounts_title),
            style = BandTrackerTheme.text.title,
            color = MaterialTheme.colorScheme.onBackground,
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.accounts, key = { it.id }) { account ->
                AccountCard(name = account.name, currencyCode = account.currency)
            }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionRow(R.drawable.ic_plus, stringResource(R.string.account_new_title), Tone.Income, onCreate)
            ActionRow(R.drawable.ic_upload, stringResource(R.string.accounts_export), Tone.Neutral, onExport)
            ActionRow(R.drawable.ic_download, stringResource(R.string.accounts_import), Tone.Neutral, onImport)
        }
    }
}
