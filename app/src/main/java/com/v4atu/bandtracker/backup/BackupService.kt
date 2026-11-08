package com.v4atu.bandtracker.backup

import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import com.v4atu.bandtracker.data.AccountEntity
import com.v4atu.bandtracker.data.BandDatabase
import com.v4atu.bandtracker.data.EntryEntity
import com.v4atu.bandtracker.domain.EntryDates
import com.v4atu.bandtracker.domain.TextKey
import java.time.Clock
import java.time.LocalDate

/** Una cuenta importada que se renombró porque su nombre ya lo usaba otra cuenta local. */
data class AccountRename(val from: String, val to: String)

data class ImportSummary(
    val accountsAdded: Int,
    val accountsSkipped: Int,
    val entriesAdded: Int,
    val entriesSkipped: Int,
    val renamedAccounts: List<AccountRename>,
)

sealed interface ImportResult {
    data class Success(val summary: ImportSummary) : ImportResult
    data class Rejected(val error: BackupError) : ImportResult
}

/**
 * Exportar e importar todos los datos (SPEC §7). Importar **fusiona**: agrega lo que no existe
 * (por `uuid`) y nunca borra ni sobrescribe. Es atómico: o se aplica todo o no cambia nada.
 */
class BackupService(
    private val db: BandDatabase,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val accountDao get() = db.accountDao()
    private val entryDao get() = db.entryDao()

    /** Todas las cuentas con sus registros, en una lectura consistente. Registros por orden de alta. */
    suspend fun exportJson(): String {
        val file = db.withReadTransaction {
            val accounts = accountDao.getAll().map { account ->
                BackupAccount(
                    uuid = account.uuid,
                    name = account.name,
                    currency = account.currency,
                    entries = entryDao.allForAccount(account.id).map { it.toBackup() },
                )
            }
            BackupFile(BackupCodec.CURRENT_VERSION, accounts)
        }
        return BackupCodec.encode(file)
    }

    suspend fun importJson(json: String): ImportResult {
        val file = when (val decoded = BackupCodec.decode(json, EntryDates.today(clock))) {
            is DecodeResult.Failed -> return ImportResult.Rejected(decoded.error)
            is DecodeResult.Valid -> decoded.file
        }
        return try {
            // Cualquier excepción dentro del bloque deshace la transacción completa.
            ImportResult.Success(db.withWriteTransaction { merge(file) })
        } catch (aborted: ImportAborted) {
            ImportResult.Rejected(aborted.error)
        }
    }

    private suspend fun merge(file: BackupFile): ImportSummary {
        val localAccounts = accountDao.getAll()
        val byUuid = localAccounts.associateBy { it.uuid }
        val takenKeys = localAccounts.mapTo(HashSet()) { it.nameKey }
        val knownEntryUuids = entryDao.allUuids().toHashSet()

        var accountsAdded = 0
        var accountsSkipped = 0
        var entriesAdded = 0
        var entriesSkipped = 0
        val renamed = mutableListOf<AccountRename>()

        file.accounts.forEachIndexed { index, account ->
            val local = byUuid[account.uuid]
            val accountId = if (local != null) {
                if (local.currency != account.currency) {
                    throw ImportAborted(BackupError.InvalidData("accounts[$index].currency", InvalidReason.CURRENCY_MISMATCH))
                }
                accountsSkipped++
                local.id
            } else {
                val name = account.name.trim()
                val finalName = uniqueName(name, takenKeys)
                if (finalName != name) renamed += AccountRename(name, finalName)
                takenKeys += TextKey.of(finalName)
                accountsAdded++
                accountDao.insert(AccountEntity.create(account.uuid, finalName, account.currency))
            }

            account.entries.forEach { entry ->
                if (knownEntryUuids.add(entry.uuid)) {
                    entryDao.insert(entry.toEntity(accountId))
                    entriesAdded++
                } else {
                    entriesSkipped++
                }
            }
        }
        return ImportSummary(accountsAdded, accountsSkipped, entriesAdded, entriesSkipped, renamed)
    }

    /** [name] si su clave está libre; si no, «name (2)», «name (3)»… el primero libre. */
    private fun uniqueName(name: String, takenKeys: Set<String>): String {
        if (TextKey.of(name) !in takenKeys) return name
        var n = 2
        while (TextKey.of("$name ($n)") in takenKeys) n++
        return "$name ($n)"
    }

    private fun EntryEntity.toBackup() = BackupEntry(uuid, type, amountMinor, date, subject, note)

    private fun BackupEntry.toEntity(accountId: Long) = EntryEntity.create(
        uuid = uuid,
        accountId = accountId,
        type = type,
        amountMinor = amountMinor,
        date = LocalDate.parse(date).toString(),
        subject = subject?.trim()?.ifEmpty { null },
        note = note?.trim()?.ifEmpty { null },
    )

    private class ImportAborted(val error: BackupError) : RuntimeException()
}
