package com.v4atu.bandtracker.ui.help

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.ui.ScreenTestBase
import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC §5.5 y DESIGN §7 «5.5 Ayuda»: versión, quién la mantiene, licencia y enlace al repositorio. */
class HelpScreenTest : ScreenTestBase() {

    private class Calls {
        var repository = 0
        var licenses = 0
    }

    private var topInsetDp = 0f
    private var bottomInsetDp = 0f

    private fun show(version: String = "1.0", repositoryUrlShown: Boolean = false, calls: Calls = Calls()) {
        showScreen {
            val metrics = LocalDensity.current
            topInsetDp = WindowInsets.safeDrawing.getTop(metrics) / metrics.density
            bottomInsetDp = WindowInsets.safeDrawing.getBottom(metrics) / metrics.density
            HelpScreen(
                version = version,
                repositoryUrlShown = repositoryUrlShown,
                onOpenRepository = { calls.repository++ },
                onOpenLicenses = { calls.licenses++ },
            )
        }
    }

    private val repository get() = string(R.string.help_repository)

    @Test
    fun showsTheTitle_theVersion_theMaintainer_andTheLicense() {
        show(version = "2.3.1")

        compose.onNodeWithText(string(R.string.help)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.help_version)).assertIsDisplayed()
        compose.onNodeWithText("2.3.1").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.help_maintainer)).assertIsDisplayed()
        compose.onNodeWithText("v4atu").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.help_license)).assertIsDisplayed()
        compose.onNodeWithText("MIT").assertIsDisplayed()
    }

    @Test
    fun theRepositoryButton_isClickable_andAsksToOpenIt() {
        val calls = Calls()
        show(calls = calls)

        compose.onNodeWithText(repository).assertIsDisplayed().assertHasClickAction().performClick()

        assertEquals(1, calls.repository)
    }

    @Test
    fun theUrlIsNotShown_unlessThereIsNoBrowser() {
        show(repositoryUrlShown = false)

        compose.onNodeWithText(HelpLinks.REPOSITORY_URL).assertDoesNotExist()
    }

    @Test
    fun withoutABrowser_theUrlIsShownAsText() {
        show(repositoryUrlShown = true)

        compose.onNodeWithText(HelpLinks.REPOSITORY_URL).assertIsDisplayed()
    }

    @Test
    fun theLicensesRow_opensTheThirdPartyLicenses() {
        val calls = Calls()
        show(calls = calls)

        compose.onNodeWithText(string(R.string.help_licenses)).assertIsDisplayed().performClick()

        assertEquals(1, calls.licenses)
    }

    // --- Medidas de Penpot ---

    @Test
    fun theFieldsAre56dpTall_16dpApart() {
        show()

        val version = compose.onNodeWithText(string(R.string.help_version))
        val maintainer = compose.onNodeWithText(string(R.string.help_maintainer))
        val license = compose.onNodeWithText(string(R.string.help_license))

        assertDp("alto de Versión", 56f, version.heightDp())
        assertDp("hueco Versión→Mantenido por", 16f, maintainer.topDp() - version.bottomDp())
        assertDp("hueco Mantenido por→Licencia", 16f, license.topDp() - maintainer.bottomDp())
    }

    @Test
    fun theRepositoryButton_is48dpTall_centered_andAtTheBottom() {
        show()

        val button = compose.onNodeWithText(repository)
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

        assertDp("alto del botón", 48f, button.heightDp())
        assertDp("centrado", root.width / density / 2, (button.leftDp() + button.rightDp()) / 2)
        assertDp("margen inferior (32 + barra del sistema)", 32f + bottomInsetDp, root.bottom / density - button.bottomDp())
    }

    @Test
    fun theTitleIsCenteredAtTheTop() {
        show()

        val title = compose.onNodeWithText(string(R.string.help))
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

        assertDp("centro vertical del título", 38f + topInsetDp + 22f, title.centerYDp())
        assertDp("centrado", root.width / density / 2, (title.leftDp() + title.rightDp()) / 2)
    }
}
