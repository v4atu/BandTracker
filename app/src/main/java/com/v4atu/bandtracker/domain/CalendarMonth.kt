package com.v4atu.bandtracker.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Lógica pura de la cuadrícula del selector de fecha (DESIGN §7 «5.2 Selector de fecha»): qué día
 * va en cada celda, el orden de la semana y la navegación entre meses, que no pasa del mes de hoy
 * (SPEC §4: no hay registros con fecha futura).
 */
object CalendarMonth {

    private const val WEEKS = 6
    private const val DAYS_IN_WEEK = 7

    /**
     * Las celdas del mes: siempre [WEEKS] filas de [DAYS_IN_WEEK], aunque la última quede vacía, para
     * que el diálogo no cambie de alto al cambiar de mes (en Penpot el calendario mide siempre 320 dp).
     * Las celdas fuera del mes son `null`. La semana empieza en [firstDayOfWeek].
     */
    fun weeks(month: YearMonth, firstDayOfWeek: DayOfWeek): List<List<LocalDate?>> {
        val leading = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + DAYS_IN_WEEK) % DAYS_IN_WEEK
        val cells = List(WEEKS * DAYS_IN_WEEK) { index ->
            val day = index - leading + 1
            if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
        }
        return cells.chunked(DAYS_IN_WEEK)
    }

    /** Los siete días de la semana empezando en [firstDayOfWeek]: el orden de las columnas. */
    fun weekdayOrder(firstDayOfWeek: DayOfWeek): List<DayOfWeek> =
        List(DAYS_IN_WEEK) { firstDayOfWeek.plus(it.toLong()) }

    fun previous(month: YearMonth): YearMonth = month.minusMonths(1)

    /** El mes siguiente, o el mismo si ya es el mes de [today]: no se navega al futuro. */
    fun next(month: YearMonth, today: LocalDate): YearMonth =
        if (canGoNext(month, today)) month.plusMonths(1) else month

    fun canGoNext(month: YearMonth, today: LocalDate): Boolean = month < YearMonth.from(today)

    /** Un día se puede elegir si no es posterior a hoy (la misma regla que valida el registro). */
    fun isSelectable(date: LocalDate, today: LocalDate): Boolean = EntryDates.isAllowed(date, today)
}
