package com.v4atu.bandtracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.v4atu.bandtracker.data.NameResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Cableado real de punta a punta: `BandTrackerApp` → `AppContainer` → Room (archivo real) →
 * `HomeViewModel` → `HomeScreen` dentro de `MainActivity` con Navigation Compose.
 */
class MainActivityTest {

    private val container: AppContainer =
        (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BandTrackerApp).container

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun string(id: Int) = compose.activity.getString(id)

    @Before
    fun startClean() = runBlocking {
        container.accounts.list().forEach { container.accounts.delete(it.id) }
        container.selectedAccountStore.set(null)
    }

    @After
    fun cleanUp() = runBlocking {
        container.accounts.list().forEach { container.accounts.delete(it.id) }
        container.selectedAccountStore.set(null)
    }

    @Test
    fun withAnEmptyDatabase_theAppStartsOnTheNoAccountsHome() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.home_account_add)) > 0 }

        compose.onNodeWithText(string(R.string.home_account_add)).assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.help)).assertIsDisplayed()
    }

    @Test
    fun anAccountCreatedWhileTheAppIsOpen_appearsWithoutRestarting() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.home_account_add)) > 0 }

        runBlocking { container.accounts.create("Efectivo", "COP") }

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount("Efectivo") > 0 }
        compose.onNodeWithText(string(R.string.home_income)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.home_expense)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.home_history)).assertIsDisplayed()
    }

    @Test
    fun theSelectedAccountIsRememberedBetweenRuns() {
        val viaje = runBlocking {
            container.accounts.create("Efectivo", "COP")
            (container.accounts.create("Viaje", "USD") as NameResult.Saved).id
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount("Efectivo") > 0 }

        compose.onNodeWithText("Efectivo").performClick()
        compose.onNodeWithText("Viaje").performClick()

        compose.waitUntil(5_000) { container.selectedAccountStore.id.value == viaje }
        // «Otra ejecución»: un almacén nuevo sobre las mismas preferencias ve la misma cuenta.
        val prefs = compose.activity.getSharedPreferences("bandtracker", android.content.Context.MODE_PRIVATE)
        assertEquals(viaje, com.v4atu.bandtracker.data.SelectedAccountStore(prefs).id.value)
    }

    @Test
    fun tappingIncome_opensTheCaptureScreen() {
        runBlocking { container.accounts.create("Efectivo", "COP") }
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount("Efectivo") > 0 }

        compose.onNodeWithText(string(R.string.home_income)).performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.entry_save_income)) > 0 }
        compose.onNodeWithText(string(R.string.entry_save_income)).assertIsDisplayed()
    }

    @Test
    fun savingAnExpense_returnsHomeWithTheNotice_andStoresTheEntryInTheSelectedAccount() {
        val accountId = runBlocking { (container.accounts.create("Efectivo", "COP") as NameResult.Saved).id }
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount("Efectivo") > 0 }

        compose.onNodeWithText(string(R.string.home_expense)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.entry_save_expense)) > 0 }
        compose.onNode(hasSetTextAction() and hasContentDescription(string(R.string.entry_amount))).performTextInput("5000")
        settleKeyboard()
        compose.onNodeWithText(string(R.string.entry_subject)).performTextInput("Taxi")
        settleKeyboard()
        compose.onNodeWithText(string(R.string.entry_save_expense)).performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.saved_expense)) > 0 }
        compose.onNodeWithText(string(R.string.saved_expense)).assertIsDisplayed()
        val saved = runBlocking { container.entries.page(accountId, 0, 10) }.single()
        assertEquals(500_000L, saved.amountMinor) // COP tiene 2 decimales (ISO 4217): 5000 = 500.000 centavos
        assertEquals(com.v4atu.bandtracker.domain.EntryType.EXPENSE, saved.type)
        assertEquals("Taxi", saved.subject)
    }

    @Test
    fun theDateFieldOpensThePicker_andTheChosenDayIsSaved() {
        val accountId = runBlocking { (container.accounts.create("Efectivo", "USD") as NameResult.Saved).id }
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount("Efectivo") > 0 }

        compose.onNodeWithText(string(R.string.home_income)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.entry_save_income)) > 0 }
        compose.onNode(hasSetTextAction() and hasContentDescription(string(R.string.entry_amount))).performTextInput("10")
        settleKeyboard()
        compose.onNodeWithText(string(R.string.entry_date)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.action_accept)) > 0 }
        compose.onNodeWithContentDescription(string(R.string.date_previous_month)).performClick()
        compose.onNodeWithText("15").performClick()
        compose.onNodeWithText(string(R.string.action_accept)).performClick()

        // Cerrado el selector, el campo ya no dice «Hoy» y se puede guardar.
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.action_accept)) == 0 }
        compose.onNodeWithText(string(R.string.entry_today)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.entry_save_income)).performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.saved_income)) > 0 }
        val saved = runBlocking { container.entries.page(accountId, 0, 10) }.single()
        assertEquals(java.time.LocalDate.now().minusMonths(1).withDayOfMonth(15), saved.date)
    }

    @Test
    fun cancellingThePicker_keepsTheDateAsToday() {
        runBlocking { container.accounts.create("Efectivo", "USD") }
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount("Efectivo") > 0 }
        compose.onNodeWithText(string(R.string.home_income)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.entry_save_income)) > 0 }

        compose.onNodeWithText(string(R.string.entry_date)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.action_cancel)) > 0 }
        compose.onNodeWithContentDescription(string(R.string.date_previous_month)).performClick()
        compose.onNodeWithText("15").performClick()
        compose.onNodeWithText(string(R.string.action_cancel)).performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.action_cancel)) == 0 }
        compose.onNodeWithText(string(R.string.entry_today)).assertIsDisplayed()
    }

    // --- Historial, edición y borrado (SPEC §5.3) ---

    private fun addEntry(accountId: Long, subject: String?, type: com.v4atu.bandtracker.domain.EntryType = com.v4atu.bandtracker.domain.EntryType.EXPENSE): Long =
        runBlocking {
            val draft = com.v4atu.bandtracker.data.EntryDraft(type, 1500, java.time.LocalDate.now(), subject, null)
            (container.entries.add(accountId, draft) as com.v4atu.bandtracker.data.EntryResult.Saved).id
        }

    private fun openHistoryWith(vararg subjects: String): Long {
        val accountId = runBlocking { (container.accounts.create("Efectivo", "USD") as NameResult.Saved).id }
        subjects.forEach { addEntry(accountId, it) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount("Efectivo") > 0 }
        compose.onNodeWithText(string(R.string.home_history)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(subjects.first()) > 0 }
        return accountId
    }

    @Test
    fun history_showsTheEntriesOfTheSelectedAccount_withTheAccountNameAsTitle() {
        openHistoryWith("Taxi", "Almuerzo")

        compose.onNodeWithText("Efectivo").assertIsDisplayed()
        compose.onNodeWithText("Taxi").assertIsDisplayed()
        compose.onNodeWithText("Almuerzo").assertIsDisplayed()
    }

    @Test
    fun tappingAHistoryRow_opensTheEditionWithTheEntryLoaded() {
        openHistoryWith("Taxi")

        compose.onNodeWithText("Taxi").performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.entry_save)) > 0 }
        compose.onNodeWithText(string(R.string.entry_edit_title)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.entry_delete)).assertIsDisplayed()
        compose.onNodeWithText("Taxi").assertIsDisplayed() // el sujeto ya viene cargado en el campo
    }

    @Test
    fun editingAnEntry_andSaving_returnsToTheHistoryWithTheChange() {
        val accountId = openHistoryWith("Taxi")
        compose.onNodeWithText("Taxi").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.entry_save)) > 0 }

        compose.onNodeWithText(string(R.string.entry_subject)).performTextReplacement("Bus")
        settleKeyboard()
        compose.onNodeWithText(string(R.string.entry_save)).performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount("Bus") > 0 && compose.onAllNodesWithTextCount(string(R.string.entry_save)) == 0 }
        compose.onNodeWithText("Bus").assertIsDisplayed()
        assertEquals("Bus", runBlocking { container.entries.page(accountId, 0, 10) }.single().subject)
    }

    @Test
    fun deletingAnEntry_asksForConfirmation_andThenRemovesIt() {
        val accountId = openHistoryWith("Taxi", "Almuerzo")
        compose.onNodeWithText("Taxi").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.entry_delete)) > 0 }

        compose.onNodeWithText(string(R.string.entry_delete)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.entry_delete_title)) > 0 }
        compose.onNodeWithText(string(R.string.entry_delete_message)).assertIsDisplayed()
        compose.onNode(hasText(string(R.string.entry_delete)) and hasAnyAncestor(isDialog())).performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount("Almuerzo") > 0 && compose.onAllNodesWithTextCount(string(R.string.entry_delete)) == 0 }
        compose.onNodeWithText("Taxi").assertDoesNotExist()
        assertEquals(listOf("Almuerzo"), runBlocking { container.entries.page(accountId, 0, 10) }.map { it.subject })
    }

    @Test
    fun cancellingTheDeleteConfirmation_keepsTheEntry() {
        val accountId = openHistoryWith("Taxi")
        compose.onNodeWithText("Taxi").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.entry_delete)) > 0 }
        compose.onNodeWithText(string(R.string.entry_delete)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.entry_delete_title)) > 0 }

        compose.onNode(hasText(string(R.string.action_cancel)) and hasAnyAncestor(isDialog())).performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.entry_delete_title)) == 0 }
        compose.onNodeWithText(string(R.string.entry_save)).assertIsDisplayed()
        assertEquals(1, runBlocking { container.entries.page(accountId, 0, 10) }.size)
    }

    @Test
    fun goingBackFromTheEdition_withoutSaving_leavesTheEntryUntouched() {
        val accountId = openHistoryWith("Taxi")
        compose.onNodeWithText("Taxi").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.entry_save)) > 0 }
        compose.onNodeWithText(string(R.string.entry_subject)).performTextReplacement("Otro")
        settleKeyboard()

        // Con el teclado abierto el primer «atrás» solo lo cierra (comportamiento normal de Android).
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        androidx.test.espresso.Espresso.pressBack()

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.entry_save)) == 0 }
        compose.onNodeWithText("Taxi").assertIsDisplayed()
        assertEquals("Taxi", runBlocking { container.entries.page(accountId, 0, 10) }.single().subject)
    }

    // --- Cuentas (SPEC §5.4) ---

    private fun createAccountThroughTheUi(name: String, currencyQuery: String, currencyCode: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.account_create)) > 0 }
        compose.onNodeWithText(string(R.string.account_name_label)).performTextInput(name)
        settleKeyboard()
        compose.onNodeWithText(string(R.string.account_currency_label)).performTextInput(currencyQuery)
        settleKeyboard()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(currencyCode) > 0 }
        compose.onNodeWithText(currencyCode).performClick()
        compose.onNodeWithText(string(R.string.account_create)).performClick()
    }

    @Test
    fun withoutAccounts_theAddAccountButtonOpensNewAccount() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.home_account_add)) > 0 }

        compose.onNodeWithText(string(R.string.home_account_add)).performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.account_new_title)) > 0 }
        compose.onNodeWithText(string(R.string.account_create)).assertIsDisplayed()
    }

    @Test
    fun creatingTheFirstAccount_returnsHomeWithItSelected_andStoresItsCurrency() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.home_account_add)) > 0 }
        compose.onNodeWithText(string(R.string.home_account_add)).performClick()

        createAccountThroughTheUi("Efectivo", "cop", "COP")

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.home_income)) > 0 }
        compose.onNodeWithText("Efectivo").assertIsDisplayed()
        val saved = runBlocking { container.accounts.list() }.single()
        assertEquals("Efectivo", saved.name)
        assertEquals("COP", saved.currency)
    }

    @Test
    fun aNameThatAlreadyExists_showsTheError_andBlocksCreating() {
        runBlocking { container.accounts.create("Efectivo", "COP") }
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount("Efectivo") > 0 }
        compose.onNodeWithText("Efectivo").performClick()
        compose.onNodeWithText(string(R.string.home_accounts_menu)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.accounts_title)) > 0 }
        compose.onNodeWithText(string(R.string.account_new_title)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.account_create)) > 0 }

        compose.onNodeWithText(string(R.string.account_name_label)).performTextInput("efectivo")
        settleKeyboard()
        compose.onNodeWithText(string(R.string.account_currency_label)).performTextInput("usd")
        settleKeyboard()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount("USD") > 0 }
        compose.onNodeWithText("USD").performClick()

        compose.onNodeWithText(string(R.string.account_name_duplicate)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.account_create)).assertIsNotEnabled()
    }

    @Test
    fun theAccountsOptionOfTheSelector_opensTheAccountsScreen_withTheThreeActions() {
        runBlocking { container.accounts.create("Efectivo", "COP") }
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount("Efectivo") > 0 }

        compose.onNodeWithText("Efectivo").performClick()
        compose.onNodeWithText(string(R.string.home_accounts_menu)).performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.accounts_title)) > 0 }
        compose.onNodeWithText("COP").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.account_new_title)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.accounts_export)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.accounts_import)).assertIsDisplayed()
    }

    @Test
    fun creatingAnAccountFromTheAccountsScreen_returnsToTheList_showingTheNewAccount() {
        runBlocking { container.accounts.create("Efectivo", "COP") }
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount("Efectivo") > 0 }
        compose.onNodeWithText("Efectivo").performClick()
        compose.onNodeWithText(string(R.string.home_accounts_menu)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.accounts_title)) > 0 }
        compose.onNodeWithText(string(R.string.account_new_title)).performClick()

        createAccountThroughTheUi("Viaje", "usd", "USD")

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.accounts_title)) > 0 }
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount("Viaje") > 0 }
        compose.onNodeWithText("Viaje").assertIsDisplayed()
        assertEquals(setOf("Efectivo", "Viaje"), runBlocking { container.accounts.list() }.map { it.name }.toSet())
    }

    // --- Ayuda y licencias (SPEC §5.5) ---

    @Test
    fun theHelpButton_opensHelp_evenWithoutAccounts() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.home_account_add)) > 0 }

        compose.onNodeWithContentDescription(string(R.string.help)).performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.help_version)) > 0 }
        compose.onNodeWithText("v4atu").assertIsDisplayed()
        compose.onNodeWithText("MIT").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.help_repository)).assertIsDisplayed()
    }

    @Test
    fun help_showsTheRealVersionOfTheInstalledApp() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.home_account_add)) > 0 }
        compose.onNodeWithContentDescription(string(R.string.help)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.help_version)) > 0 }

        val installed = compose.activity.packageManager.getPackageInfo(compose.activity.packageName, 0).versionName
        compose.onNodeWithText(installed.orEmpty()).assertIsDisplayed()
    }

    @Test
    fun help_thirdPartyLicensesRow_opensTheLicenseTexts_andBackReturnsToHelp() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.home_account_add)) > 0 }
        compose.onNodeWithContentDescription(string(R.string.help)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.help_licenses)) > 0 }

        compose.onNodeWithText(string(R.string.help_licenses)).performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.licenses_title)) > 0 }
        compose.onNodeWithText("Istok Web").assertIsDisplayed()

        androidx.test.espresso.Espresso.pressBack()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(string(R.string.help_version)) > 0 }
    }

    /**
     * Tras escribir, el teclado se abre con una animación que reacomoda el diseño; un toque inyectado
     * en ese momento puede perderse porque el botón se está moviendo (en el Pixel 9a con Android 17 un
     * «Guardar» tocado así no llegaba a guardar). Una persona toca cuando el teclado ya se asentó:
     * se cierra y se espera a que la interfaz quede quieta.
     */
    private fun settleKeyboard() {
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitForIdle()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextCount(text: String): Int =
        onAllNodesWithText(text).fetchSemanticsNodes().size
}
