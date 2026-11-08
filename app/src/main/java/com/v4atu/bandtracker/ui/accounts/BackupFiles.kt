package com.v4atu.bandtracker.ui.accounts

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lee y escribe el archivo de respaldo que el usuario eligió con el selector del sistema
 * (`CreateDocument` / `OpenDocument`, SPEC §7): no hace falta ningún permiso de almacenamiento, el
 * `Uri` ya trae el acceso. Todo se hace fuera del hilo principal.
 */
object BackupFiles {

    /** Tamaño máximo de un archivo a importar (un respaldo real pesa unos pocos MB): evita agotar la memoria. */
    const val MAX_BYTES = 32 * 1024 * 1024

    /** Guarda [text] en UTF-8, reemplazando todo el contenido anterior. `false` si no se pudo escribir. */
    suspend fun write(resolver: ContentResolver, uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            resolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    /** El contenido del archivo como texto, o `null` si no se puede leer o pesa más de [maxBytes]. */
    suspend fun read(resolver: ContentResolver, uri: Uri, maxBytes: Int = MAX_BYTES): String? = withContext(Dispatchers.IO) {
        try {
            resolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readNBytes(maxBytes + 1)
                if (bytes.size > maxBytes) null else String(bytes, Charsets.UTF_8)
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
}
