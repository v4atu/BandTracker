package com.v4atu.bandtracker.ui.history

import com.v4atu.bandtracker.data.Entry
import com.v4atu.bandtracker.domain.EntryType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SPEC §5.3: del más reciente al más antiguo, con encabezados planos por día; cuando cambia el mes
 * o el año se muestra el separador correspondiente (Penpot lo pone también antes del primer día).
 */
class HistoryItemsTest {

    private var nextId = 100L

    private fun entry(date: LocalDate, id: Long = nextId--) = Entry(
        id = id, uuid = "u$id", accountId = 1, type = EntryType.EXPENSE,
        amountMinor = 1000, date = date, subject = null, note = null,
    )

    private fun List<HistoryItem>.kinds() = map {
        when (it) {
            is HistoryItem.Month -> "M${it.yearMonth}"
            is HistoryItem.Day -> "D${it.date.dayOfMonth}"
            is HistoryItem.Row -> "R${it.entry.id}"
        }
    }

    @Test
    fun noEntries_noItems() {
        assertTrue(HistoryItems.of(emptyList()).isEmpty())
    }

    @Test
    fun oneEntry_isMonthThenDayThenRow() {
        val items = HistoryItems.of(listOf(entry(LocalDate.of(2026, 11, 6), id = 1)))

        assertEquals(listOf("M2026-11", "D6", "R1"), items.kinds())
        assertEquals(YearMonth.of(2026, 11), (items[0] as HistoryItem.Month).yearMonth)
        assertEquals(LocalDate.of(2026, 11, 6), (items[1] as HistoryItem.Day).date)
    }

    @Test
    fun sameDay_sharesOneDayHeader() {
        val day = LocalDate.of(2026, 11, 6)

        val items = HistoryItems.of(listOf(entry(day, 3), entry(day, 2), entry(day, 1)))

        assertEquals(listOf("M2026-11", "D6", "R3", "R2", "R1"), items.kinds())
    }

    @Test
    fun differentDaysInTheSameMonth_getADayHeaderEach_butOneMonthSeparator() {
        val items = HistoryItems.of(
            listOf(entry(LocalDate.of(2026, 11, 6), 3), entry(LocalDate.of(2026, 11, 5), 2), entry(LocalDate.of(2026, 11, 5), 1)),
        )

        assertEquals(listOf("M2026-11", "D6", "R3", "D5", "R2", "R1"), items.kinds())
    }

    @Test
    fun whenTheMonthChanges_aNewSeparatorAppears() {
        val items = HistoryItems.of(listOf(entry(LocalDate.of(2026, 11, 1), 2), entry(LocalDate.of(2026, 10, 30), 1)))

        assertEquals(listOf("M2026-11", "D1", "R2", "M2026-10", "D30", "R1"), items.kinds())
    }

    @Test
    fun whenTheYearChanges_aNewSeparatorAppears_evenForTheSameMonthNumber() {
        val items = HistoryItems.of(listOf(entry(LocalDate.of(2026, 11, 6), 2), entry(LocalDate.of(2025, 11, 6), 1)))

        assertEquals(listOf("M2026-11", "D6", "R2", "M2025-11", "D6", "R1"), items.kinds())
    }

    @Test
    fun acrossTheYearBoundary_januaryFollowedByDecember() {
        val items = HistoryItems.of(listOf(entry(LocalDate.of(2026, 1, 2), 2), entry(LocalDate.of(2025, 12, 31), 1)))

        assertEquals(listOf("M2026-01", "D2", "R2", "M2025-12", "D31", "R1"), items.kinds())
    }

    @Test
    fun everyEntryAppearsOnce_inTheOrderGiven() {
        val entries = listOf(
            entry(LocalDate.of(2026, 11, 6), 5), entry(LocalDate.of(2026, 11, 6), 4), entry(LocalDate.of(2026, 10, 2), 3),
            entry(LocalDate.of(2025, 3, 9), 2), entry(LocalDate.of(2025, 3, 9), 1),
        )

        val rows = HistoryItems.of(entries).filterIsInstance<HistoryItem.Row>().map { it.entry.id }

        assertEquals(listOf(5L, 4L, 3L, 2L, 1L), rows)
    }

    @Test
    fun theKeysAreUnique_soTheListCanTrackItems() {
        val items = HistoryItems.of(
            listOf(entry(LocalDate.of(2026, 11, 6), 3), entry(LocalDate.of(2026, 10, 6), 2), entry(LocalDate.of(2025, 11, 6), 1)),
        )

        assertEquals(items.size, items.map { it.key }.toSet().size)
    }
}
