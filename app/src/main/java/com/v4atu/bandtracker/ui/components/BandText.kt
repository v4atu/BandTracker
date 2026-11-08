package com.v4atu.bandtracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/**
 * Fija la altura de una **línea de texto suelta** a [lineHeight], centrando el texto dentro de esa
 * caja como hacen CSS y Penpot (el espacio sobrante o faltante se reparte mitad arriba, mitad abajo).
 *
 * Hace falta porque Istok Web mide ~1.44× de alto natural y Compose no baja una línea suelta de esa
 * altura con ningún `LineHeightStyle` (medido en dispositivo), mientras que el DESIGN §3 pide 1.2×.
 * No recorta: el texto que sobresalga por arriba o por abajo se sigue dibujando.
 */
fun Modifier.lineBox(lineHeight: TextUnit): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    val height = lineHeight.roundToPx().coerceIn(constraints.minHeight, constraints.maxHeight)
    val offset = (height - placeable.height) / 2

    val alignmentLines = buildMap<AlignmentLine, Int> {
        val first = placeable[FirstBaseline]
        val last = placeable[LastBaseline]
        if (first != AlignmentLine.Unspecified) put(FirstBaseline, first + offset)
        if (last != AlignmentLine.Unspecified) put(LastBaseline, last + offset)
    }
    layout(placeable.width, height, alignmentLines) { placeable.placeRelative(0, offset) }
}

/**
 * Texto de BandTracker. Si es de una sola línea ([maxLines] = 1, lo habitual) su altura es
 * exactamente `lineHeight` del [style] (1.2× el tamaño, DESIGN §3); con varias líneas, Compose
 * respeta el interlineado entre ellas y solo arriba y abajo queda la altura natural de la fuente.
 */
@Composable
fun BandText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
) {
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
        modifier = if (maxLines == 1) modifier.lineBox(style.lineHeight) else modifier,
    )
}

/** Caja de una línea alrededor de [content] (p. ej. el texto interno de un campo editable). */
@Composable
internal fun LineBox(lineHeight: TextUnit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.lineBox(lineHeight)) { content() }
}
