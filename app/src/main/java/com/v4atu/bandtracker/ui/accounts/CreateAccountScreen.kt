package com.v4atu.bandtracker.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.domain.EntryType
import com.v4atu.bandtracker.domain.NameCheck
import com.v4atu.bandtracker.ui.components.BandText
import com.v4atu.bandtracker.ui.components.FormField
import com.v4atu.bandtracker.ui.components.ListOption
import com.v4atu.bandtracker.ui.components.PrimaryButton
import com.v4atu.bandtracker.ui.theme.BandTrackerDimens
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme

private val MdShape = RoundedCornerShape(BandTrackerDimens.RadiusMd)

/** Alto máximo de la lista de monedas (Penpot: 4 + 3 filas de 48 + 4); con más resultados se desplaza. */
private val ResultsMaxHeight = 152.dp

/**
 * Nueva cuenta (SPEC §5.4, DESIGN §7 «5.4 Crear cuenta» y «nombre duplicado»): nombre, moneda con
 * buscador y el aviso de que la moneda no se puede cambiar después. Con el nombre repetido la línea
 * del campo va en rojo, se explica el motivo y «Crear» queda deshabilitado.
 */
@Composable
fun CreateAccountScreen(
    state: CreateAccountUiState,
    name: TextFieldState,
    query: TextFieldState,
    onSelectCurrency: (String) -> Unit,
    onCreate: () -> Unit,
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
            text = stringResource(R.string.account_new_title),
            style = BandTrackerTheme.text.title,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space16),
        ) {
            val duplicate = state.nameCheck == NameCheck.DUPLICATE
            FormField(
                label = stringResource(R.string.account_name_label),
                state = name,
                placeholder = stringResource(R.string.account_name_placeholder),
                isError = duplicate,
            )
            if (duplicate) {
                BandText(
                    text = stringResource(R.string.account_name_duplicate),
                    style = BandTrackerTheme.text.note,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            FormField(
                label = stringResource(R.string.account_currency_label),
                state = query,
                placeholder = stringResource(R.string.account_currency_search),
            )
            if (state.results.isNotEmpty()) {
                CurrencyResults(state, onSelectCurrency)
            }
            BandText(
                text = stringResource(R.string.account_currency_note),
                style = BandTrackerTheme.text.note,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }

        // «Crear» es verde como el botón de Ingreso (Penpot); deshabilitado usa `surface` con texto secundario.
        PrimaryButton(
            text = stringResource(R.string.account_create),
            type = EntryType.INCOME,
            onClick = onCreate,
            enabled = state.canCreate,
        )
    }
}

@Composable
private fun CurrencyResults(state: CreateAccountUiState, onSelectCurrency: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MdShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp),
    ) {
        LazyColumn(Modifier.heightIn(max = ResultsMaxHeight - 8.dp)) {
            items(state.results, key = { it.code }) { option ->
                ListOption(
                    text = option.code,
                    detail = option.name,
                    selected = option.code == state.selectedCurrency,
                    onClick = { onSelectCurrency(option.code) },
                )
            }
        }
    }
}
