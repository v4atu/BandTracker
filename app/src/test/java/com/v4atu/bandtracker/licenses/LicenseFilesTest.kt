package com.v4atu.bandtracker.licenses

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DESIGN §3 y §5: la OFL de Istok Web exige no modificar la fuente y llevar su texto completo con
 * cada copia; Lucide exige conservar su aviso. Estas pruebas vigilan que lo empaquetado en la app
 * sea idéntico, byte a byte, a lo documentado y a lo descargado (SHA-256 de `istok-web-ORIGEN.md`).
 */
class LicenseFilesTest {

    private val root: File = generateSequence(File(System.getProperty("user.dir")!!).absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() }

    private fun file(path: String) = File(root, path).also { assertTrue("no existe $path", it.exists()) }

    private fun sha256(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    @Test
    fun theLicensesInsideTheApp_areByteForByteTheDocumentedOnes() {
        listOf("istok-web-OFL.txt", "lucide-LICENSE.txt").forEach { name ->
            assertArrayEquals(
                name,
                file("docs/licenses/$name").readBytes(),
                file("app/src/main/assets/licenses/$name").readBytes(),
            )
        }
    }

    @Test
    fun theFontsAreTheUnmodifiedOriginals_perTheRecordedSha256() {
        val origin = file("docs/licenses/istok-web-ORIGEN.md").readText()

        listOf("istok_web_regular.ttf", "istok_web_bold.ttf").forEach { name ->
            val hash = sha256(file("app/src/main/res/font/$name"))
            assertTrue("el SHA-256 de $name ($hash) no coincide con el de istok-web-ORIGEN.md", origin.contains(hash))
        }
    }

    @Test
    fun theOflTextIsTheOneRecorded() {
        val origin = file("docs/licenses/istok-web-ORIGEN.md").readText()

        assertTrue(origin.contains(sha256(file("docs/licenses/istok-web-OFL.txt"))))
        assertTrue(file("app/src/main/assets/licenses/istok-web-OFL.txt").readText().contains("SIL Open Font License, Version 1.1"))
    }

    @Test
    fun theLicenseTexts_carryTheirCopyrightNotices() {
        assertTrue(file("app/src/main/assets/licenses/istok-web-OFL.txt").readText().contains("Andrey V. Panov"))
        assertTrue(file("app/src/main/assets/licenses/lucide-LICENSE.txt").readText().contains("Lucide Icons and Contributors"))
    }

    @Test
    fun theAppsOwnLicense_isMit() {
        val license = file("LICENSE").readText()

        assertTrue(license.contains("MIT License"))
        assertEquals(true, license.contains("Permission is hereby granted"))
    }
}
