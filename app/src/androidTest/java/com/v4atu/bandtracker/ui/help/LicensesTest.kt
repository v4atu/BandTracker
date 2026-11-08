package com.v4atu.bandtracker.ui.help

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.ui.ScreenTestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DESIGN §3 y §5: la licencia OFL de Istok Web y la de Lucide viajan dentro de la app y se pueden
 * leer en Ayuda (los avisos de copyright son un requisito de ambas licencias).
 */
class LicensesTest : ScreenTestBase() {

    private val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets

    @Test
    fun theBundledLicenses_areLoaded_inOrder_withTheirFullText() {
        val licenses = LicenseAssets.load(assets)

        assertEquals(listOf("Istok Web", "Lucide"), licenses.map { it.name })
        assertTrue(licenses[0].text.contains("SIL Open Font License, Version 1.1"))
        assertTrue("el aviso de copyright de la fuente", licenses[0].text.contains("Andrey V. Panov"))
        assertTrue(licenses[1].text.contains("ISC License"))
        assertTrue("el aviso de copyright de Lucide", licenses[1].text.contains("Lucide Icons and Contributors"))
    }

    @Test
    fun theBundledTexts_areTheFullFiles_notExcerpts() {
        val licenses = LicenseAssets.load(assets)

        listOf("licenses/istok-web-OFL.txt", "licenses/lucide-LICENSE.txt").zip(licenses).forEach { (path, license) ->
            val file = assets.open(path).bufferedReader().use { it.readText() }
            assertEquals(path, file.trim(), license.text.trim())
        }
    }

    @Test
    fun theScreen_showsEachLicenseWithItsName() {
        showScreen {
            LicensesScreen(
                licenses = listOf(LicenseText("Istok Web", "Texto de la OFL"), LicenseText("Lucide", "Texto de ISC")),
            )
        }

        compose.onNodeWithText(string(R.string.licenses_title)).assertIsDisplayed()
        compose.onNodeWithText("Istok Web").assertIsDisplayed()
        compose.onNodeWithText("Texto de la OFL").assertIsDisplayed()
    }

    @Test
    fun aLongLicense_canBeScrolledToTheEnd() {
        val long = (1..200).joinToString("\n") { "línea $it de la licencia" }
        showScreen {
            LicensesScreen(licenses = listOf(LicenseText("Larga", long), LicenseText("Otra", "Final")))
        }

        compose.onNode(hasScrollAction()).performScrollToNode(androidx.compose.ui.test.hasText("Final"))

        compose.onNodeWithText("Final").assertIsDisplayed()
    }
}
