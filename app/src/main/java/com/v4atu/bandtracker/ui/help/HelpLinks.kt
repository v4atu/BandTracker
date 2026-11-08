package com.v4atu.bandtracker.ui.help

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri

/** Los datos y enlaces de Ayuda (SPEC §5.5, §9). */
object HelpLinks {
    const val REPOSITORY_URL = "https://github.com/v4atu/BandTracker"
    const val MAINTAINER = "v4atu"
    const val LICENSE_NAME = "MIT"

    /**
     * Abre el repositorio en el navegador del usuario con `ACTION_VIEW`. Devuelve `false` si no hay
     * ninguno (`ActivityNotFoundException`): entonces Ayuda muestra la URL como texto (SPEC §5.5).
     * [startActivity] se inyecta para poder probarlo sin lanzar nada.
     */
    fun openRepository(startActivity: (Intent) -> Unit): Boolean = try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL)))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
