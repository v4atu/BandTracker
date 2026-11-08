package com.v4atu.bandtracker.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Etiquetas de fecha del historial según el idioma (SPEC §5.3, §6; DESIGN §6). */
object DateLabels {

    /** «Viernes 6» / «Friday 6»: día de la semana y día del mes, sin cero a la izquierda. */
    fun day(date: LocalDate, locale: Locale): String =
        DateTimeFormatter.ofPattern("EEEE d", locale).format(date).capitalized(locale)

    /** «Noviembre 2026» / «November 2026». `LLLL` es la forma independiente del mes (la que va sola). */
    fun month(yearMonth: YearMonth, locale: Locale): String =
        DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(yearMonth).capitalized(locale)

    /** Fecha del campo «Fecha» cuando no es hoy: formato medio de la región («6 nov 2026» / «Nov 6, 2026»). */
    fun field(date: LocalDate, locale: Locale): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)

    /** Fecha completa («viernes, 6 de noviembre de 2026»): lo que lee el lector de pantalla en cada día. */
    fun full(date: LocalDate, locale: Locale): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(date)

    private fun String.capitalized(locale: Locale) = replaceFirstChar { it.titlecase(locale) }
}
