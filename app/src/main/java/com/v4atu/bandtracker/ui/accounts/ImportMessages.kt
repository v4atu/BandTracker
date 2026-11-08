package com.v4atu.bandtracker.ui.accounts

import androidx.annotation.StringRes
import com.v4atu.bandtracker.R
import com.v4atu.bandtracker.backup.BackupError
import com.v4atu.bandtracker.backup.InvalidReason

/** Traduce el motivo por el que se rechazó un archivo a un mensaje claro y localizado (SPEC §7). */
object ImportMessages {

    @StringRes
    fun message(error: BackupError): Int = when (error) {
        BackupError.NotValidJson -> R.string.import_error_not_json
        is BackupError.UnsupportedVersion -> R.string.import_error_version
        is BackupError.InvalidData -> when (error.reason) {
            InvalidReason.STRUCTURE -> R.string.import_error_structure
            InvalidReason.BLANK -> R.string.import_error_blank
            InvalidReason.DUPLICATE_UUID -> R.string.import_error_duplicate_uuid
            InvalidReason.UNKNOWN_CURRENCY -> R.string.import_error_unknown_currency
            InvalidReason.UNKNOWN_TYPE -> R.string.import_error_unknown_type
            InvalidReason.NON_POSITIVE_AMOUNT -> R.string.import_error_non_positive_amount
            InvalidReason.BAD_DATE -> R.string.import_error_bad_date
            InvalidReason.FUTURE_DATE -> R.string.import_error_future_date
            InvalidReason.CURRENCY_MISMATCH -> R.string.import_error_currency_mismatch
        }
    }

    /** Dónde del archivo está el problema (`accounts[0].entries[2].date`), si se sabe. */
    fun location(error: BackupError): String? = (error as? BackupError.InvalidData)?.location
}
