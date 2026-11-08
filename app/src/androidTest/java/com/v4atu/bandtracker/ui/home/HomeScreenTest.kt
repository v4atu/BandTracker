package com.v4atu.bandtracker.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.data.Account
import com.v4atu.bandtracker.ui.ScreenTestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC §5.1 y DESIGN §7 (tableros «5.1 Inicio», «sin cuentas», «selector abierto» y «aviso de guardado»). */
class HomeScreenTest : ScreenTestBase() {

    private val efectivo = Account(1, "u1", "Efectivo", "COP")
    private val viaje = Account(2, "u2", "Viaje", "USD")
    private val withAccounts = HomeUiState(loaded = true, accounts = listOf(efectivo, viaje), selected = efectivo)
    private val withoutAccounts = HomeUiState(loaded = true)

    private class Calls {
        var income = 0; var expense = 0; var history = 0; var accounts = 0; var create = 0; var help = 0
        var noticeShown = 0
        val selected = mutableListOf<Long>()
    }

    private fun show(state: HomeUiState, notice: SavedNotice? = null, calls: Calls = Calls(), dark: Boolean = true) {
        showScreen(dark) {
            HomeScreen(
                state = state, notice = notice, onNoticeShown = { calls.noticeShown++ },
                onSelectAccount = { calls.selected += it },
                onIncome = { calls.income++ }, onExpense = { calls.expense++ }, onHistory = { calls.history++ },
                onAccounts = { calls.accounts++ }, onCreateAccount = { calls.create++ }, onHelp = { calls.help++ },
            )
        }
    }

    private val incomeLabel get() = string(R.string.home_income)
    private val expenseLabel get() = string(R.string.home_expense)
    private val historyLabel get() = string(R.string.home_history)
    private val addAccountLabel get() = string(R.string.home_account_add)
    private val helpLabel get() = string(R.string.help)

    // --- Estado normal: con cuentas ---

    @Test
    fun withAccounts_showsTheSelectedAccountHelpAndTheThreeActions() {
        show(withAccounts)

        compose.onNodeWithText("Efectivo").assertIsDisplayed()
        compose.onNodeWithContentDescription(helpLabel).assertIsDisplayed()
        compose.onNodeWithText(incomeLabel).assertIsDisplayed()
        compose.onNodeWithText(expenseLabel).assertIsDisplayed()
        compose.onNodeWithText(historyLabel).assertIsDisplayed()
        compose.onNodeWithText(addAccountLabel).assertDoesNotExist()
    }

    @Test
    fun actionsAreStackedAtTheBottomInOrder_with12dpBetweenThem() {
        show(withAccounts)
        val income = compose.onNodeWithText(incomeLabel)
        val expense = compose.onNodeWithText(expenseLabel)
        val history = compose.onNodeWithText(historyLabel)

        assertTrue(income.topDp() < expense.topDp() && expense.topDp() < history.topDp())
        assertDp("hueco ingreso→egreso", 12f, expense.topDp() - income.bottomDp())
        assertDp("hueco egreso→historial", 12f, history.topDp() - expense.bottomDp())
        income.assertHeightIsEqualTo(64.dp)
        // «Zona del pulgar» (DESIGN §1): las acciones ocupan el tercio inferior de la pantalla.
        val rootHeight = compose.onRoot().fetchSemanticsNode().size.height / density
        assertTrue("las acciones deben estar en la mitad inferior", income.topDp() > rootHeight / 2)
    }

    @Test
    fun topBar_hasTheSelectorAndAHelpButton_of48dpEach_12dpApart() {
        show(withAccounts)
        val selector = compose.onNodeWithText("Efectivo")
        val help = compose.onNodeWithContentDescription(helpLabel)

        assertDp("alto del selector", 48f, selector.heightDp())
        assertDp("alto de ayuda", 48f, help.heightDp())
        assertDp("hueco entre ambos", 12f, help.leftDp() - selector.rightDp())
        assertDp("margen derecho", 28f, compose.onRoot().fetchSemanticsNode().size.width / density - help.rightDp())
        assertDp("margen izquierdo", 28f, selector.leftDp())
        assertDp("alineados arriba", selector.topDp(), help.topDp(), tolerance = 0.5f)
    }

    @Test
    fun actionClicks_invokeTheirOwnCallbacks() {
        val calls = Calls()
        show(withAccounts, calls = calls)

        compose.onNodeWithText(incomeLabel).performClick()
        compose.onNodeWithText(expenseLabel).performClick()
        compose.onNodeWithText(historyLabel).performClick()
        compose.onNodeWithContentDescription(helpLabel).performClick()

        assertEquals(listOf(1, 1, 1, 1), listOf(calls.income, calls.expense, calls.history, calls.help))
    }

    // --- Sin cuentas ---

    @Test
    fun withoutAccounts_theOnlyElementIsTheAccountButton_plusHelp() {
        show(withoutAccounts)

        compose.onNodeWithText(addAccountLabel).assertIsDisplayed()
        compose.onNodeWithContentDescription(helpLabel).assertIsDisplayed()
        compose.onNodeWithText(incomeLabel).assertDoesNotExist()
        compose.onNodeWithText(expenseLabel).assertDoesNotExist()
        compose.onNodeWithText(historyLabel).assertDoesNotExist()
    }

    @Test
    fun withoutAccounts_theAccountButtonIsCenteredInTheScreen() {
        show(withoutAccounts)
        val rootHeight = compose.onRoot().fetchSemanticsNode().size.height / density

        val centerRatio = compose.onNodeWithText(addAccountLabel).centerYDp() / rootHeight
        // Penpot: la fila queda en y=489 de 923 (≈0.53) bajo la barra superior.
        assertTrue("centrada, no pegada abajo (ratio $centerRatio)", centerRatio in 0.4f..0.62f)
    }

    @Test
    fun withoutAccounts_theAccountButtonClicks_andHelpStillWorks() {
        val calls = Calls()
        show(withoutAccounts, calls = calls)

        compose.onNodeWithText(addAccountLabel).performClick()
        compose.onNodeWithContentDescription(helpLabel).performClick()

        assertEquals(1 to 1, calls.create to calls.help)
    }

    // --- Antes de cargar ---

    @Test
    fun beforeTheDatabaseAnswers_nothingIsShown_soThereIsNoFlashOfTheEmptyState() {
        show(HomeUiState(loaded = false))

        compose.onNodeWithText(addAccountLabel).assertDoesNotExist()
        compose.onNodeWithText(incomeLabel).assertDoesNotExist()
        compose.onNodeWithText("Efectivo").assertDoesNotExist()
    }

    // --- Selector abierto ---

    @Test
    fun tappingTheSelector_listsEveryAccountMarkingTheSelectedOne_andTheAccountsEntry() {
        show(withAccounts)

        compose.onNodeWithText("Efectivo").performClick()

        // «Efectivo» aparece dos veces: en el selector y en la lista (seleccionada).
        compose.onAllNodesWithText("Efectivo").assertCountEquals(2)
        compose.onNodeWithText("Viaje").assertIsDisplayed().assertIsNotSelected().assertHeightIsEqualTo(48.dp)
        compose.onNodeWithText(string(R.string.home_accounts_menu)).assertIsDisplayed().assertHeightIsEqualTo(48.dp)
        compose.onNode(hasText("Efectivo") and isSelectable()).assertIsSelected()
    }

    @Test
    fun choosingAnAccount_reportsItsId_andClosesTheList() {
        val calls = Calls()
        show(withAccounts, calls = calls)
        compose.onNodeWithText("Efectivo").performClick()

        compose.onNodeWithText("Viaje").performClick()

        assertEquals(listOf(2L), calls.selected)
        compose.onNodeWithText("Viaje").assertDoesNotExist()
    }

    @Test
    fun theAccountsEntry_opensAccounts_andClosesTheList() {
        val calls = Calls()
        show(withAccounts, calls = calls)
        compose.onNodeWithText("Efectivo").performClick()

        compose.onNodeWithText(string(R.string.home_accounts_menu)).performClick()

        assertEquals(1, calls.accounts)
        compose.onNodeWithText("Viaje").assertDoesNotExist()
    }

    @Test
    fun pressingBack_closesTheListWithoutChangingAnything() {
        val calls = Calls()
        show(withAccounts, calls = calls)
        compose.onNodeWithText("Efectivo").performClick()
        compose.onNodeWithText("Viaje").assertIsDisplayed()

        Espresso.pressBack()
        compose.waitForIdle()

        compose.onNodeWithText("Viaje").assertDoesNotExist()
        assertTrue(calls.selected.isEmpty())
    }

    // --- Aviso de guardado (~2 s) ---

    @Test
    fun savedNotice_showsTheMessageOfEachKind() {
        show(withAccounts, notice = SavedNotice.Income)
        compose.onNodeWithText(string(R.string.saved_income)).assertIsDisplayed()
    }

    @Test
    fun savedNotice_forAnExpense() {
        show(withAccounts, notice = SavedNotice.Expense)
        compose.onNodeWithText(string(R.string.saved_expense)).assertIsDisplayed()
    }

    @Test
    fun savedNotice_sits16dpAboveTheFirstAction() {
        show(withAccounts, notice = SavedNotice.Income)

        val gap = compose.onNodeWithText(incomeLabel).topDp() - compose.onNodeWithText(string(R.string.saved_income)).bottomDp()

        assertDp("separación aviso → primera acción", 16f, gap)
    }

    @Test
    fun savedNotice_disappearsAfterAboutTwoSeconds_byReportingItWasShown() {
        val calls = Calls()
        compose.mainClock.autoAdvance = false
        show(withAccounts, notice = SavedNotice.Income, calls = calls)

        compose.mainClock.advanceTimeBy(1_800)
        assertEquals("aún no debe retirarse a los 1.8 s", 0, calls.noticeShown)

        compose.mainClock.advanceTimeBy(400)
        assertEquals("debe retirarse a los ~2 s", 1, calls.noticeShown)
    }

    @Test
    fun savedNotice_doesNotMoveTheActions() {
        var notice by mutableStateOf<SavedNotice?>(SavedNotice.Income)
        showScreen {
            HomeScreen(
                state = withAccounts, notice = notice, onNoticeShown = {}, onSelectAccount = {},
                onIncome = {}, onExpense = {}, onHistory = {}, onAccounts = {}, onCreateAccount = {}, onHelp = {},
            )
        }
        val withNotice = compose.onNodeWithText(incomeLabel).topDp()

        notice = null
        compose.waitForIdle()

        assertDp("la primera acción no se mueve", withNotice, compose.onNodeWithText(incomeLabel).topDp(), tolerance = 0.5f)
    }
}
