package com.v4atu.bandtracker.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cuadrícula del selector de fecha (DESIGN §7 «5.2 Selector de fecha»): siempre 6 semanas de 7 días. */
class CalendarMonthTest {

    private val november2026 = YearMonth.of(2026, 11) // el 1 cae domingo
    private val today = LocalDate.of(2026, 11, 6)

    private fun List<LocalDate?>.days() = map { it?.dayOfMonth }

    // --- weeks ---

    @Test
    fun weeks_alwaysHasSixRowsOfSevenCells() {
        for (month in listOf(YearMonth.of(2026, 2), YearMonth.of(2026, 11), YearMonth.of(2026, 8), YearMonth.of(2024, 2))) {
            DayOfWeek.entries.forEach { first ->
                val weeks = CalendarMonth.weeks(month, first)
                assertEquals("$month desde $first", 6, weeks.size)
                assertTrue("$month desde $first", weeks.all { it.size == 7 })
            }
        }
    }

    @Test
    fun weeks_mondayFirst_putsNovember2026OnTheLastColumnOfTheFirstRow() {
        val weeks = CalendarMonth.weeks(november2026, DayOfWeek.MONDAY)

        assertEquals(listOf(null, null, null, null, null, null, 1), weeks[0].days())
        assertEquals(listOf(2, 3, 4, 5, 6, 7, 8), weeks[1].days())
        assertEquals(listOf(23, 24, 25, 26, 27, 28, 29), weeks[4].days())
        assertEquals(listOf(30, null, null, null, null, null, null), weeks[5].days())
    }

    @Test
    fun weeks_sundayFirst_startsNovember2026OnTheFirstColumn() {
        val weeks = CalendarMonth.weeks(november2026, DayOfWeek.SUNDAY)

        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), weeks[0].days())
        assertEquals(listOf(29, 30, null, null, null, null, null), weeks[4].days())
        assertTrue("la sexta fila queda vacía pero existe", weeks[5].all { it == null })
    }

    @Test
    fun weeks_containEveryDayOfTheMonthExactlyOnce_inOrder_withRealDates() {
        listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 2), YearMonth.of(2024, 2), YearMonth.of(2026, 12)).forEach { month ->
            val flat = CalendarMonth.weeks(month, DayOfWeek.MONDAY).flatten().filterNotNull()

            assertEquals(month.lengthOfMonth(), flat.size)
            assertEquals((1..month.lengthOfMonth()).map { month.atDay(it) }, flat)
        }
    }

    @Test
    fun weeks_aLeapFebruaryHas29Days() {
        val flat = CalendarMonth.weeks(YearMonth.of(2024, 2), DayOfWeek.MONDAY).flatten().filterNotNull()

        assertEquals(29, flat.size)
        assertEquals(LocalDate.of(2024, 2, 29), flat.last())
    }

    @Test
    fun weeks_everyDateSitsInTheColumnOfItsWeekday() {
        val weeks = CalendarMonth.weeks(november2026, DayOfWeek.MONDAY)

        weeks.forEach { row ->
            row.forEachIndexed { column, date ->
                if (date != null) assertEquals(DayOfWeek.of(column + 1), date.dayOfWeek)
            }
        }
        assertNull(weeks[0][0])
    }

    // --- weekdayOrder ---

    @Test
    fun weekdayOrder_startsOnTheGivenDay_andWrapsAround() {
        assertEquals(DayOfWeek.entries.toList(), CalendarMonth.weekdayOrder(DayOfWeek.MONDAY))
        assertEquals(
            listOf(
                DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
            ),
            CalendarMonth.weekdayOrder(DayOfWeek.SUNDAY),
        )
        assertEquals(DayOfWeek.SATURDAY, CalendarMonth.weekdayOrder(DayOfWeek.SATURDAY).first())
    }

    // --- navegación entre meses ---

    @Test
    fun previous_goesBackOneMonth_acrossTheYearBoundary() {
        assertEquals(YearMonth.of(2026, 10), CalendarMonth.previous(november2026))
        assertEquals(YearMonth.of(2025, 12), CalendarMonth.previous(YearMonth.of(2026, 1)))
    }

    @Test
    fun next_goesForwardOneMonth_butNeverPastTheMonthOfToday() {
        assertEquals(YearMonth.of(2026, 10), CalendarMonth.next(YearMonth.of(2026, 9), today))
        assertEquals(november2026, CalendarMonth.next(YearMonth.of(2026, 10), today))
        assertEquals("ya estamos en el mes de hoy", november2026, CalendarMonth.next(november2026, today))
    }

    @Test
    fun next_acrossTheYearBoundary() {
        assertEquals(YearMonth.of(2026, 1), CalendarMonth.next(YearMonth.of(2025, 12), today))
    }

    @Test
    fun canGoNext_isFalseOnlyInTheMonthOfToday() {
        assertFalse(CalendarMonth.canGoNext(november2026, today))
        assertTrue(CalendarMonth.canGoNext(YearMonth.of(2026, 10), today))
        assertTrue(CalendarMonth.canGoNext(YearMonth.of(2020, 1), today))
    }

    @Test
    fun canGoNext_onTheFirstAndLastDayOfTheMonth() {
        assertFalse(CalendarMonth.canGoNext(november2026, LocalDate.of(2026, 11, 1)))
        assertFalse(CalendarMonth.canGoNext(november2026, LocalDate.of(2026, 11, 30)))
        assertTrue(CalendarMonth.canGoNext(YearMonth.of(2026, 10), LocalDate.of(2026, 11, 1)))
    }

    // --- días elegibles ---

    @Test
    fun isSelectable_allowsTodayAndThePast_butNotTheFuture() {
        assertTrue(CalendarMonth.isSelectable(today, today))
        assertTrue(CalendarMonth.isSelectable(today.minusDays(1), today))
        assertTrue(CalendarMonth.isSelectable(LocalDate.of(2020, 2, 29), today))
        assertFalse(CalendarMonth.isSelectable(today.plusDays(1), today))
        assertFalse(CalendarMonth.isSelectable(LocalDate.of(2027, 1, 1), today))
    }

    @Test
    fun isSelectable_agreesWithTheDomainRuleOfEntries() {
        listOf(-400L, -1L, 0L, 1L, 30L).forEach { offset ->
            val date = today.plusDays(offset)
            assertEquals("offset $offset", EntryDates.isAllowed(date, today), CalendarMonth.isSelectable(date, today))
        }
    }
}
