package com.v4atu.bandtracker

import android.content.Context
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de humo del stack definido en SPEC.md §3:
 * - Room (KSP + AndroidSQLiteDriver, SQLite del sistema) genera código y persiste.
 * - kotlinx-serialization (plugin + runtime JSON) serializa y deserializa.
 *
 * Las clases de apoyo son solo de prueba; el modelo real (Account/Entry) llega en su propio ciclo TDD.
 */
@RunWith(AndroidJUnit4::class)
class StackSmokeTest {

    @Entity(tableName = "smoke")
    data class SmokeEntity(
        @PrimaryKey val id: Long,
        val label: String,
    )

    @Dao
    interface SmokeDao {
        @Insert
        suspend fun insert(entity: SmokeEntity)

        @Query("SELECT * FROM smoke WHERE label = :label")
        suspend fun findByLabel(label: String): List<SmokeEntity>
    }

    @Database(entities = [SmokeEntity::class], version = 1, exportSchema = false)
    abstract class SmokeDatabase : RoomDatabase() {
        abstract fun smokeDao(): SmokeDao
    }

    @Serializable
    data class SmokePayload(val version: Int, val name: String)

    private lateinit var db: SmokeDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<SmokeDatabase>(context)
            .setDriver(AndroidSQLiteDriver())
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun room_persistsAndReadsBack() = runTest {
        val entity = SmokeEntity(id = 1, label = "efectivo")

        db.smokeDao().insert(entity)

        assertEquals(listOf(entity), db.smokeDao().findByLabel("efectivo"))
    }

    @Test
    fun serialization_roundTripsJson() {
        val payload = SmokePayload(version = 1, name = "BandTracker")

        val json = Json.encodeToString(SmokePayload.serializer(), payload)
        val decoded = Json.decodeFromString(SmokePayload.serializer(), json)

        assertEquals(payload, decoded)
    }
}
