package com.v4atu.bandtracker.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun findById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE uuid = :uuid")
    suspend fun findByUuid(uuid: String): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY nameKey")
    suspend fun getAll(): List<AccountEntity>

    /** Las cuentas por nombre; emite de nuevo cada vez que cambia la tabla. */
    @Query("SELECT * FROM accounts ORDER BY nameKey")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT name FROM accounts")
    suspend fun names(): List<String>

    /** Solo nombre y su clave: la moneda es inmutable (SPEC §4). [nameKey] = `TextKey.of(name)`. */
    @Query("UPDATE accounts SET name = :name, nameKey = :nameKey WHERE id = :id")
    suspend fun rename(id: Long, name: String, nameKey: String)

    /** Borra la cuenta; sus registros caen por `ON DELETE CASCADE`. */
    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)
}
