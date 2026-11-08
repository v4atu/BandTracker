package com.v4atu.bandtracker.ui.accounts

import com.v4atu.bandtracker.backup.BackupError
import com.v4atu.bandtracker.backup.InvalidReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC §7: la importación rechaza el archivo «mostrando un mensaje claro»; cada causa tiene el suyo. */
class ImportMessagesTest {

    @Test
    fun everyInvalidReason_hasItsOwnMessage() {
        val ids = InvalidReason.entries.map { ImportMessages.message(BackupError.InvalidData("accounts[0]", it)) }

        assertTrue("todos tienen texto", ids.all { it != 0 })
        assertEquals("ninguno se repite", ids.size, ids.toSet().size)
    }

    @Test
    fun notValidJson_andUnsupportedVersion_haveTheirOwnMessages() {
        val json = ImportMessages.message(BackupError.NotValidJson)
        val version = ImportMessages.message(BackupError.UnsupportedVersion(found = 9, supported = 1))

        assertNotEquals(0, json)
        assertNotEquals(0, version)
        assertNotEquals(json, version)
        InvalidReason.entries.forEach {
            assertNotEquals(json, ImportMessages.message(BackupError.InvalidData(null, it)))
            assertNotEquals(version, ImportMessages.message(BackupError.InvalidData(null, it)))
        }
    }

    @Test
    fun theLocationIsOnlyGivenForInvalidData() {
        assertEquals("accounts[0].entries[2].date", ImportMessages.location(BackupError.InvalidData("accounts[0].entries[2].date", InvalidReason.BAD_DATE)))
        assertNull(ImportMessages.location(BackupError.InvalidData(null, InvalidReason.BAD_DATE)))
        assertNull(ImportMessages.location(BackupError.NotValidJson))
        assertNull(ImportMessages.location(BackupError.UnsupportedVersion(2, 1)))
    }
}
