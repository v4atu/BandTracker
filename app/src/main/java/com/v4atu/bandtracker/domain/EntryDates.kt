package com.v4atu.bandtracker.domain

import java.time.Clock
import java.time.LocalDate

/** Reglas de fecha de un registro (SPEC §4): solo día, hoy por defecto, nunca posterior a hoy. */
object EntryDates {

    /** «Hoy» en la zona horaria del reloj (la del dispositivo en producción). */
    fun today(clock: Clock): LocalDate = LocalDate.now(clock)

    fun isAllowed(date: LocalDate, today: LocalDate): Boolean = !date.isAfter(today)
}
