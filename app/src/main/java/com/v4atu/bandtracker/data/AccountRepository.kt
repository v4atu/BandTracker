package com.v4atu.bandtracker.data

import androidx.sqlite.SQLiteException
import com.v4atu.bandtracker.domain.AccountNames
import com.v4atu.bandtracker.domain.NameCheck
import com.v4atu.bandtracker.domain.TextKey
import java.util.Currency
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cuentas (SPEC §4, §5.4). Aquí se aplica la regla de nombre único; el índice único sobre
 * `nameKey` es la red de seguridad ante una carrera.
 */
class AccountRepository(
    private val db: BandDatabase,
    private val newUuid: () -> String = { UUID.randomUUID().toString() },
) {
    private val dao get() = db.accountDao()

    suspend fun list(): List<Account> = dao.getAll().map { it.toAccount() }

    /** Las cuentas por nombre, observables: la interfaz se actualiza sola al crear, renombrar o borrar. */
    val accounts: Flow<List<Account>> get() = dao.observeAll().map { rows -> rows.map { it.toAccount() } }

    suspend fun entryCount(accountId: Long): Int = db.entryDao().count(accountId)

    /** @throws IllegalArgumentException si [currency] no es un código ISO 4217 conocido. */
    suspend fun create(name: String, currency: String): NameResult {
        Currency.getInstance(currency) // valida el código; la moneda es inmutable después
        val clean = AccountNames.normalized(name)
        val check = AccountNames.check(clean, dao.names())
        if (check != NameCheck.OK) return NameResult.Rejected(check)

        return try {
            NameResult.Saved(dao.insert(AccountEntity.create(newUuid(), clean, currency)))
        } catch (_: SQLiteException) {
            NameResult.Rejected(NameCheck.DUPLICATE)
        }
    }

    suspend fun rename(id: Long, name: String): NameResult {
        val current = requireNotNull(dao.findById(id)) { "No existe la cuenta $id" }
        val clean = AccountNames.normalized(name)
        val check = AccountNames.check(clean, dao.names(), currentName = current.name)
        if (check != NameCheck.OK) return NameResult.Rejected(check)

        return try {
            dao.rename(id, clean, TextKey.of(clean))
            NameResult.Saved(id)
        } catch (_: SQLiteException) {
            NameResult.Rejected(NameCheck.DUPLICATE)
        }
    }

    /** Borra la cuenta y, por cascada, todos sus registros. */
    suspend fun delete(id: Long) = dao.delete(id)
}
