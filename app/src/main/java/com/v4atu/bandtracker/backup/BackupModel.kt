package com.v4atu.bandtracker.backup

import kotlinx.serialization.Serializable

/**
 * Archivo de exportación/importación (SPEC §7). `version` es la del esquema del archivo.
 * Los montos van en unidad mínima (entero) y las fechas en ISO `yyyy-MM-dd`.
 */
@Serializable
data class BackupFile(val version: Int, val accounts: List<BackupAccount>)

@Serializable
data class BackupAccount(
    val uuid: String,
    val name: String,
    val currency: String,
    val entries: List<BackupEntry> = emptyList(),
)

@Serializable
data class BackupEntry(
    val uuid: String,
    val type: String,
    val amountMinor: Long,
    val date: String,
    val subject: String? = null,
    val note: String? = null,
)

enum class InvalidReason {
    /** Falta un campo, sobra uno, o un valor tiene el tipo equivocado. */
    STRUCTURE,
    BLANK,
    DUPLICATE_UUID,
    UNKNOWN_CURRENCY,
    UNKNOWN_TYPE,
    NON_POSITIVE_AMOUNT,
    BAD_DATE,
    FUTURE_DATE,

    /** Una cuenta que ya existe localmente (mismo uuid) tiene otra moneda en el archivo. */
    CURRENCY_MISMATCH,
}

/** Por qué se rechazó un archivo. La UI lo traduce a un mensaje localizado (SPEC §7). */
sealed interface BackupError {
    data object NotValidJson : BackupError

    data class UnsupportedVersion(val found: Int, val supported: Int) : BackupError

    /** [location] es la ruta del campo, p. ej. `accounts[0].entries[2].date`; `null` si no se puede precisar. */
    data class InvalidData(val location: String?, val reason: InvalidReason) : BackupError
}

sealed interface DecodeResult {
    data class Valid(val file: BackupFile) : DecodeResult
    data class Failed(val error: BackupError) : DecodeResult
}
