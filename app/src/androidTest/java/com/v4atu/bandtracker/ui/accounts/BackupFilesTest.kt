package com.v4atu.bandtracker.ui.accounts

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Leer y escribir el archivo que el usuario elige con el selector del sistema (SPEC §7). */
class BackupFilesTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver = context.contentResolver
    private lateinit var dir: File

    @Before
    fun setUp() {
        dir = File(context.cacheDir, "backup-files-test").apply { deleteRecursively(); mkdirs() }
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun uri(name: String) = Uri.fromFile(File(dir, name))

    @Test
    fun whatIsWritten_isReadBackIdentically_includingAccentsAndEmoji() = runBlocking {
        val text = """{"version":1,"nombre":"Año Café ñ ✓ 🙂"}"""
        val target = uri("respaldo.json")

        assertTrue(BackupFiles.write(resolver, target, text))

        assertEquals(text, BackupFiles.read(resolver, target))
    }

    @Test
    fun writingOverAnExistingFile_replacesItsWholeContent() = runBlocking {
        val target = uri("respaldo.json")
        BackupFiles.write(resolver, target, "x".repeat(1_000))

        BackupFiles.write(resolver, target, "corto")

        assertEquals("corto", BackupFiles.read(resolver, target))
    }

    @Test
    fun readingAFileThatDoesNotExist_givesNull() = runBlocking {
        assertNull(BackupFiles.read(resolver, uri("no-existe.json")))
    }

    @Test
    fun writingToAPlaceThatCannotBeOpened_givesFalse() = runBlocking {
        assertFalse(BackupFiles.write(resolver, Uri.fromFile(File(dir, "carpeta/que/no/existe/x.json")), "{}"))
    }

    @Test
    fun aFileLargerThanTheLimit_isNotRead() = runBlocking {
        val target = uri("grande.json")
        BackupFiles.write(resolver, target, "a".repeat(100))

        assertNull(BackupFiles.read(resolver, target, maxBytes = 50))
        assertEquals("a".repeat(100), BackupFiles.read(resolver, target, maxBytes = 100))
    }

    @Test
    fun anEmptyFile_isReadAsEmptyText() = runBlocking {
        val target = uri("vacio.json")
        BackupFiles.write(resolver, target, "")

        assertEquals("", BackupFiles.read(resolver, target))
    }
}
