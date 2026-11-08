package com.v4atu.bandtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** SPEC §5.3: título = sujeto; nota de subtítulo; sin sujeto el título es la nota; sin ambos, texto localizado. */
class EntryRowTextTest {

    @Test
    fun subjectAndNote_subjectIsTitleNoteIsSubtitle() {
        val text = EntryRowText.of(subject = "Taxi", note = "Aeropuerto")

        assertEquals(RowTitle.Subject("Taxi"), text.title)
        assertEquals("Aeropuerto", text.subtitle)
    }

    @Test
    fun subjectOnly_hasNoSubtitle() {
        val text = EntryRowText.of(subject = "Taxi", note = null)

        assertEquals(RowTitle.Subject("Taxi"), text.title)
        assertNull(text.subtitle)
    }

    @Test
    fun noteOnly_noteBecomesTitleWithoutSubtitle() {
        val text = EntryRowText.of(subject = null, note = "Aeropuerto")

        assertEquals(RowTitle.Note("Aeropuerto"), text.title)
        assertNull(text.subtitle)
    }

    @Test
    fun neither_titleIsLocalizedFallbackMarker() {
        val text = EntryRowText.of(subject = null, note = null)

        assertEquals(RowTitle.None, text.title)
        assertNull(text.subtitle)
    }

    @Test
    fun blankValuesCountAsAbsent() {
        assertEquals(RowTitle.None, EntryRowText.of(subject = "  ", note = "").title)
        assertEquals(RowTitle.Note("Nota"), EntryRowText.of(subject = " ", note = "Nota").title)
        assertNull(EntryRowText.of(subject = "Taxi", note = "   ").subtitle)
    }

    @Test
    fun valuesAreTrimmed() {
        val text = EntryRowText.of(subject = "  Taxi ", note = " ida ")

        assertEquals(RowTitle.Subject("Taxi"), text.title)
        assertEquals("ida", text.subtitle)
    }
}
