package com.v4atu.bandtracker.ui.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.ui.components.BandText
import com.v4atu.bandtracker.ui.components.FormField
import com.v4atu.bandtracker.ui.components.TextButton
import com.v4atu.bandtracker.ui.components.Tone
import com.v4atu.bandtracker.ui.theme.BandTrackerDimens
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme

/**
 * Ayuda (SPEC §5.5, DESIGN §7 «5.5 Ayuda»): versión de la app, quién la mantiene, la licencia y un
 * botón al repositorio. La URL solo se muestra como texto si no hay navegador ([repositoryUrlShown]).
 * La fila de licencias de terceros no está en Penpot: la pidió V4atu para cumplir DESIGN §5.
 */
@Composable
fun HelpScreen(
    version: String,
    repositoryUrlShown: Boolean,
    onOpenRepository: () -> Unit,
    onOpenLicenses: () -> Unit,
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space24),
    ) {
        BandText(
            text = stringResource(R.string.help),
            style = BandTrackerTheme.text.title,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(BandTrackerDimens.Space16),
        ) {
            FormField(label = stringResource(R.string.help_version), value = version)
            FormField(label = stringResource(R.string.help_maintainer), value = HelpLinks.MAINTAINER)
            FormField(label = stringResource(R.string.help_license), value = HelpLinks.LICENSE_NAME)
            FormField(
                label = stringResource(R.string.help_licenses),
                value = "",
                trailingIcon = R.drawable.ic_chevron_right,
                onClick = onOpenLicenses,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (repositoryUrlShown) {
                BandText(
                    text = HelpLinks.REPOSITORY_URL,
                    style = BandTrackerTheme.text.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            TextButton(
                text = stringResource(R.string.help_repository),
                tone = Tone.Neutral,
                icon = R.drawable.ic_external_link,
                onClick = onOpenRepository,
            )
        }
    }
}
