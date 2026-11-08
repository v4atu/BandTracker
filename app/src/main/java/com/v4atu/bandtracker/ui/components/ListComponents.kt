package com.v4atu.bandtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.domain.DateLabels
import com.v4atu.bandtracker.domain.EntryRowText
import com.v4atu.bandtracker.domain.EntryType
import com.v4atu.bandtracker.domain.RowTitle
import com.v4atu.bandtracker.ui.theme.BandTrackerDimens
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

private val MdShape = RoundedCornerShape(BandTrackerDimens.RadiusMd)

@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/**
 * Fila del historial (SPEC §5.3): sujeto como título y nota como subtítulo; sin sujeto, el título es
 * la nota; sin ambos, un texto localizado en gris. El monto lleva su signo ([amount] ya viene con
 * `+` / `−`) y el color de su [type]. Al menos 60 dp de alto.
 */
@Composable
fun EntryRow(
    text: EntryRowText,
    amount: String,
    type: EntryType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (val t = text.title) {
        is RowTitle.Subject -> t.text
        is RowTitle.Note -> t.text
        RowTitle.None -> stringResource(R.string.entry_no_subject)
    }
    val titleColor = if (text.title == RowTitle.None) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val amountColor = if (type == EntryType.INCOME) BandTrackerTheme.colors.income else BandTrackerTheme.colors.expense

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(MdShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = BandTrackerDimens.Space16, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BandText(text = title, style = BandTrackerTheme.text.bodyEmphasis, color = titleColor)
            text.subtitle?.let {
                BandText(text = it, style = BandTrackerTheme.text.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // El monto se mide primero y nunca se recorta: es el dato que importa.
        BandText(text = amount, style = BandTrackerTheme.text.action, color = amountColor, softWrap = false, overflow = TextOverflow.Clip)
    }
}

/** Encabezado plano de día, p. ej. «Viernes 6» (estilo `Nota`, gris). */
@Composable
fun DayHeader(date: LocalDate, modifier: Modifier = Modifier, locale: Locale = currentLocale()) {
    BandText(
        text = DateLabels.day(date, locale),
        style = BandTrackerTheme.text.note,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() },
    )
}

/** Separador cuando cambia el mes o el año, p. ej. «Noviembre 2026». */
@Composable
fun MonthHeader(yearMonth: YearMonth, modifier: Modifier = Modifier, locale: Locale = currentLocale()) {
    BandText(
        text = DateLabels.month(yearMonth, locale),
        style = BandTrackerTheme.text.bodyEmphasis,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { heading() },
    )
}

/** Tarjeta de cuenta (Cuentas): nombre y código de moneda; clicable solo si hay [onClick]. 52 dp (Penpot: 16 + 20 + 16). */
@Composable
fun AccountCard(
    name: String,
    currencyCode: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val click = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(MdShape)
            .background(MaterialTheme.colorScheme.surface)
            .then(click)
            .semantics(mergeDescendants = true) {}
            .padding(BandTrackerDimens.Space16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BandText(
            text = name,
            style = BandTrackerTheme.text.bodyEmphasis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        BandText(text = currencyCode, style = BandTrackerTheme.text.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Opción de una lista (cuentas, monedas): 48 dp; la seleccionada muestra una marca verde. Con
 * [detail] (p. ej. el nombre de la moneda junto a su código) se muestra a continuación en regular y
 * color secundario; toda la fila sigue siendo una sola opción.
 */
@Composable
fun ListOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BandTrackerDimens.MinTouchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = BandTrackerDimens.Space16, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (detail == null) {
            BandText(
                text = text,
                style = BandTrackerTheme.text.bodyEmphasis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        } else {
            BandText(text = text, style = BandTrackerTheme.text.bodyEmphasis, color = MaterialTheme.colorScheme.onSurface)
            BandText(
                text = detail,
                style = BandTrackerTheme.text.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        if (selected) DecorativeIcon(R.drawable.ic_check, BandTrackerTheme.colors.income)
    }
}
