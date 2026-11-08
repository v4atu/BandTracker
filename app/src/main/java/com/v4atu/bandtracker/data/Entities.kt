package com.v4atu.bandtracker.data

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.v4atu.bandtracker.domain.TextKey

/**
 * Cuenta (SPEC §4). La unicidad del nombre (sin mayúsculas ni tildes, la ñ se distingue) se
 * aplica sobre [nameKey], calculada con [TextKey]; la regla de negocio vive en `AccountNames` y el
 * índice único es la red de seguridad. `currency` es inmutable: ningún DAO la actualiza.
 * Usar [create] para no olvidar la clave.
 */
@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["nameKey"], unique = true),
    ],
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val nameKey: String,
    val currency: String,
) {
    companion object {
        fun create(uuid: String, name: String, currency: String) =
            AccountEntity(uuid = uuid, name = name, nameKey = TextKey.of(name), currency = currency)
    }
}

/**
 * Registro (SPEC §4). `type` ("INCOME"/"EXPENSE") y `date` (ISO `yyyy-MM-dd`) se guardan como
 * texto: el ISO ordena igual que la fecha y evita convertidores. `subjectKey` es la clave de
 * comparación del sujeto ([TextKey]); es `null` si no hay sujeto. Usar [create].
 */
@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(
            value = ["accountId", "date", "id"],
            orders = [Index.Order.ASC, Index.Order.DESC, Index.Order.DESC],
        ),
        Index(value = ["accountId", "subjectKey"]),
    ],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val accountId: Long,
    val type: String,
    val amountMinor: Long,
    val date: String,
    val subject: String? = null,
    val subjectKey: String? = null,
    val note: String? = null,
) {
    companion object {
        fun create(
            uuid: String,
            accountId: Long,
            type: String,
            amountMinor: Long,
            date: String,
            subject: String? = null,
            note: String? = null,
        ) = EntryEntity(
            uuid = uuid,
            accountId = accountId,
            type = type,
            amountMinor = amountMinor,
            date = date,
            subject = subject,
            subjectKey = subject?.let(TextKey::of)?.ifEmpty { null },
            note = note,
        )
    }
}
