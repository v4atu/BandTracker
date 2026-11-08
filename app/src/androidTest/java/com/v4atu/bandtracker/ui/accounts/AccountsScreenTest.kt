package com.v4atu.bandtracker.ui.accounts

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.data.Account
import com.v4atu.bandtracker.ui.ScreenTestBase
import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC §5.4 y DESIGN §7 «5.4 Cuentas»: la lista arriba y, en la zona del pulgar, Nueva cuenta, Exportar e Importar. */
class AccountsScreenTest : ScreenTestBase() {

    private val accounts = listOf(Account(1, "u1", "Account 1", "COP"), Account(2, "u2", "Viaje", "USD"))

    private class Calls {
        var create = 0
        var export = 0
        var import = 0
    }

    private var topInsetDp = 0f
    private var bottomInsetDp = 0f

    private fun show(list: List<Account> = accounts, calls: Calls = Calls()) {
        showScreen {
            val metrics = LocalDensity.current
            topInsetDp = WindowInsets.safeDrawing.getTop(metrics) / metrics.density
            bottomInsetDp = WindowInsets.safeDrawing.getBottom(metrics) / metrics.density
            AccountsScreen(
                state = AccountsUiState(loaded = true, accounts = list),
                onCreate = { calls.create++ }, onExport = { calls.export++ }, onImport = { calls.import++ },
            )
        }
    }

    private val create get() = string(R.string.account_new_title)
    private val export get() = string(R.string.accounts_export)
    private val import get() = string(R.string.accounts_import)

    @Test
    fun showsTheTitle_theAccountsWithTheirCurrency_andTheThreeActions() {
        show()

        compose.onNodeWithText(string(R.string.accounts_title)).assertIsDisplayed()
        compose.onNodeWithText("Account 1").assertIsDisplayed()
        compose.onNodeWithText("COP").assertIsDisplayed()
        compose.onNodeWithText("Viaje").assertIsDisplayed()
        compose.onNodeWithText("USD").assertIsDisplayed()
        compose.onNodeWithText(create).assertIsDisplayed()
        compose.onNodeWithText(export).assertIsDisplayed()
        compose.onNodeWithText(import).assertIsDisplayed()
    }

    @Test
    fun theActions_callTheirOwnCallbacks() {
        val calls = Calls()
        show(calls = calls)

        compose.onNodeWithText(create).performClick()
        compose.onNodeWithText(export).performClick()
        compose.onNodeWithText(import).performClick()
        compose.onNodeWithText(import).performClick()

        assertEquals(1, calls.create)
        assertEquals(1, calls.export)
        assertEquals(2, calls.import)
    }

    @Test
    fun withoutAccounts_theActionsAreStillThere() {
        show(list = emptyList())

        compose.onNodeWithText(create).assertIsDisplayed()
        compose.onNodeWithText(export).assertIsDisplayed()
        compose.onNodeWithText(import).assertIsDisplayed()
    }

    // --- Medidas de Penpot ---

    @Test
    fun theTitleIsCenteredAtTheTop() {
        show()

        val title = compose.onNodeWithText(string(R.string.accounts_title))
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

        assertDp("centro vertical del título", 38f + topInsetDp + 22f, title.centerYDp())
        assertDp("centrado", root.width / density / 2, (title.leftDp() + title.rightDp()) / 2)
    }

    @Test
    fun accountCardsAre52dpTall_8dpApart_startingBelowTheTitle() {
        show()

        val first = compose.onNodeWithText("Account 1")
        val second = compose.onNodeWithText("Viaje")
        val title = compose.onNodeWithText(string(R.string.accounts_title))

        // Penpot: título (44) + 24 de hueco, tarjetas de 52 dp con 8 dp entre ellas. Los textos se miden por su centro.
        assertDp("paso entre tarjetas (52 + 8)", 60f, second.centerYDp() - first.centerYDp())
        assertDp("título → primera tarjeta", 22f + 24f + 26f, first.centerYDp() - title.centerYDp(), tolerance = 2f)
    }

    @Test
    fun theThreeActions_are64dpTall_12dpApart_andSitAtTheBottom() {
        show()

        val newAccount = compose.onNodeWithText(create)
        val exportRow = compose.onNodeWithText(export)
        val importRow = compose.onNodeWithText(import)
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

        assertDp("centro Nueva cuenta → Exportar (64 + 12)", 76f, exportRow.centerYDp() - newAccount.centerYDp())
        assertDp("centro Exportar → Importar (64 + 12)", 76f, importRow.centerYDp() - exportRow.centerYDp())
        assertDp("margen inferior (32 + barra del sistema + media fila)", 32f + bottomInsetDp + 32f, root.bottom / density - importRow.centerYDp(), tolerance = 2f)
    }
}
