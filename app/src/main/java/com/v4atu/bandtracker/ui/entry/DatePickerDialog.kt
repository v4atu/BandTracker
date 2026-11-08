package com.v4atu.bandtracker.ui.entry

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.domain.CalendarMonth
import com.v4atu.bandtracker.domain.DateLabels
import com.v4atu.bandtracker.ui.components.BandText
import com.v4atu.bandtracker.ui.components.TextButton
import com.v4atu.bandtracker.ui.components.Tone
import com.v4atu.bandtracker.ui.theme.BandTrackerDimens
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private val MdShape = RoundedCornerShape(BandTrackerDimens.RadiusMd)
private val CellSize = 48.dp
private val IndicatorSize = 40.dp

/**
 * Selector de fecha (SPEC §5.2, DESIGN §7 «5.2 Selector de fecha»): cuadrícula propia del mes con los
 * días posteriores a [today] deshabilitados. Devuelve el día elegido con [onConfirm]; [onDismiss]
 * cubre «Cancelar» y cerrar el diálogo. La semana empieza según la región.
 */
@Composable
fun DatePickerDialog(
    initial: LocalDate,
    today: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    locale: Locale = LocalConfiguration.current.locales[0],
    firstDayOfWeek: DayOfWeek = WeekFields.of(locale).firstDayOfWeek,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DatePickerContent(initial, today, locale, firstDayOfWeek, onConfirm, onDismiss)
    }
}

/** El contenido del diálogo, separado de su ventana para poder dibujarlo y capturarlo en las galerías. */
@Composable
internal fun DatePickerContent(
    initial: LocalDate,
    today: LocalDate,
    locale: Locale,
    firstDayOfWeek: DayOfWeek,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // Se guardan como números (día y mes absolutos) para sobrevivir a la rotación sin Parcelable.
    var selectedDay by rememberSaveable(initial) { mutableLongStateOf(initial.toEpochDay()) }
    var monthIndex by rememberSaveable(initial) { mutableIntStateOf(initial.year * 12 + initial.monthValue - 1) }
    val selected = LocalDate.ofEpochDay(selectedDay)
    val month = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BandTrackerDimens.Space16)
            .clip(MdShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(BandTrackerDimens.Space16),
        verticalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space16),
    ) {
        BandText(
            text = stringResource(R.string.entry_date),
            style = BandTrackerTheme.text.action,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )

        MonthBar(
            label = DateLabels.month(month, locale),
            canGoNext = CalendarMonth.canGoNext(month, today),
            onPrevious = { monthIndex = CalendarMonth.previous(month).let { it.year * 12 + it.monthValue - 1 } },
            onNext = { monthIndex = CalendarMonth.next(month, today).let { it.year * 12 + it.monthValue - 1 } },
        )

        Calendar(month, selected, today, locale, firstDayOfWeek) { selectedDay = it.toEpochDay() }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space8, Alignment.End),
        ) {
            TextButton(text = stringResource(R.string.action_cancel), tone = Tone.Neutral, onClick = onDismiss)
            TextButton(text = stringResource(R.string.action_accept), tone = Tone.Income, onClick = { onConfirm(selected) })
        }
    }
}

@Composable
private fun MonthBar(label: String, canGoNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MonthButton(R.drawable.ic_chevron_left, stringResource(R.string.date_previous_month), enabled = true, onClick = onPrevious)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            BandText(
                text = label,
                style = BandTrackerTheme.text.bodyEmphasis,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        MonthButton(R.drawable.ic_chevron_right, stringResource(R.string.date_next_month), enabled = canGoNext, onClick = onNext)
    }
}

/** Chevron de 48 × 48 dp. Deshabilitado va en color secundario con opacidad 0.4 (Penpot: «Mes siguiente (deshabilitado)»). */
@Composable
private fun MonthButton(@DrawableRes icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(CellSize)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun Calendar(
    month: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    locale: Locale,
    firstDayOfWeek: DayOfWeek,
    onPick: (LocalDate) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            CalendarMonth.weekdayOrder(firstDayOfWeek).forEach { day ->
                Box(Modifier.size(CellSize, 32.dp), contentAlignment = Alignment.Center) {
                    BandText(
                        text = day.getDisplayName(TextStyle.NARROW, locale),
                        style = BandTrackerTheme.text.note,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        CalendarMonth.weeks(month, firstDayOfWeek).forEach { week ->
            Row {
                week.forEach { date ->
                    if (date == null) {
                        Box(Modifier.size(CellSize))
                    } else {
                        DayCell(date, selected = date == selected, selectable = CalendarMonth.isSelectable(date, today), locale = locale) {
                            onPick(date)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Una celda de 48 dp; el elegido lleva un círculo verde de 40 dp (Penpot «Seleccionado»). El color
 * del número no es la única señal: el elegido tiene círculo, y los futuros no responden al toque y
 * se anuncian como deshabilitados.
 */
@Composable
private fun DayCell(date: LocalDate, selected: Boolean, selectable: Boolean, locale: Locale, onClick: () -> Unit) {
    val description = DateLabels.full(date, locale)
    val textColor: Color = when {
        selected -> BandTrackerTheme.colors.onAccent
        selectable -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(CellSize)
            .clickable(enabled = selectable, role = Role.Button, onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.size(IndicatorSize).clip(CircleShape).background(BandTrackerTheme.colors.income))
        }
        BandText(text = date.dayOfMonth.toString(), style = BandTrackerTheme.text.body, color = textColor)
    }
}
