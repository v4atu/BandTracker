package com.v4atu.bandtracker.backup

import com.v4atu.bandtracker.domain.EntryDates
import com.v4atu.bandtracker.domain.EntryType
import com.v4atu.bandtracker.domain.Money
import java.time.DateTimeException
import java.time.LocalDate
import java.util.Currency
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Codifica y decodifica el archivo de respaldo (SPEC §7). Decodificar valida todo antes de que
 * nada toque la base de datos: versión, estructura estricta (sin claves desconocidas) y datos.
 */
object BackupCodec {

    const val CURRENT_VERSION = 1

    private val writer = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    // Valores por defecto de Json: estricto con claves desconocidas y tipos incorrectos.
    private val reader = Json

    fun encode(file: BackupFile): String = writer.encodeToString(BackupFile.serializer(), file)

    fun decode(json: String, today: LocalDate): DecodeResult {
        val root = try {
            reader.parseToJsonElement(json)
        } catch (_: IllegalArgumentException) {
            return failed(BackupError.NotValidJson)
        }
        if (root !is JsonObject) return invalid(null, InvalidReason.STRUCTURE)

        // La versión se lee antes que el resto: una versión futura puede tener otra forma.
        val version = (root["version"] as? JsonPrimitive)
            ?.takeIf { !it.isString }
            ?.longOrNull
            ?.takeIf { it >= 1 }
            ?: return invalid("version", InvalidReason.STRUCTURE)
        if (version > CURRENT_VERSION) {
            return failed(BackupError.UnsupportedVersion(version.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), CURRENT_VERSION))
        }

        val file = try {
            reader.decodeFromJsonElement(BackupFile.serializer(), root)
        } catch (_: IllegalArgumentException) {
            return invalid(null, InvalidReason.STRUCTURE)
        }
        return validate(file, today)
    }

    private fun validate(file: BackupFile, today: LocalDate): DecodeResult {
        val accountUuids = HashSet<String>()
        val entryUuids = HashSet<String>()

        file.accounts.forEachIndexed { a, account ->
            val at = "accounts[$a]"
            if (account.uuid.isBlank()) return invalid("$at.uuid", InvalidReason.BLANK)
            if (!accountUuids.add(account.uuid)) return invalid("$at.uuid", InvalidReason.DUPLICATE_UUID)
            if (account.name.isBlank()) return invalid("$at.name", InvalidReason.BLANK)
            if (!isKnownCurrency(account.currency)) return invalid("$at.currency", InvalidReason.UNKNOWN_CURRENCY)

            account.entries.forEachIndexed { e, entry ->
                val of = "$at.entries[$e]"
                if (entry.uuid.isBlank()) return invalid("$of.uuid", InvalidReason.BLANK)
                if (!entryUuids.add(entry.uuid)) return invalid("$of.uuid", InvalidReason.DUPLICATE_UUID)
                if (EntryType.entries.none { it.name == entry.type }) return invalid("$of.type", InvalidReason.UNKNOWN_TYPE)
                if (!Money.isValidAmount(entry.amountMinor)) return invalid("$of.amountMinor", InvalidReason.NON_POSITIVE_AMOUNT)

                val date = parseDate(entry.date) ?: return invalid("$of.date", InvalidReason.BAD_DATE)
                if (!EntryDates.isAllowed(date, today)) return invalid("$of.date", InvalidReason.FUTURE_DATE)
            }
        }
        return DecodeResult.Valid(file)
    }

    private fun isKnownCurrency(code: String): Boolean =
        try {
            Currency.getInstance(code)
            true
        } catch (_: IllegalArgumentException) {
            false
        }

    private fun parseDate(text: String): LocalDate? =
        try {
            LocalDate.parse(text)
        } catch (_: DateTimeException) {
            null
        }

    private fun failed(error: BackupError) = DecodeResult.Failed(error)

    private fun invalid(location: String?, reason: InvalidReason) =
        failed(BackupError.InvalidData(location, reason))
}
