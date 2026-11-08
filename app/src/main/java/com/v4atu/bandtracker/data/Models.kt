package com.v4atu.bandtracker.data

import com.v4atu.bandtracker.domain.EntryType
import com.v4atu.bandtracker.domain.NameCheck
import java.time.LocalDate

/** Cuenta tal como la ve el resto de la app (SPEC §4). */
data class Account(val id: Long, val uuid: String, val name: String, val currency: String)

/** Registro tal como lo ve el resto de la app (SPEC §4). */
data class Entry(
    val id: Long,
    val uuid: String,
    val accountId: Long,
    val type: EntryType,
    val amountMinor: Long,
    val date: LocalDate,
    val subject: String?,
    val note: String?,
)

/** Lo que el usuario captura al crear o editar un registro. */
data class EntryDraft(
    val type: EntryType,
    val amountMinor: Long,
    val date: LocalDate,
    val subject: String?,
    val note: String?,
)

sealed interface NameResult {
    data class Saved(val id: Long) : NameResult
    data class Rejected(val reason: NameCheck) : NameResult
}

sealed interface EntryResult {
    data class Saved(val id: Long) : EntryResult
    data object InvalidAmount : EntryResult
    data object FutureDate : EntryResult
}

internal fun AccountEntity.toAccount() = Account(id, uuid, name, currency)

internal fun EntryEntity.toEntry() = Entry(
    id = id,
    uuid = uuid,
    accountId = accountId,
    type = EntryType.valueOf(type),
    amountMinor = amountMinor,
    date = LocalDate.parse(date),
    subject = subject,
    note = note,
)
