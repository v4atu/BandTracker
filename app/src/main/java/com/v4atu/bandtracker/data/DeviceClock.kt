package com.v4atu.bandtracker.data

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Reloj que lee la zona horaria del dispositivo **cada vez** (SPEC §4: «hoy en la zona horaria del
 * dispositivo»). `Clock.systemDefaultZone()` fija la zona al crearse: si el usuario viaja con la app
 * abierta, «hoy» quedaría desfasado.
 */
object DeviceClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()
}
