package com.v4atu.bandtracker.ui.help

import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Ayuda con la versión real de la app y el enlace al repositorio (SPEC §5.5). */
@Composable
fun HelpRoute(onOpenLicenses: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).versionName.orEmpty()
    }
    var noBrowser by rememberSaveable { mutableStateOf(false) }

    HelpScreen(
        version = version,
        repositoryUrlShown = noBrowser,
        onOpenRepository = { if (!HelpLinks.openRepository { context.startActivity(it) }) noBrowser = true },
        onOpenLicenses = onOpenLicenses,
    )
}

/** Licencias de terceros, leídas de los assets de la app. */
@Composable
fun LicensesRoute() {
    val assets = LocalContext.current.assets
    val licenses = remember { LicenseAssets.load(assets) }
    LicensesScreen(licenses)
}
