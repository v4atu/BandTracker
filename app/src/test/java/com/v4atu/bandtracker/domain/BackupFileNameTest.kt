package com.v4atu.bandtracker.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** Nombre sugerido del archivo al exportar (SPEC §7): lleva la fecha para distinguir respaldos. */
class BackupFileNameTest {

    @Test
    fun itCarriesTheIsoDate_andTheJsonExtension() {
        assertEquals("bandtracker-2026-11-06.json", BackupFileName.of(LocalDate.of(2026, 11, 6)))
    }

    @Test
    fun monthAndDayArePadded() {
        assertEquals("bandtracker-2026-01-05.json", BackupFileName.of(LocalDate.of(2026, 1, 5)))
    }
}
