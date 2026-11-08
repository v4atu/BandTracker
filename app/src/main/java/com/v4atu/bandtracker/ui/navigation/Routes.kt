package com.v4atu.bandtracker.ui.navigation

import com.v4atu.bandtracker.domain.EntryType
import kotlinx.serialization.Serializable

/** Rutas tipadas (Navigation Compose + kotlinx-serialization). Cada pantalla se agrega con su parte. */
@Serializable
data object Home

/** Captura de un registro nuevo (SPEC §5.2): Ingreso o Egreso según [type]. */
@Serializable
data class Capture(val type: EntryType)

/** Lista de cuentas, con exportar e importar (SPEC §5.4). */
@Serializable
data object Accounts

/** «Nueva cuenta» (SPEC §5.4): se llega desde Inicio sin cuentas o desde la lista de cuentas. */
@Serializable
data object CreateAccount

/** Ayuda (SPEC §5.5): versión, quién la mantiene, licencia y repositorio. */
@Serializable
data object Help

/** Licencias de terceros (DESIGN §3 y §5), que se abren desde Ayuda. */
@Serializable
data object Licenses

/** Historial de la cuenta seleccionada (SPEC §5.3). */
@Serializable
data object History

/** Edición de un registro existente (SPEC §5.3): se llega tocando una fila del Historial. */
@Serializable
data class EditEntry(val entryId: Long)

/** Claves con las que una pantalla devuelve un resultado a la anterior. */
object ResultKeys {
    /** `SavedNotice.name`: lo que acaba de guardarse, para el aviso breve de Inicio (SPEC §5.2). */
    const val SAVED_NOTICE = "saved_notice"
}
