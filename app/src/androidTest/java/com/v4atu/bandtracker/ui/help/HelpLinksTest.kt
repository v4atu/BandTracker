package com.v4atu.bandtracker.ui.help

import android.content.ActivityNotFoundException
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC §5.5: el enlace se abre con `ACTION_VIEW`; sin navegador se captura `ActivityNotFoundException`. */
class HelpLinksTest {

    @Test
    fun theRepositoryUrl_isTheOneInTheSpec() {
        assertEquals("https://github.com/v4atu/BandTracker", HelpLinks.REPOSITORY_URL)
    }

    @Test
    fun openRepository_launchesAnActionViewIntentWithTheUrl() {
        var launched: Intent? = null

        val opened = HelpLinks.openRepository { launched = it }

        assertTrue(opened)
        assertEquals(Intent.ACTION_VIEW, launched?.action)
        assertEquals(HelpLinks.REPOSITORY_URL, launched?.data.toString())
    }

    @Test
    fun withoutABrowser_openRepositoryReportsFailure_insteadOfCrashing() {
        val opened = HelpLinks.openRepository { throw ActivityNotFoundException("no hay navegador") }

        assertFalse(opened)
    }

    @Test
    fun otherErrorsAreNotSwallowed() {
        var thrown: Throwable? = null
        try {
            HelpLinks.openRepository { throw IllegalStateException("otro problema") }
        } catch (e: IllegalStateException) {
            thrown = e
        }

        assertEquals("otro problema", thrown?.message)
    }
}
