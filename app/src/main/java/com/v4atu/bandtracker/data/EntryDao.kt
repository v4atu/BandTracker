package com.v4atu.bandtracker.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update

@Dao
interface EntryDao {

    @Insert
    suspend fun insert(entry: EntryEntity): Long

    @Update
    suspend fun update(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun findById(id: Long): EntryEntity?

    /** Todos los registros de la cuenta por orden de alta (id ascendente), para exportar. */
    @Query("SELECT * FROM entries WHERE accountId = :accountId ORDER BY id ASC")
    suspend fun allForAccount(accountId: Long): List<EntryEntity>

    @Query("SELECT uuid FROM entries")
    suspend fun allUuids(): List<String>

    @Query("SELECT COUNT(*) FROM entries WHERE accountId = :accountId")
    suspend fun count(accountId: Long): Int

    /** Historial acotado de una cuenta, orden global `date DESC, id DESC` (SPEC §4, §5.3, §8). */
    @Query(
        "SELECT * FROM entries WHERE accountId = :accountId " +
            "ORDER BY date DESC, id DESC LIMIT :limit OFFSET :offset",
    )
    suspend fun page(accountId: Long, limit: Int, offset: Int): List<EntryEntity>

    /**
     * Sujetos distintos (por clave) más recientes de la cuenta, el más reciente primero; se usa
     * con el campo Sujeto vacío (SPEC §5.2). Devuelve la variante escrita más recientemente.
     */
    @Query(
        "SELECT subject FROM entries WHERE id IN (" +
            "SELECT MAX(id) FROM entries WHERE accountId = :accountId AND subjectKey IS NOT NULL " +
            "GROUP BY subjectKey) ORDER BY id DESC LIMIT :limit",
    )
    suspend fun recentSubjects(accountId: Long, limit: Int): List<String>

    /**
     * Sujetos de la cuenta cuya clave empieza por [keyPrefix] (SPEC §5.2). [keyPrefix] debe ser
     * `TextKey.of(textoDelUsuario)`: así se ignoran mayúsculas y tildes pero no la ñ. Devuelve una
     * sola variante por clave —la escrita más recientemente— ordenada por clave. `%` y `_` del
     * prefijo se tratan como texto literal.
     */
    @Query(
        "SELECT subject FROM entries WHERE id IN (" +
            "SELECT MAX(id) FROM entries WHERE accountId = :accountId AND subjectKey IS NOT NULL " +
            "AND subjectKey LIKE replace(replace(replace(:keyPrefix, '\\', '\\\\'), '%', '\\%'), '_', '\\_') || '%' ESCAPE '\\' " +
            "GROUP BY subjectKey) ORDER BY subjectKey LIMIT :limit",
    )
    suspend fun suggestSubjects(accountId: Long, keyPrefix: String, limit: Int): List<String>
}
