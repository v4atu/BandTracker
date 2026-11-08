package com.v4atu.bandtracker.ui.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.ui.components.BandText
import com.v4atu.bandtracker.ui.theme.BandTrackerDimens
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme

/**
 * Licencias de terceros (DESIGN §3 y §5): el texto completo de la OFL de Istok Web y de la licencia
 * de Lucide, con sus avisos de copyright, leído de lo que viaja dentro de la app.
 */
@Composable
fun LicensesScreen(licenses: List<LicenseText>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = BandTrackerDimens.ScreenHorizontal, top = BandTrackerDimens.ScreenTop, end = BandTrackerDimens.ScreenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space24),
    ) {
        BandText(
            text = stringResource(R.string.licenses_title),
            style = BandTrackerTheme.text.title,
            color = MaterialTheme.colorScheme.onBackground,
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space24),
            contentPadding = PaddingValues(bottom = BandTrackerDimens.ScreenBottom),
        ) {
            items(licenses, key = { it.name }) { license ->
                Column(verticalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space8)) {
                    BandText(text = license.name, style = BandTrackerTheme.text.action, color = MaterialTheme.colorScheme.onBackground)
                    BandText(
                        text = license.text,
                        style = BandTrackerTheme.text.note,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}
