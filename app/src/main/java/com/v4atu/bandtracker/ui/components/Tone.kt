package com.v4atu.bandtracker.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme

/**
 * Tono de una acción (variante «Tono» de Penpot). Verde = ingreso, rojo = egreso, el resto es
 * neutro (DESIGN §1). El color nunca es la única señal: siempre hay etiqueta e ícono.
 */
enum class Tone { Income, Expense, Neutral }

@Composable
internal fun Tone.color(): Color = when (this) {
    Tone.Income -> BandTrackerTheme.colors.income
    Tone.Expense -> BandTrackerTheme.colors.expense
    Tone.Neutral -> MaterialTheme.colorScheme.onBackground // color.text.primary
}

/** Ícono decorativo de 24 dp: la etiqueta de al lado ya lo describe (DESIGN §5). */
@Composable
internal fun DecorativeIcon(@DrawableRes icon: Int, tint: Color, modifier: Modifier = Modifier) {
    Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = modifier.size(24.dp))
}
