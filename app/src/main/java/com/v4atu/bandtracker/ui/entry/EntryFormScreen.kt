package com.v4atu.bandtracker.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.domain.DateLabels
import com.v4atu.bandtracker.domain.EntryType
import com.v4atu.bandtracker.ui.components.AmountField
import com.v4atu.bandtracker.ui.components.BandText
import com.v4atu.bandtracker.ui.components.FormField
import com.v4atu.bandtracker.ui.components.PrimaryButton
import com.v4atu.bandtracker.ui.components.TextButton
import com.v4atu.bandtracker.ui.components.Tone
import com.v4atu.bandtracker.ui.theme.BandTrackerDimens
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme

private val MdShape = RoundedCornerShape(BandTrackerDimens.RadiusMd)

/**
 * Captura de Ingreso o Egreso (SPEC §5.2, DESIGN §7: «5.2 Ingreso», «5.2 Egreso» y «sugerencias de
 * sujeto»). Título arriba, el monto y los campos centrados, y el botón de confirmar abajo.
 *
 * Sin estado propio de negocio: el texto vive en [amount], [subject] y [note]; [state] trae lo demás.
 * Las sugerencias solo se ofrecen mientras el campo Sujeto tiene el foco, y empujan «Nota» hacia abajo.
 */
@Composable
fun EntryFormScreen(
    state: EntryFormUiState,
    amount: TextFieldState,
    subject: TextFieldState,
    note: TextFieldState,
    onDateClick: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
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
        // Hasta conocer la cuenta no se dibuja nada: no hay moneda para el monto.
        if (!state.loaded) return@Column

        val income = state.type == EntryType.INCOME
        BandText(
            text = stringResource(
                when {
                    state.isEdit -> R.string.entry_edit_title
                    income -> R.string.home_income
                    else -> R.string.home_expense
                },
            ),
            style = BandTrackerTheme.text.title,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Con el teclado abierto o la fuente muy grande el contenido se desplaza; si cabe, queda centrado.
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
        ) {
            AmountField(
                state = amount,
                currencyCode = state.currencyCode,
                tone = if (income) Tone.Income else Tone.Expense,
            )
            Fields(state, subject, note, onDateClick)
        }

        PrimaryButton(
            text = stringResource(
                when {
                    state.isEdit -> R.string.entry_save
                    income -> R.string.entry_save_income
                    else -> R.string.entry_save_expense
                },
            ),
            type = state.type,
            onClick = onSave,
            enabled = state.canSave,
        )

        // Borrar vive en la edición (SPEC §5.3); la confirmación la pide quien usa la pantalla.
        if (state.isEdit) {
            TextButton(
                text = stringResource(R.string.entry_delete),
                tone = Tone.Expense,
                icon = R.drawable.ic_trash,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun Fields(
    state: EntryFormUiState,
    subject: TextFieldState,
    note: TextFieldState,
    onDateClick: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val focusManager = LocalFocusManager.current
    val optional = stringResource(R.string.entry_optional)
    var subjectFocused by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space16)) {
        FormField(
            label = stringResource(R.string.entry_date),
            value = if (state.isToday) stringResource(R.string.entry_today) else DateLabels.field(state.date, locale),
            trailingIcon = R.drawable.ic_calendar,
            onClick = onDateClick,
        )
        FormField(
            label = stringResource(R.string.entry_subject),
            state = subject,
            placeholder = optional,
            modifier = Modifier.onFocusChanged { subjectFocused = it.isFocused },
        )

        // Lo que ya está escrito no se ofrece como sugerencia.
        val offered = state.suggestions.filterNot { it == subject.text.toString() }
        if (subjectFocused && offered.isNotEmpty()) {
            Suggestions(offered) { suggestion ->
                subject.setTextAndPlaceCursorAtEnd(suggestion)
                focusManager.clearFocus()
            }
        }

        FormField(label = stringResource(R.string.entry_note), state = note, placeholder = optional)
    }
}

/** Sujetos ya usados en la cuenta (DESIGN §6: no es un componente): panel `surface` de radio 12 con filas de 48 dp. */
@Composable
private fun Suggestions(suggestions: List<String>, onPick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MdShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp),
    ) {
        suggestions.forEach { suggestion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = BandTrackerDimens.MinTouchTarget)
                    .clickable(role = Role.Button) { onPick(suggestion) }
                    .padding(horizontal = BandTrackerDimens.Space16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BandText(
                    text = suggestion,
                    style = BandTrackerTheme.text.body,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
