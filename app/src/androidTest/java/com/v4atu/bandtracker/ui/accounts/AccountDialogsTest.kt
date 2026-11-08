package com.v4atu.bandtracker.ui.accounts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.backup.AccountRename
import com.v4atu.bandtracker.backup.BackupError
import com.v4atu.bandtracker.backup.ImportSummary
import com.v4atu.bandtracker.backup.InvalidReason
import com.v4atu.bandtracker.data.Account
import com.v4atu.bandtracker.domain.AccountNames
import com.v4atu.bandtracker.domain.NameCheck
import com.v4atu.bandtracker.ui.components.ComponentTestBase
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/** DESIGN §7: «5.4 Renombrar cuenta», «5.4 Borrar cuenta» y «5.4 Error de importación» (más el resumen). */
class AccountDialogsTest : ComponentTestBase() {

    private val efectivo = Account(1, "u1", "Efectivo", "COP")
    private val names = listOf("Efectivo", "Viaje")

    // --- Renombrar ---

    private var renamed: String? = null
    private var dismissed = 0

    private fun showRename(current: Account = efectivo) {
        renamed = null
        dismissed = 0
        show {
            RenameAccountDialog(
                account = current,
                check = { AccountNames.check(it, names, currentName = current.name) },
                onConfirm = { renamed = it },
                onDismiss = { dismissed++ },
            )
        }
    }

    @Test
    fun rename_showsTheTitle_theCurrentName_andBothActions() {
        showRename()

        compose.onNodeWithText(string(R.string.account_rename_title)).assertIsDisplayed()
        compose.onNodeWithText("Efectivo").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.action_cancel)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.action_save)).assertIsDisplayed()
    }

    @Test
    fun rename_saveIsEnabled_withTheCurrentName_andReturnsTheTypedName() {
        showRename()

        compose.onNodeWithText(string(R.string.action_save)).assertIsEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("Caja")
        compose.onNodeWithText(string(R.string.action_save)).performClick()

        assertEquals("Caja", renamed)
    }

    @Test
    fun rename_aBlankName_disablesSave_withoutAnErrorMessage() {
        showRename()

        compose.onNode(hasSetTextAction()).performTextReplacement("   ")

        compose.onNodeWithText(string(R.string.action_save)).assertIsNotEnabled()
        compose.onNodeWithText(string(R.string.account_name_duplicate)).assertDoesNotExist()
    }

    @Test
    fun rename_aNameThatExists_showsTheError_andDisablesSave() {
        showRename()

        compose.onNode(hasSetTextAction()).performTextReplacement("viaje")

        compose.onNodeWithText(string(R.string.account_name_duplicate)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.action_save)).assertIsNotEnabled().performClick()
        assertEquals(null, renamed)
    }

    @Test
    fun rename_theAccountsOwnNameInOtherCase_isNotADuplicate() {
        showRename()

        compose.onNode(hasSetTextAction()).performTextReplacement("EFECTIVO")

        compose.onNodeWithText(string(R.string.account_name_duplicate)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.action_save)).assertIsEnabled()
    }

    @Test
    fun rename_cancelDismissesWithoutRenaming() {
        showRename()
        compose.onNode(hasSetTextAction()).performTextReplacement("Caja")

        compose.onNodeWithText(string(R.string.action_cancel)).performClick()

        assertEquals(1, dismissed)
        assertEquals(null, renamed)
    }

    // --- Borrar ---

    private var confirmed = 0

    private fun showDelete(count: Int, locale: Locale? = null) {
        confirmed = 0
        dismissed = 0
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            DeleteAccountDialog(
                request = DeleteRequest(efectivo, count),
                onConfirm = { confirmed++ },
                onDismiss = { dismissed++ },
            )
        }
        if (locale == null) show(content = content) else showLocalized(locale, content)
    }

    @Test
    fun delete_namesTheAccount_explainsTheCascade_andCountsTheEntries() {
        showDelete(128)

        compose.onNodeWithText(context.getString(R.string.account_delete_title, "Efectivo")).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.account_delete_message)).assertIsDisplayed()
        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.account_delete_count, 128, 128)).assertIsDisplayed()
    }

    @Test
    fun delete_theCountUsesTheSingularForOne() {
        showDelete(1, Locale.ENGLISH)

        compose.onNodeWithText("1 entry will be lost").assertIsDisplayed()
    }

    @Test
    fun delete_withNoEntries_stillSaysSo() {
        showDelete(0)

        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.account_delete_count, 0, 0)).assertIsDisplayed()
    }

    @Test
    fun delete_inEnglish_isLocalizedAndPluralized() {
        showDelete(2, Locale.ENGLISH)

        compose.onNodeWithText("Delete “Efectivo”?").assertIsDisplayed()
        compose.onNodeWithText("2 entries will be lost").assertIsDisplayed()
        compose.onNodeWithText("Delete account").assertIsDisplayed()
    }

    @Test
    fun delete_confirmAndCancel_callTheirOwnCallbacks() {
        showDelete(3)

        compose.onNodeWithText(string(R.string.account_delete_confirm)).performClick()
        compose.onNodeWithText(string(R.string.action_cancel)).performClick()

        assertEquals(1, confirmed)
        assertEquals(1, dismissed)
    }

    // --- Resultado de importar ---

    private fun showOutcome(outcome: ImportOutcome, locale: Locale? = null) {
        dismissed = 0
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            ImportOutcomeDialog(outcome = outcome, onDismiss = { dismissed++ })
        }
        if (locale == null) show(content = content) else showLocalized(locale, content)
    }

    @Test
    fun importRejected_saysWhy_andThatNothingChanged() {
        showOutcome(ImportOutcome.Rejected(BackupError.UnsupportedVersion(found = 9, supported = 1)))

        compose.onNodeWithText(string(R.string.import_error_title)).assertIsDisplayed()
        compose.onNodeWithText(
            string(R.string.import_error_version) + " " + string(R.string.import_error_no_changes),
        ).assertIsDisplayed()
    }

    @Test
    fun importRejected_withALocation_showsWhereTheProblemIs() {
        showOutcome(ImportOutcome.Rejected(BackupError.InvalidData("accounts[0].entries[2].date", InvalidReason.FUTURE_DATE)))

        val expected = string(R.string.import_error_future_date) + " " +
            context.getString(R.string.import_error_location, "accounts[0].entries[2].date") + " " +
            string(R.string.import_error_no_changes)
        compose.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun importUnreadable_saysTheFileCouldNotBeRead() {
        showOutcome(ImportOutcome.Unreadable)

        compose.onNodeWithText(string(R.string.import_error_title)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.import_error_unreadable) + " " + string(R.string.import_error_no_changes)).assertIsDisplayed()
    }

    @Test
    fun importSuccess_summarizesWhatWasAdded() {
        showOutcome(ImportOutcome.Success(ImportSummary(2, 0, 40, 0, emptyList())), Locale.ENGLISH)

        compose.onNodeWithText("Import complete").assertIsDisplayed()
        compose.onNodeWithText("Added 2 accounts and 40 entries.").assertIsDisplayed()
    }

    @Test
    fun importSuccess_mentionsWhatAlreadyExisted_onlyWhenSomethingDid() {
        showOutcome(ImportOutcome.Success(ImportSummary(1, 2, 5, 3, emptyList())), Locale.ENGLISH)

        compose.onNodeWithText("Added 1 account and 5 entries.\n2 accounts and 3 entries already existed and were left unchanged.").assertIsDisplayed()
    }

    @Test
    fun importSuccess_listsTheAccountsThatWereRenamed() {
        showOutcome(ImportOutcome.Success(ImportSummary(1, 0, 1, 0, listOf(AccountRename("Viaje", "Viaje (2)")))), Locale.ENGLISH)

        compose.onNodeWithText("Added 1 account and 1 entry.\n“Viaje” was added as “Viaje (2)” because that name already existed.").assertIsDisplayed()
    }

    @Test
    fun importOutcome_acceptDismisses() {
        showOutcome(ImportOutcome.Unreadable)

        compose.onNodeWithText(string(R.string.action_accept)).performClick()

        assertEquals(1, dismissed)
    }
}
