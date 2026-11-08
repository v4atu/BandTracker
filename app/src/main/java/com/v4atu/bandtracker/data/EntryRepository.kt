package com.v4atu.bandtracker.data

import com.v4atu.bandtracker.domain.EntryDates
import com.v4atu.bandtracker.domain.Money
import com.v4atu.bandtracker.domain.TextKey
import java.time.Clock
import java.util.UUID

/** Registros (SPEC §4, §5.2, §5.3). Valida monto y fecha antes de tocar la base. */
class EntryRepository(
    private val db: BandDatabase,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val newUuid: () -> String = { UUID.randomUUID().toString() },
) {
    private val dao get() = db.entryDao()

    suspend fun add(accountId: Long, draft: EntryDraft): EntryResult {
        validate(draft)?.let { return it }
        return EntryResult.Saved(dao.insert(draft.toEntity(newUuid(), accountId)))
    }

    suspend fun update(id: Long, draft: EntryDraft): EntryResult {
        val existing = requireNotNull(dao.findById(id)) { "No existe el registro $id" }
        validate(draft)?.let { return it }
        dao.update(draft.toEntity(existing.uuid, existing.accountId).copy(id = id))
        return EntryResult.Saved(id)
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun find(id: Long): Entry? = dao.findById(id)?.toEntry()

    /** Página [pageIndex] (desde 0) del historial: más reciente primero (SPEC §4, §5.3). */
    suspend fun page(accountId: Long, pageIndex: Int, pageSize: Int): List<Entry> =
        dao.page(accountId, pageSize, pageIndex * pageSize).map { it.toEntry() }

    /**
     * Hasta [MAX_SUGGESTIONS] sujetos de la cuenta que empiezan por [input]. Con el campo vacío
     * devuelve los más recientes (SPEC §5.2).
     */
    suspend fun suggestSubjects(accountId: Long, input: String): List<String> {
        val key = TextKey.of(input)
        return if (key.isEmpty()) {
            dao.recentSubjects(accountId, MAX_SUGGESTIONS)
        } else {
            dao.suggestSubjects(accountId, key, MAX_SUGGESTIONS)
        }
    }

    private fun validate(draft: EntryDraft): EntryResult? = when {
        !Money.isValidAmount(draft.amountMinor) -> EntryResult.InvalidAmount
        !EntryDates.isAllowed(draft.date, EntryDates.today(clock)) -> EntryResult.FutureDate
        else -> null
    }

    private fun EntryDraft.toEntity(uuid: String, accountId: Long) = EntryEntity.create(
        uuid = uuid,
        accountId = accountId,
        type = type.name,
        amountMinor = amountMinor,
        date = date.toString(),
        subject = subject?.trim()?.ifEmpty { null },
        note = note?.trim()?.ifEmpty { null },
    )

    companion object {
        const val MAX_SUGGESTIONS = 5
    }
}
