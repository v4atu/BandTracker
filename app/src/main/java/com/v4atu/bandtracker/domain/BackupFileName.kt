package com.v4atu.bandtracker.domain

import java.time.LocalDate

/** Nombre sugerido del archivo de respaldo (SPEC §7): lleva la fecha para distinguir un respaldo de otro. */
object BackupFileName {
    fun of(date: LocalDate): String = "bandtracker-$date.json"
}
