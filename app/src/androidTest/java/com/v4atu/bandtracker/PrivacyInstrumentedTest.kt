package com.v4atu.bandtracker

import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SPEC §8 (privacidad) y criterio de aceptación 1, comprobado sobre el paquete **instalado** en el
 * dispositivo: la app no pide ningún permiso, ni declara los suyos (tampoco los internos que AndroidX fusiona).
 *
 * Ojo: a partir de Android 17, para las apps que apuntan al SDK 37, el sistema agrega solo el permiso
 * implícito `android.permission.OTHER_SENSORS` al analizar el paquete (el manifest compilado no lo
 * menciona; incluso `getPackageArchiveInfo` lo devuelve). Por eso aquí solo se admite ese permiso
 * conocido; la comprobación estricta del APK, sin excepciones, es `aapt2 dump permissions` en
 * `scripts/verify-spec.sh`.
 */
class PrivacyInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val info = context.packageManager.getPackageInfo(
        context.packageName,
        PackageManager.PackageInfoFlags.of(
            (PackageManager.GET_PERMISSIONS or PackageManager.GET_RECEIVERS or PackageManager.GET_SERVICES).toLong(),
        ),
    )

    @Test
    fun theAppRequestsNoPermissions_beyondTheOneTheSystemAddsByItself() {
        val requested = info.requestedPermissions.orEmpty().toSet() - PLATFORM_IMPLICIT

        assertTrue("la app pide permisos: $requested", requested.isEmpty())
    }

    @Test
    fun theAppDeclaresNoPermissionsOfItsOwn() {
        val declared = info.permissions.orEmpty().map { it.name }

        assertTrue("la app declara permisos: $declared", declared.isEmpty())
    }

    @Test
    fun theAppHasNoBroadcastReceiversNorBackgroundServices() {
        // SPEC §8 (batería): sin servicios en segundo plano, alarmas, notificaciones ni receptores.
        assertNull("receptores declarados: ${info.receivers?.map { it.name }}", info.receivers)
        assertNull("servicios declarados: ${info.services?.map { it.name }}", info.services)
    }

    @Test
    fun internetIsNeverRequested_inAnyWay() {
        assertTrue(
            "INTERNET no debe aparecer jamás",
            info.requestedPermissions.orEmpty().none { it.contains("INTERNET") },
        )
    }

    @Test
    fun theAppIsNotBackedUp() {
        val flags = context.applicationInfo.flags
        assertEquals("allowBackup debe ser false", 0, flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    private companion object {
        /** Permisos que Android agrega solo al instalar, sin que el manifest los pida (ver el comentario de la clase). */
        val PLATFORM_IMPLICIT = setOf("android.permission.OTHER_SENSORS")
    }
}
