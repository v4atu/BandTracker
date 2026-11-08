package com.v4atu.bandtracker.ui.history

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.data.Account
import com.v4atu.bandtracker.data.Entry
import com.v4atu.bandtracker.domain.DateLabels
import com.v4atu.bandtracker.domain.EntryType
import com.v4atu.bandtracker.domain.Money
import com.v4atu.bandtracker.ui.ScreenTestBase
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC §5.3 y DESIGN §7 «5.3 Historial». */
class HistoryScreenTest : ScreenTestBase() {

    private val locale get() = context.resources.configuration.locales[0]
    private val account = Account(1, "u1", "Account 1", "USD")

    private fun entry(
        id: Long, date: LocalDate, type: EntryType = EntryType.EXPENSE, amount: Long = 1_500_000,
        subject: String? = null, note: String? = null,
    ) = Entry(id, "u$id", 1, type, amount, date, subject, note)

    private val nov6 = LocalDate.of(2026, 11, 6)
    private val entries = listOf(
        entry(3, nov6, EntryType.EXPENSE, 1_500_000, "Almuerzo", "Corrientazo"),
        entry(2, nov6, EntryType.INCOME, 12_000_000, "Venta"),
        entry(1, LocalDate.of(2026, 10, 30), EntryType.EXPENSE, 200_000, null, null),
    )

    private class Calls {
        val clicked = mutableListOf<Long>()
        var loadMore = 0
    }

    private var topInsetDp = 0f

    private fun show(
        entries: List<Entry> = this.entries, canLoadMore: Boolean = false, account: Account? = this.account, calls: Calls = Calls(),
    ) {
        showScreen {
            val metrics = LocalDensity.current
            topInsetDp = WindowInsets.safeDrawing.getTop(metrics) / metrics.density
            HistoryScreen(
                state = HistoryUiState(loaded = true, account = account, entries = entries, canLoadMore = canLoadMore),
                onEntryClick = { calls.clicked += it },
                onLoadMore = { calls.loadMore++ },
            )
        }
    }

    private fun signed(entry: Entry) = Money.formatSigned(entry.type, entry.amountMinor, "USD", locale)

    // --- Contenido ---

    @Test
    fun showsTheAccountNameAsTheTitle() {
        show()

        compose.onNodeWithText("Account 1").assertIsDisplayed()
    }

    @Test
    fun showsTheMonthSeparatorsAndTheDayHeaders() {
        show()

        compose.onNodeWithText(DateLabels.month(YearMonth.of(2026, 11), locale)).assertIsDisplayed()
        compose.onNodeWithText(DateLabels.day(nov6, locale)).assertIsDisplayed()
        compose.onNodeWithText(DateLabels.month(YearMonth.of(2026, 10), locale)).assertIsDisplayed()
        compose.onNodeWithText(DateLabels.day(LocalDate.of(2026, 10, 30), locale)).assertIsDisplayed()
    }

    @Test
    fun eachRowShowsSubjectNoteAndTheSignedAmount() {
        show()

        compose.onNodeWithText("Almuerzo").assertIsDisplayed()
        compose.onNodeWithText("Corrientazo").assertIsDisplayed()
        compose.onNodeWithText(signed(entries[0])).assertIsDisplayed()
        compose.onNodeWithText("Venta").assertIsDisplayed()
        compose.onNodeWithText(signed(entries[1])).assertIsDisplayed()
        assertTrue("el egreso lleva «−» y el ingreso «+»", signed(entries[0]).startsWith("−") && signed(entries[1]).startsWith("+"))
    }

    @Test
    fun aRowWithoutSubjectOrNote_showsTheLocalizedFallback() {
        show()

        compose.onNodeWithText(string(R.string.entry_no_subject)).assertIsDisplayed()
    }

    @Test
    fun tappingARow_asksToOpenItsEdition() {
        val calls = Calls()
        show(calls = calls)

        compose.onNodeWithText("Venta").performClick()
        compose.onNodeWithText("Almuerzo").performClick()

        assertEquals(listOf(2L, 3L), calls.clicked)
    }

    @Test
    fun withoutEntries_onlyTheTitleIsShown() {
        show(entries = emptyList())

        compose.onNodeWithText("Account 1").assertIsDisplayed()
    }

    @Test
    fun withoutAnAccount_nothingIsDrawn_andNothingCrashes() {
        show(entries = emptyList(), account = null)

        compose.onNodeWithText("Account 1").assertDoesNotExist()
    }

    // --- Medidas de Penpot (tablero «5.3 Historial») ---

    @Test
    fun theTitleIsAtTheTopLeft_andTheListStarts16dpBelow() {
        show()

        val title = compose.onNodeWithText("Account 1")
        val month = compose.onNodeWithText(DateLabels.month(YearMonth.of(2026, 11), locale))

        // Los nodos de Text sobresalen de su caja de línea (ver BandText): se comparan los centros.
        assertDp("centro del título (38 + barra + 14.5)", 38f + topInsetDp + 14.5f, title.centerYDp())
        assertDp("margen izquierdo", 28f, title.leftDp())
        assertDp("centro del separador de mes (título 29 + 16 + 10)", 38f + topInsetDp + 29f + 16f + 10f, month.centerYDp())
    }

    @Test
    fun itemsAreSeparatedBy8dp() {
        show()

        val month = compose.onNodeWithText(DateLabels.month(YearMonth.of(2026, 11), locale))
        val day = compose.onNodeWithText(DateLabels.day(nov6, locale))
        val firstRow = compose.onNodeWithText("Almuerzo")

        // Penpot: separador 20 dp + 8 + encabezado 15 dp + 8 + fila. Centros: 10 + 8 + 7.5 y 7.5 + 8.
        assertDp("separador de mes → encabezado de día", 10f + 8f + 7.5f, day.centerYDp() - month.centerYDp())
        assertDp("encabezado de día → fila", 7.5f + 8f, firstRow.topDp() - day.centerYDp(), tolerance = 2f)
    }

    // --- Carga por páginas ---

    private val manyEntries = (1..60).map { entry(it.toLong(), nov6.minusDays(it.toLong()), subject = "Registro $it") }.reversed()

    @Test
    fun withMorePagesAvailable_scrollingNearTheEnd_asksForMore() {
        val calls = Calls()
        show(entries = manyEntries, canLoadMore = true, calls = calls)
        compose.waitForIdle()
        assertEquals("al inicio no hace falta pedir más", 0, calls.loadMore)

        compose.onNode(hasScrollAction()).performScrollToIndex(HistoryItems.of(manyEntries).size - 1)
        compose.waitForIdle()

        assertTrue("al llegar al final pide la siguiente página", calls.loadMore >= 1)
    }

    @Test
    fun withoutMorePages_itNeverAsksForMore() {
        val calls = Calls()
        show(entries = manyEntries, canLoadMore = false, calls = calls)

        compose.onNode(hasScrollAction()).performScrollToIndex(HistoryItems.of(manyEntries).size - 1)
        compose.waitForIdle()

        assertEquals(0, calls.loadMore)
    }

    @Test
    fun aShortListThatFitsTheScreen_withMorePages_asksForMoreStraightAway() {
        val calls = Calls()
        show(canLoadMore = true, calls = calls)

        compose.waitForIdle()

        assertTrue(calls.loadMore >= 1)
    }
}
