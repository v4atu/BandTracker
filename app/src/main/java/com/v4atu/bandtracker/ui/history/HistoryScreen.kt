package com.v4atu.bandtracker.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.v4atu.bandtracker.domain.EntryRowText
import com.v4atu.bandtracker.domain.Money
import com.v4atu.bandtracker.ui.components.BandText
import com.v4atu.bandtracker.ui.components.DayHeader
import com.v4atu.bandtracker.ui.components.EntryRow
import com.v4atu.bandtracker.ui.components.MonthHeader
import com.v4atu.bandtracker.ui.theme.BandTrackerDimens
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme

/** Cuántos elementos antes del final se pide la página siguiente. */
private const val LOAD_AHEAD = 10

/**
 * Historial (SPEC §5.3, DESIGN §7 «5.3 Historial»): el nombre de la cuenta arriba y la lista de
 * registros del más reciente al más antiguo, con separador de mes y encabezado de día. Tocar una fila
 * abre su edición. Sin totales (v1).
 */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onEntryClick: (Long) -> Unit,
    onLoadMore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = BandTrackerDimens.ScreenHorizontal, top = BandTrackerDimens.ScreenTop, end = BandTrackerDimens.ScreenHorizontal),
        verticalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space16),
    ) {
        // Hasta conocer la cuenta no se dibuja nada; si la cuenta desaparece, tampoco.
        val account = state.account ?: return@Column
        val locale = LocalConfiguration.current.locales[0]

        BandText(text = account.name, style = BandTrackerTheme.text.action, color = MaterialTheme.colorScheme.onBackground)

        val listState = rememberLazyListState()
        val itemCount = state.items.size
        LaunchedEffect(listState, state.canLoadMore, itemCount) {
            if (!state.canLoadMore) return@LaunchedEffect
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
                .collect { last -> if (last >= itemCount - LOAD_AHEAD) onLoadMore() }
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = BandTrackerDimens.ScreenBottom),
        ) {
            items(state.items, key = { it.key }, contentType = { it::class }) { item ->
                when (item) {
                    is HistoryItem.Month -> MonthHeader(item.yearMonth, locale = locale)
                    is HistoryItem.Day -> DayHeader(item.date, locale = locale)
                    is HistoryItem.Row -> {
                        val entry = item.entry
                        EntryRow(
                            text = EntryRowText.of(entry.subject, entry.note),
                            amount = Money.formatSigned(entry.type, entry.amountMinor, account.currency, locale),
                            type = entry.type,
                            onClick = { onEntryClick(entry.id) },
                        )
                    }
                }
            }
        }
    }
}
