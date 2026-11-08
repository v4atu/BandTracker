package com.v4atu.bandtracker.data

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Recuerda entre ejecuciones la última cuenta seleccionada (SPEC §5.1). Un solo dato, así que
 * `SharedPreferences` (API de Android, sin dependencias): se lee de forma síncrona al arrancar,
 * sin parpadeo de pantalla vacía.
 */
class SelectedAccountStore(private val prefs: SharedPreferences) {

    private val _id = MutableStateFlow(read())

    /** Id recordado, o `null` si nunca se eligió una cuenta. */
    val id: StateFlow<Long?> = _id.asStateFlow()

    fun set(id: Long?) {
        prefs.edit {
            if (id == null) remove(KEY) else putLong(KEY, id)
        }
        _id.value = id
    }

    private fun read(): Long? = if (prefs.contains(KEY)) prefs.getLong(KEY, 0L) else null

    private companion object {
        const val KEY = "selected_account_id"
    }
}
