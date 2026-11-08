package com.v4atu.bandtracker.ui.history

import com.v4atu.bandtracker.data.Entry
import java.time.LocalDate
import java.time.YearMonth

/** Un elemento de la lista del Historial (SPEC §5.3): separador de mes, encabezado de día o registro. */
sealed interface HistoryItem {
    /** Identidad estable para la lista: no se repite entre elementos. */
    val key: String

    data class Month(val yearMonth: YearMonth) : HistoryItem {
        override val key get() = "month-$yearMonth"
    }

    data class Day(val date: LocalDate) : HistoryItem {
        override val key get() = "day-$date"
    }

    data class Row(val entry: Entry) : HistoryItem {
        override val key get() = "entry-${entry.id}"
    }
}

object HistoryItems {

    /**
     * Intercala separadores y encabezados entre los [entries], que llegan del más reciente al más
     * antiguo (`date DESC, id DESC`, SPEC §4). Hay separador de mes antes del primer registro y cada
     * vez que cambia el mes o el año; encabezado de día cada vez que cambia el día.
     */
    fun of(entries: List<Entry>): List<HistoryItem> {
        val items = ArrayList<HistoryItem>(entries.size * 2)
        var month: YearMonth? = null
        var day: LocalDate? = null
        for (entry in entries) {
            val entryMonth = YearMonth.from(entry.date)
            if (entryMonth != month) {
                items += HistoryItem.Month(entryMonth)
                month = entryMonth
                day = null
            }
            if (entry.date != day) {
                items += HistoryItem.Day(entry.date)
                day = entry.date
            }
            items += HistoryItem.Row(entry)
        }
        return items
    }
}
