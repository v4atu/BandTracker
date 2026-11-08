package com.v4atu.bandtracker.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.v4atu.bandtracker.ui.theme.BandTrackerDimens
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme

/**
 * Campo de solo lectura (Penpot: «Campo» / «Campo con ícono»): etiqueta a la izquierda, valor a la
 * derecha y una línea divisoria de 1 dp. 56 dp de alto. Con [onClick] se comporta como botón (la
 * fecha abre el selector). Si [value] está vacío se muestra [placeholder] en gris.
 */
@Composable
fun FormField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    @DrawableRes trailingIcon: Int? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickable = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    FieldFrame(label, hasIcon = trailingIcon != null, modifier = modifier.then(clickable), mergeSemantics = true) {
        ValueText(value, placeholder, Modifier.weight(1f))
        if (trailingIcon != null) DecorativeIcon(trailingIcon, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Campo editable con el mismo aspecto, basado en `TextFieldState` (la API de texto vigente de
 * Compose). [inputTransformation] filtra lo que se escribe, p. ej. el monto solo admite dígitos.
 * Con [isError] la línea divisoria va en rojo (`color.expense`).
 * La etiqueta se dibuja dentro del decorador del campo, así el lector de pantalla la asocia con él.
 */
@Composable
fun FormField(
    label: String,
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    inputTransformation: InputTransformation? = null,
    isError: Boolean = false,
) {
    BasicTextField(
        state = state,
        modifier = modifier.fillMaxWidth(),
        textStyle = BandTrackerTheme.text.bodyEmphasis.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = keyboardOptions,
        inputTransformation = inputTransformation,
        decorator = { innerTextField ->
            FieldFrame(label, hasIcon = false, modifier = Modifier, mergeSemantics = false, isError = isError) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    if (state.text.isEmpty() && placeholder.isNotEmpty()) {
                        BandText(
                            text = placeholder,
                            style = BandTrackerTheme.text.bodyEmphasis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                        )
                    }
                    LineBox(BandTrackerTheme.text.bodyEmphasis.lineHeight) { innerTextField() }
                }
            }
        },
    )
}

@Composable
private fun ValueText(value: String, placeholder: String, modifier: Modifier) {
    val empty = value.isEmpty()
    BandText(
        text = if (empty) placeholder else value,
        style = BandTrackerTheme.text.bodyEmphasis,
        color = if (empty) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.End,
        modifier = modifier,
    )
}

/** Estructura común: fila etiqueta + valor, y la línea de 1 dp (Penpot: relleno 14/13, o 12/11 con ícono). */
@Composable
private fun FieldFrame(
    label: String,
    hasIcon: Boolean,
    modifier: Modifier,
    mergeSemantics: Boolean = true,
    isError: Boolean = false,
    value: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (mergeSemantics) Modifier.semantics(mergeDescendants = true) {} else Modifier)
            .padding(top = if (hasIcon) 12.dp else 14.dp, bottom = if (hasIcon) 11.dp else 13.dp),
        verticalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space8),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = if (hasIcon) 24.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space8),
        ) {
            BandText(text = label, style = BandTrackerTheme.text.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            value()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(if (isError) MaterialTheme.colorScheme.error else BandTrackerTheme.colors.divider))
    }
}
