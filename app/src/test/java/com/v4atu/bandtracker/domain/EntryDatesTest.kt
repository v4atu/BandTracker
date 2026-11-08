package com.v4atu.bandtracker.domain

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC §4: date es solo día, por defecto hoy en la zona del dispositivo, nunca posterior a hoy. */
class EntryDatesTest {

    private val today = LocalDate.of(2026, 11, 7)

    @Test
    fun today_isAllowed() = assertTrue(EntryDates.isAllowed(today, today))

    @Test
    fun past_isAllowed() = assertTrue(EntryDates.isAllowed(today.minusDays(1), today))

    @Test
    fun farPast_isAllowed() = assertTrue(EntryDates.isAllowed(LocalDate.of(1999, 1, 1), today))

    @Test
    fun tomorrow_isRejected() = assertFalse(EntryDates.isAllowed(today.plusDays(1), today))

    @Test
    fun farFuture_isRejected() = assertFalse(EntryDates.isAllowed(today.plusYears(5), today))

    @Test
    fun today_usesTheClockZoneNotUtc() {
        // 2026-11-08 02:30 UTC sigue siendo 7 de noviembre en Bogotá (UTC-5).
        val instant = Instant.parse("2026-11-08T02:30:00Z")
        val bogota = Clock.fixed(instant, ZoneId.of("America/Bogota"))
        val utc = Clock.fixed(instant, ZoneId.of("UTC"))

        assertEquals(LocalDate.of(2026, 11, 7), EntryDates.today(bogota))
        assertEquals(LocalDate.of(2026, 11, 8), EntryDates.today(utc))
    }

    @Test
    fun entryAtLocalTodayIsAllowedEvenWhenUtcDayIsAhead() {
        val instant = Instant.parse("2026-11-08T02:30:00Z")
        val clock = Clock.fixed(instant, ZoneId.of("America/Bogota"))

        assertTrue(EntryDates.isAllowed(LocalDate.of(2026, 11, 7), EntryDates.today(clock)))
        assertFalse(EntryDates.isAllowed(LocalDate.of(2026, 11, 8), EntryDates.today(clock)))
    }
}
