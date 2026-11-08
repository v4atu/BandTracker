package com.v4atu.bandtracker

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.v4atu.bandtracker.backup.BackupService
import com.v4atu.bandtracker.data.AccountRepository
import com.v4atu.bandtracker.data.AccountSelection
import com.v4atu.bandtracker.data.BandDatabase
import com.v4atu.bandtracker.data.DeviceClock
import com.v4atu.bandtracker.data.EntryRepository
import com.v4atu.bandtracker.data.SelectedAccountStore

/**
 * Dependencias de la app, creadas bajo demanda y compartidas (una sola base de datos). Sin
 * framework de inyección: son pocas y así no hay dependencias de más (SPEC §3).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: BandDatabase by lazy {
        Room.databaseBuilder<BandDatabase>(appContext, DATABASE_NAME)
            .setDriver(AndroidSQLiteDriver())
            .build()
    }

    val accounts: AccountRepository by lazy { AccountRepository(database) }
    val entries: EntryRepository by lazy { EntryRepository(database, DeviceClock) }
    val backup: BackupService by lazy { BackupService(database, DeviceClock) }

    val selectedAccountStore: SelectedAccountStore by lazy {
        SelectedAccountStore(appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE))
    }

    /** La cuenta seleccionada, compartida por todas las pantallas (SPEC §5.1). */
    val selection: AccountSelection by lazy { AccountSelection(accounts, selectedAccountStore) }

    private companion object {
        const val DATABASE_NAME = "bandtracker.db"
        const val PREFERENCES_NAME = "bandtracker"
    }
}
