package com.v4atu.bandtracker.domain

/** Qué texto muestra el título de una fila del historial (SPEC §5.3). */
sealed interface RowTitle {
    data class Subject(val text: String) : RowTitle
    data class Note(val text: String) : RowTitle

    /** Sin sujeto ni nota: la UI muestra el texto localizado «Sin sujeto» / «No subject». */
    data object None : RowTitle
}

data class EntryRowText(val title: RowTitle, val subtitle: String?) {
    companion object {
        fun of(subject: String?, note: String?): EntryRowText {
            val s = subject?.trim().orEmpty()
            val n = note?.trim().orEmpty()
            return when {
                s.isNotEmpty() -> EntryRowText(RowTitle.Subject(s), n.ifEmpty { null })
                n.isNotEmpty() -> EntryRowText(RowTitle.Note(n), null)
                else -> EntryRowText(RowTitle.None, null)
            }
        }
    }
}
