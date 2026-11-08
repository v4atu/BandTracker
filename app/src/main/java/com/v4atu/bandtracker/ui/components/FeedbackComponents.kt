package com.v4atu.bandtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.ui.theme.BandTrackerDimens
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme

private val MdShape = RoundedCornerShape(BandTrackerDimens.RadiusMd)

/**
 * Aviso breve tras guardar («Ingreso guardado»). Colores invertidos —fondo `text.primary`, texto y
 * marca `bg`— que en Material equivalen a `inverseSurface` (DESIGN §6). Se anuncia a los lectores
 * de pantalla sin robar el foco. La duración (~2 s) la gobierna quien lo muestra.
 */
@Composable
fun SavedSnackbar(message: String, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.inverseOnSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MdShape)
            .background(MaterialTheme.colorScheme.inverseSurface)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = BandTrackerDimens.Space16, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DecorativeIcon(R.drawable.ic_check, ink)
        BandText(
            text = message,
            style = BandTrackerTheme.text.body,
            color = ink,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Diálogo de confirmación: título (`Acción`), mensaje (secundario) y dos botones de texto,
 * cancelar a la izquierda y confirmar a la derecha. Si [destructive], confirmar va en rojo;
 * si no, en verde. Ancho de pantalla menos 28 dp por lado, relleno de 24 y radio 12. Con [warning]
 * se agrega, bajo el mensaje, una línea en rojo y negrita (p. ej. cuántos registros se perderían).
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    warning: String? = null,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ConfirmDialogContent(title, message, confirmText, dismissText, destructive, onConfirm, onDismiss, warning)
    }
}

/** El contenido del diálogo, separado de su ventana para poder dibujarlo y capturarlo en las galerías. */
@Composable
internal fun ConfirmDialogContent(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    warning: String? = null,
) {
    DialogFrame(title, message, warning) {
        TextButton(text = dismissText, tone = Tone.Neutral, onClick = onDismiss)
        TextButton(text = confirmText, tone = if (destructive) Tone.Expense else Tone.Income, onClick = onConfirm)
    }
}

/**
 * Diálogo informativo de un solo botón (error de importación, resumen de lo importado): mismo marco
 * que [ConfirmDialog], con una única acción en verde que cierra.
 */
@Composable
fun InfoDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        InfoDialogContent(title, message, confirmText, onDismiss)
    }
}

@Composable
internal fun InfoDialogContent(title: String, message: String, confirmText: String, onDismiss: () -> Unit) {
    DialogFrame(title, message, warning = null) {
        TextButton(text = confirmText, tone = Tone.Income, onClick = onDismiss)
    }
}

/** Marco común: título (`Acción`), mensaje, advertencia opcional y la fila de acciones alineada al final. */
@Composable
private fun DialogFrame(title: String, message: String, warning: String?, actions: @Composable RowScope.() -> Unit) {
    DialogShell(title, actions) {
        Text(text = message, style = BandTrackerTheme.text.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (warning != null) {
            Text(text = warning, style = BandTrackerTheme.text.bodyEmphasis, color = MaterialTheme.colorScheme.error)
        }
    }
}

/**
 * La carcasa de todos los diálogos (Penpot: ancho de pantalla menos 28 dp por lado, relleno de 24,
 * radio 12 y 16 dp entre bloques): el título, un [body] a medida y la fila de [actions] al final.
 */
@Composable
internal fun DialogShell(
    title: String,
    actions: @Composable RowScope.() -> Unit,
    body: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BandTrackerDimens.Space28)
            .clip(MdShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(BandTrackerDimens.Space24),
        verticalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space16),
    ) {
        Text(
            text = title,
            style = BandTrackerTheme.text.action,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        body()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space24, Alignment.End),
            content = actions,
        )
    }
}
