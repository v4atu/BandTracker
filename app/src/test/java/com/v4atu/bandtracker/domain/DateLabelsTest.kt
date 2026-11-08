package com.v4atu.bandtracker.domain

import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC §5.3 y DESIGN §6: encabezado de día («Viernes 6») y separador de mes («Noviembre 2026»), según el idioma. */
class DateLabelsTest {

    private val es = Locale.forLanguageTag("es-CO")
    private val en = Locale.US

    @Test
    fun day_isWeekdayAndDayOfMonth_capitalized() {
        val friday = LocalDate.of(2026, 11, 6)

        assertEquals("Viernes 6", DateLabels.day(friday, es))
        assertEquals("Friday 6", DateLabels.day(friday, en))
    }

    @Test
    fun day_doesNotPadTheDayOfMonth() {
        assertEquals("Domingo 1", DateLabels.day(LocalDate.of(2026, 11, 1), es))
        assertEquals("Monday 30", DateLabels.day(LocalDate.of(2026, 11, 30), en))
    }

    @Test
    fun month_isMonthNameAndYear_capitalized() {
        assertEquals("Noviembre 2026", DateLabels.month(YearMonth.of(2026, 11), es))
        assertEquals("November 2026", DateLabels.month(YearMonth.of(2026, 11), en))
    }

    @Test
    fun field_isTheMediumLocalDate_forTheDateFieldOfTheForm() {
        val date = LocalDate.of(2026, 11, 6)

        assertEquals("Nov 6, 2026", DateLabels.field(date, en))
        // El formato exacto en español lo define el sistema; debe llevar día y año y no el orden en inglés.
        val spanish = DateLabels.field(date, es)
        assert("6" in spanish && "2026" in spanish && "Nov 6" !in spanish) { "fecha en español: «$spanish»" }
    }

    @Test
    fun full_isTheCompleteLocalizedDate_forScreenReaders() {
        val date = LocalDate.of(2026, 11, 6)

        assertEquals("Friday, November 6, 2026", DateLabels.full(date, en))
        val spanish = DateLabels.full(date, es).lowercase()
        assert("viernes" in spanish && "noviembre" in spanish && "2026" in spanish) { "fecha en español: «$spanish»" }
    }

    @Test
    fun month_usesTheStandaloneFormInSpanish() {
        assertEquals("Marzo 2025", DateLabels.month(YearMonth.of(2025, 3), es))
        assertEquals("Septiembre 2024", DateLabels.month(YearMonth.of(2024, 9), es))
    }

    @Test
    fun month_andDay_followTheGivenLocaleNotTheDefaultOne() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.JAPAN)
            assertEquals("Viernes 6", DateLabels.day(LocalDate.of(2026, 11, 6), es))
            assertEquals("November 2026", DateLabels.month(YearMonth.of(2026, 11), en))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
