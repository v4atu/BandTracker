package com.v4atu.bandtracker.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.domain.EntryType
import com.v4atu.bandtracker.ui.theme.BandTrackerDimens
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme

private val MdShape = RoundedCornerShape(BandTrackerDimens.RadiusMd)

/**
 * Fila de acción (Inicio, Cuentas): ícono tonal de 40 dp, etiqueta y chevron, sobre `surface`.
 * 64 dp de alto, radio 12, relleno 12/12/12/16 y separación de 16 (Penpot, «Fila de acción»).
 */
@Composable
fun ActionRow(
    @DrawableRes icon: Int,
    label: String,
    tone: Tone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(MdShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = BandTrackerDimens.Space16, top = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space16),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            DecorativeIcon(icon, tone.color())
        }
        BandText(
            text = label,
            style = BandTrackerTheme.text.bodyEmphasis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        DecorativeIcon(R.drawable.ic_chevron_right, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Botón de ícono de 48 × 48 dp (el «?» de Ayuda). Es el único ícono que va solo, por eso exige
 * [contentDescription] (DESIGN §5).
 */
@Composable
fun IconAction(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(BandTrackerDimens.MinTouchTarget)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Selector de cuenta de Inicio: nombre y chevron hacia abajo, 48 dp de alto sobre `surface`. */
@Composable
fun AccountSelector(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BandTrackerDimens.MinTouchTarget)
            .clip(MdShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.DropdownList, onClick = onClick)
            .padding(horizontal = BandTrackerDimens.Space16, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space8),
    ) {
        BandText(
            text = name,
            style = BandTrackerTheme.text.bodyEmphasis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        DecorativeIcon(R.drawable.ic_chevron_down, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Botón de confirmación de captura («Ingresar» / «Egresar»). Del color de su [type]; deshabilitado
 * usa `surface` con texto secundario (DESIGN §6). Estilo `Acción` (24 sp), unos 53 dp de alto.
 */
@Composable
fun PrimaryButton(
    text: String,
    type: EntryType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = BandTrackerTheme.colors
    val container = when {
        !enabled -> MaterialTheme.colorScheme.surface
        type == EntryType.INCOME -> colors.income
        else -> colors.expense
    }
    val content = if (enabled) colors.onAccent else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MdShape)
            .background(container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BandText(text = text, style = BandTrackerTheme.text.action, color = content)
    }
}

/**
 * Botón de texto (diálogos, Ayuda). Mínimo 48 × 48 dp; con [icon] opcional de 24 dp y 8 dp de
 * separación. El color sale de [tone]. Deshabilitado ([enabled] = false) no responde al toque y se
 * dibuja en color secundario con opacidad 0.4, para no parecer una acción disponible.
 */
@Composable
fun TextButton(
    text: String,
    tone: Tone = Tone.Neutral,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    val color = if (enabled) tone.color() else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .heightIn(min = BandTrackerDimens.MinTouchTarget)
            .widthIn(min = BandTrackerDimens.MinTouchTarget)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(MdShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space8, Alignment.CenterHorizontally),
    ) {
        if (icon != null) DecorativeIcon(icon, color)
        BandText(text = text, style = BandTrackerTheme.text.bodyEmphasis, color = color)
    }
}
