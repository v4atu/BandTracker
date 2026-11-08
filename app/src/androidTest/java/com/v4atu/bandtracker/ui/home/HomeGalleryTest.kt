package com.v4atu.bandtracker.ui.home

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.v4atu.bandtracker.data.Account
import com.v4atu.bandtracker.ui.ScreenTestBase
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Herramienta de revisión visual (DESIGN §7): guarda capturas de los estados de Inicio en
 * `getExternalFilesDir()/gallery/` para compararlas con los tableros «5.1 Inicio…» de Penpot.
 */
class HomeGalleryTest : ScreenTestBase() {

    private val out = File(context.getExternalFilesDir(null), "gallery").apply { mkdirs() }
    private val accounts = listOf(Account(1, "u1", "Account 1", "COP"), Account(2, "u2", "Viaje", "USD"))

    private fun save(name: String, bitmap: Bitmap) {
        val file = File(out, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 0)
    }

    private fun show(state: HomeUiState, notice: SavedNotice? = null) = showScreen {
        HomeScreen(
            state = state, notice = notice, onNoticeShown = {}, onSelectAccount = {},
            onIncome = {}, onExpense = {}, onHistory = {}, onAccounts = {}, onCreateAccount = {}, onHelp = {},
        )
    }

    @Test
    fun home() {
        compose.mainClock.autoAdvance = false
        show(HomeUiState(true, accounts, accounts[0]))
        compose.mainClock.advanceTimeByFrame()
        save("home", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun homeWithoutAccounts() {
        show(HomeUiState(loaded = true))
        save("home-no-accounts", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun homeWithNotice() {
        compose.mainClock.autoAdvance = false
        show(HomeUiState(true, accounts, accounts[0]), SavedNotice.Income)
        compose.mainClock.advanceTimeByFrame()
        save("home-notice", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun homeSelectorOpen() {
        show(HomeUiState(true, accounts, accounts[0]))
        compose.onNodeWithText("Account 1").performClick()
        compose.waitForIdle()
        // La lista es una ventana aparte (Popup): captureToImage no la incluye, la captura del sistema sí.
        compose.waitForIdle()
        Thread.sleep(400)
        val screenshot = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        save("home-open", screenshot)
    }
}
