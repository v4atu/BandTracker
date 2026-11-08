package com.v4atu.bandtracker.data

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** SPEC §4: «hoy» es el de la zona horaria del dispositivo, aunque cambie con la app abierta (p. ej. al viajar). */
class DeviceClockTest {

    private lateinit var original: TimeZone

    @Before
    fun remember() {
        original = TimeZone.getDefault()
    }

    @After
    fun restore() {
        TimeZone.setDefault(original)
    }

    private fun zone(id: String) = TimeZone.setDefault(TimeZone.getTimeZone(id))

    @Test
    fun theClockReadsTheCurrentDefaultZoneEveryTime() {
        zone("Pacific/Kiritimati") // UTC+14
        assertEquals(ZoneId.of("Pacific/Kiritimati"), DeviceClock.zone)

        zone("Pacific/Pago_Pago") // UTC-11
        assertEquals(ZoneId.of("Pacific/Pago_Pago"), DeviceClock.zone)
    }

    @Test
    fun todayFollowsTheZoneChange_whereASnapshotClockDoesNot() {
        zone("Pacific/Kiritimati")
        val snapshot = Clock.systemDefaultZone() // fija la zona al crearse: lo que NO queremos

        zone("Pacific/Pago_Pago") // 25 horas atrás: el día local es distinto
        val live = LocalDate.now(DeviceClock)
        val frozen = LocalDate.now(snapshot)

        assertNotEquals("el reloj congelado se queda en la zona vieja", live, frozen)
        assertTrue("Kiritimati (+14) va por delante de Pago Pago (−11)", frozen.isAfter(live))
    }

    @Test
    fun itTicksWithRealTime() {
        val before = System.currentTimeMillis()
        val millis = DeviceClock.millis()
        val after = System.currentTimeMillis()

        assertTrue(millis in before..after)
    }

    @Test
    fun withZone_returnsAClockPinnedToThatZone() {
        val pinned = DeviceClock.withZone(ZoneId.of("Asia/Tokyo"))

        assertEquals(ZoneId.of("Asia/Tokyo"), pinned.zone)
    }
}
