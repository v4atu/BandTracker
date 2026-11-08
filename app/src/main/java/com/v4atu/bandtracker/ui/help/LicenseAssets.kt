package com.v4atu.bandtracker.ui.help

import android.content.res.AssetManager

/** El texto completo de una licencia de terceros, con su nombre para mostrar. */
data class LicenseText(val name: String, val text: String)

/**
 * Las licencias empaquetadas en `assets/licenses/` (DESIGN §3 y §5): la OFL exige el aviso de
 * copyright y el texto completo junto a la fuente, y Lucide (ISC, con partes MIT de Feather) exige
 * conservar su aviso. Se leen del APK, nunca de la red.
 */
object LicenseAssets {

    private val files = listOf(
        "Istok Web" to "licenses/istok-web-OFL.txt",
        "Lucide" to "licenses/lucide-LICENSE.txt",
    )

    fun load(assets: AssetManager): List<LicenseText> = files.map { (name, path) ->
        LicenseText(name, assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }.trim())
    }
}
