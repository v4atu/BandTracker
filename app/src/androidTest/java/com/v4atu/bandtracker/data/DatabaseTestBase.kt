package com.v4atu.bandtracker.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before

/** Base de las pruebas de datos: Room en memoria con el SQLite del sistema (SPEC §3). */
abstract class DatabaseTestBase {

    protected lateinit var db: BandDatabase
    private val extraDatabases = mutableListOf<BandDatabase>()

    @Before
    fun createDb() {
        db = buildDatabase()
    }

    @After
    fun closeDb() {
        db.close()
        extraDatabases.forEach { it.close() }
    }

    /** Otra base en memoria independiente (p. ej. «instalación limpia» para probar importaciones). */
    protected fun newDatabase(): BandDatabase = buildDatabase().also { extraDatabases += it }

    private fun buildDatabase(): BandDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder<BandDatabase>(context)
            .setDriver(AndroidSQLiteDriver())
            .build()
    }

    protected fun account(
        uuid: String = "acc-1",
        name: String = "Efectivo",
        currency: String = "COP",
    ) = AccountEntity.create(uuid = uuid, name = name, currency = currency)

    protected fun entry(
        accountId: Long,
        uuid: String,
        date: String = "2026-11-07",
        type: String = "INCOME",
        amountMinor: Long = 1000,
        subject: String? = null,
        note: String? = null,
    ) = EntryEntity.create(
        uuid = uuid,
        accountId = accountId,
        type = type,
        amountMinor = amountMinor,
        date = date,
        subject = subject,
        note = note,
    )
}
