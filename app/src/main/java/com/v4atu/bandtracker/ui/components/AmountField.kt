package com.v4atu.bandtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.domain.AmountFormat
import com.v4atu.bandtracker.domain.AmountInput
import com.v4atu.bandtracker.domain.Money
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Monto de la captura (Penpot «5.2 Ingreso», «Monto»): el número grande de 48 sp, centrado y del
 * color del tipo de registro ([tone]: verde ingreso, rojo egreso), con el símbolo de la moneda a la
 * izquierda. Se escribe directamente sobre él. Vacío muestra ceros según los decimales de la moneda.
 *
 * El texto vive en [state] y es plano; el filtro [AmountInput] solo deja dígitos y un separador con
 * los decimales de [currencyCode]. Lo que se ve lleva separadores de miles y el decimal de la región
 * ([AmountFormat]), tanto al escribir como al cargar un valor. Todo el ancho es área táctil.
 */
@Composable
fun AmountField(
    state: TextFieldState,
    currencyCode: String,
    tone: Tone,
    modifier: Modifier = Modifier,
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    val style = BandTrackerTheme.text.amount
    val color = tone.color()
    val digits = Money.fractionDigits(currencyCode)
    val symbol = Money.symbol(currencyCode, locale)
    val zero = Money.format(0, currencyCode, locale)
    val filter = remember(digits) {
        InputTransformation {
            if (!AmountInput.accepts(asCharSequence().toString(), digits)) revertAllChanges()
        }
    }
    val label = stringResource(R.string.entry_amount)

    val grouped = remember(locale) { GroupedAmount(locale) }

    BasicTextField(
        state = state,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = label },
        textStyle = style.copy(color = color),
        cursorBrush = SolidColor(color),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(keyboardType = if (digits == 0) KeyboardType.Number else KeyboardType.Decimal),
        inputTransformation = filter,
        outputTransformation = grouped,
        decorator = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BandText(text = symbol, style = style, color = color)
                Spacer(Modifier.width(12.dp))
                // El campo de una línea se expandiría a todo el ancho restante: se ciñe al de su texto
                // (o al del placeholder si está vacío) para que símbolo y número queden centrados juntos.
                Box(Modifier.width(IntrinsicSize.Min)) {
                    if (state.text.isEmpty()) BandText(text = zero, style = style, color = color)
                    LineBox(style.lineHeight) { innerTextField() }
                }
            }
        },
    )
}

/** Muestra el monto agrupado por miles sin tocar el texto guardado: solo inserta o reemplaza separadores. */
private class GroupedAmount(private val locale: Locale) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        val raw = asCharSequence().toString()
        val symbols = DecimalFormatSymbols.getInstance(locale)

        // Primero el decimal (a la derecha) y luego los miles de derecha a izquierda: así cada posición
        // sigue siendo válida aunque ya se hayan insertado separadores más adelante.
        val decimal = AmountFormat.decimalIndex(raw)
        if (decimal >= 0 && raw[decimal] != symbols.decimalSeparator) {
            replace(decimal, decimal + 1, symbols.decimalSeparator.toString())
        }
        AmountFormat.groupSeparatorIndexes(raw).asReversed().forEach { insert(it, symbols.groupingSeparator.toString()) }
    }
}
