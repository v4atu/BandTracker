package com.v4atu.bandtracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.data.Account
import com.v4atu.bandtracker.ui.components.AccountSelector
import com.v4atu.bandtracker.ui.components.ActionRow
import com.v4atu.bandtracker.ui.components.BandText
import com.v4atu.bandtracker.ui.components.IconAction
import com.v4atu.bandtracker.ui.components.ListOption
import com.v4atu.bandtracker.ui.components.SavedSnackbar
import com.v4atu.bandtracker.ui.components.Tone
import com.v4atu.bandtracker.ui.theme.BandTrackerDimens
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme
import kotlinx.coroutines.delay

/** Qué se acaba de guardar, para mostrar el aviso breve al volver a Inicio (SPEC §5.2). */
enum class SavedNotice { Income, Expense }

/** Lo que dura visible el aviso de guardado (SPEC §5.2: ~2 s). */
private const val NOTICE_MILLIS = 2_000L

private val MdShape = RoundedCornerShape(BandTrackerDimens.RadiusMd)

/**
 * Inicio (SPEC §5.1, DESIGN §7): arriba el selector de cuenta y «?»; abajo, en la zona del pulgar,
 * las tres acciones. Sin cuentas, el único elemento es la fila «Cuenta», centrada.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    notice: SavedNotice?,
    onNoticeShown: () -> Unit,
    onSelectAccount: (Long) -> Unit,
    onIncome: () -> Unit,
    onExpense: () -> Unit,
    onHistory: () -> Unit,
    onAccounts: () -> Unit,
    onCreateAccount: () -> Unit,
    onHelp: () -> Unit,
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
    ) {
        // Hasta la primera lectura de la base no se dibuja nada: evita el parpadeo del estado «sin cuentas».
        if (!state.loaded) return@Column

        val helpLabel = stringResource(R.string.help)
        val selected = state.selected

        if (selected == null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconAction(R.drawable.ic_circle_question_mark, helpLabel, onHelp)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ActionRow(R.drawable.ic_plus, stringResource(R.string.home_account_add), Tone.Income, onCreateAccount)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccountPicker(state.accounts, selected, onSelectAccount, onAccounts, Modifier.weight(1f))
                IconAction(R.drawable.ic_circle_question_mark, helpLabel, onHelp)
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
            ) {
                NoticeSlot(notice, onNoticeShown)
                ActionRow(R.drawable.ic_plus, stringResource(R.string.home_income), Tone.Income, onIncome)
                ActionRow(R.drawable.ic_minus, stringResource(R.string.home_expense), Tone.Expense, onExpense)
                ActionRow(R.drawable.ic_list, stringResource(R.string.home_history), Tone.Neutral, onHistory)
            }
        }
    }
}

/**
 * El aviso ocupa una ranura de alto cero sobre la primera acción, así que mostrarlo u ocultarlo no
 * mueve nada (en Penpot es un elemento «absoluto»). La columna deja 12 dp entre la ranura y la
 * primera acción; el desplazamiento de −4 dp completa los 16 dp del diseño.
 */
@Composable
private fun NoticeSlot(notice: SavedNotice?, onNoticeShown: () -> Unit) {
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(NOTICE_MILLIS)
            onNoticeShown()
        }
    }
    Box(Modifier.fillMaxWidth().height(0.dp)) {
        if (notice != null) {
            val message = stringResource(if (notice == SavedNotice.Income) R.string.saved_income else R.string.saved_expense)
            SavedSnackbar(
                message = message,
                modifier = Modifier.wrapContentHeight(align = Alignment.Bottom, unbounded = true).offset(y = (-4).dp),
            )
        }
    }
}

/**
 * Selector de cuenta con su lista desplegable (tablero «selector abierto»): las cuentas con la
 * seleccionada marcada, una línea y «Cuentas» en verde. La lista cuelga 6 dp bajo el selector y es
 * 8 dp más ancha, como en Penpot.
 */
@Composable
private fun AccountPicker(
    accounts: List<Account>,
    selected: Account,
    onSelectAccount: (Long) -> Unit,
    onAccounts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(modifier.onSizeChanged { anchor = it }) {
        AccountSelector(name = selected.name, onClick = { expanded = true })

        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, anchor.height + with(density) { 6.dp.roundToPx() }),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Column(
                    modifier = Modifier
                        .width(with(density) { anchor.width.toDp() } + 8.dp)
                        .clip(MdShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = BandTrackerDimens.Space8),
                ) {
                    accounts.forEach { account ->
                        ListOption(
                            text = account.name,
                            selected = account.id == selected.id,
                            onClick = {
                                expanded = false
                                onSelectAccount(account.id)
                            },
                        )
                    }
                    Box(
                        Modifier
                            .padding(horizontal = BandTrackerDimens.Space8)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BandTrackerTheme.colors.divider),
                    )
                    AccountsEntry(onClick = {
                        expanded = false
                        onAccounts()
                    })
                }
            }
        }
    }
}

/** «Cuentas» al final de la lista: una opción de 48 dp con el texto en verde (Penpot: `color.income`). */
@Composable
private fun AccountsEntry(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = BandTrackerDimens.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = BandTrackerDimens.Space16, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BandText(
            text = stringResource(R.string.home_accounts_menu),
            style = BandTrackerTheme.text.bodyEmphasis,
            color = BandTrackerTheme.colors.income,
        )
    }
}
