package com.v4atu.bandtracker.data

import com.v4atu.bandtracker.domain.EntryType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** SPEC §4 (monto > 0, fecha no futura), §5.2 (sugerencias), §5.3 (historial, edición, borrado). */
class EntryRepositoryTest : DatabaseTestBase() {

    // 2026-11-08 02:30 UTC es 7 de noviembre en Bogotá: "hoy" sigue la zona del reloj.
    private val clock = Clock.fixed(Instant.parse("2026-11-08T02:30:00Z"), ZoneId.of("America/Bogota"))
    private val today = LocalDate.of(2026, 11, 7)

    private lateinit var repo: EntryRepository
    private var accountId = 0L
    private var counter = 0

    @Before
    fun setUpRepo() = kotlinx.coroutines.runBlocking {
        accountId = db.accountDao().insert(account(uuid = "acc", name = "Efectivo"))
        repo = EntryRepository(db, clock) { "e-${++counter}" }
    }

    private fun draft(
        type: EntryType = EntryType.INCOME,
        amountMinor: Long = 1000,
        date: LocalDate = today,
        subject: String? = null,
        note: String? = null,
    ) = EntryDraft(type, amountMinor, date, subject, note)

    private suspend fun add(d: EntryDraft = draft()): Long = (repo.add(accountId, d) as EntryResult.Saved).id

    // --- Alta ---

    @Test
    fun add_savesAllFieldsAndMapsToDomainTypes() = runTest {
        val id = add(draft(EntryType.EXPENSE, 15000, today.minusDays(2), "Taxi", "Aeropuerto"))

        val entry = repo.find(id)!!
        assertEquals(EntryType.EXPENSE, entry.type)
        assertEquals(15000L, entry.amountMinor)
        assertEquals(today.minusDays(2), entry.date)
        assertEquals("Taxi", entry.subject)
        assertEquals("Aeropuerto", entry.note)
        assertEquals(accountId, entry.accountId)
        assertEquals("e-1", entry.uuid)
    }

    @Test
    fun add_trimsTextAndTurnsBlankIntoNull() = runTest {
        val id = add(draft(subject = "  Taxi ", note = "   "))

        val entry = repo.find(id)!!
        assertEquals("Taxi", entry.subject)
        assertNull(entry.note)
    }

    @Test
    fun add_rejectsZeroAndNegativeAmounts() = runTest {
        assertEquals(EntryResult.InvalidAmount, repo.add(accountId, draft(amountMinor = 0)))
        assertEquals(EntryResult.InvalidAmount, repo.add(accountId, draft(amountMinor = -5)))
        assertEquals(0, db.entryDao().count(accountId))
    }

    @Test
    fun add_rejectsFutureDatesButAcceptsLocalToday() = runTest {
        assertEquals(EntryResult.FutureDate, repo.add(accountId, draft(date = today.plusDays(1))))
        assertEquals(0, db.entryDao().count(accountId))

        // El día UTC ya es el 8, pero en Bogotá hoy es el 7: el 8 es futuro.
        assertEquals(EntryResult.FutureDate, repo.add(accountId, draft(date = LocalDate.of(2026, 11, 8))))
        assertEquals(true, repo.add(accountId, draft(date = today)) is EntryResult.Saved)
    }

    // --- Edición y borrado ---

    @Test
    fun update_changesFieldsAndRecomputesTheSubjectKey() = runTest {
        val id = add(draft(subject = "Taxi"))

        val result = repo.update(id, draft(EntryType.EXPENSE, 2500, today.minusDays(1), "Café", "n"))

        assertEquals(EntryResult.Saved(id), result)
        val entry = repo.find(id)!!
        assertEquals("Café", entry.subject)
        assertEquals(EntryType.EXPENSE, entry.type)
        assertEquals(2500L, entry.amountMinor)
        assertEquals(listOf("Café"), repo.suggestSubjects(accountId, "cafe"))
        assertEquals(emptyList<String>(), repo.suggestSubjects(accountId, "tax"))
    }

    @Test
    fun update_appliesTheSameRulesAsAdd() = runTest {
        val id = add()

        assertEquals(EntryResult.InvalidAmount, repo.update(id, draft(amountMinor = 0)))
        assertEquals(EntryResult.FutureDate, repo.update(id, draft(date = today.plusDays(1))))
        assertEquals(1000L, repo.find(id)!!.amountMinor)
    }

    @Test
    fun update_keepsUuidAndAccount() = runTest {
        val id = add()

        repo.update(id, draft(amountMinor = 7))

        val entry = repo.find(id)!!
        assertEquals("e-1", entry.uuid)
        assertEquals(accountId, entry.accountId)
    }

    @Test
    fun delete_removesTheEntry() = runTest {
        val id = add()

        repo.delete(id)

        assertNull(repo.find(id))
    }

    // --- Historial ---

    @Test
    fun page_returnsDomainEntriesNewestFirstAndBounded() = runTest {
        (1..5).forEach { add(draft(date = today.minusDays((5 - it).toLong()), subject = "s$it")) }

        val first = repo.page(accountId, pageIndex = 0, pageSize = 2)
        val second = repo.page(accountId, pageIndex = 1, pageSize = 2)
        val third = repo.page(accountId, pageIndex = 2, pageSize = 2)

        assertEquals(listOf("s5", "s4"), first.map { it.subject })
        assertEquals(listOf("s3", "s2"), second.map { it.subject })
        assertEquals(listOf("s1"), third.map { it.subject })
    }

    // --- Sugerencias ---

    @Test
    fun suggestSubjects_normalizesTheUserInput() = runTest {
        add(draft(subject = "Café con leche"))
        add(draft(subject = "Ñandú"))
        add(draft(subject = "Nandina"))

        assertEquals(listOf("Café con leche"), repo.suggestSubjects(accountId, "CAFE"))
        assertEquals(listOf("Ñandú"), repo.suggestSubjects(accountId, " ñan"))
        assertEquals(listOf("Nandina"), repo.suggestSubjects(accountId, "nan"))
    }

    @Test
    fun suggestSubjects_returnsAtMostFive() = runTest {
        (1..8).forEach { add(draft(subject = "Sujeto $it")) }

        assertEquals(5, repo.suggestSubjects(accountId, "suj").size)
    }

    // Contrato definido por V4atu: con el campo vacío se sugieren los sujetos más recientes.
    @Test
    fun suggestSubjects_blankInputSuggestsMostRecentSubjectsFirst() = runTest {
        add(draft(subject = "Taxi"))
        add(draft(subject = "Mercado"))
        add(draft(subject = "Café"))

        assertEquals(listOf("Café", "Mercado", "Taxi"), repo.suggestSubjects(accountId, ""))
        assertEquals(listOf("Café", "Mercado", "Taxi"), repo.suggestSubjects(accountId, "   "))
    }

    @Test
    fun suggestSubjects_blankInputIsCappedAtFiveAndSkipsEntriesWithoutSubject() = runTest {
        (1..7).forEach { add(draft(subject = "S$it")) }
        add(draft(subject = null))

        assertEquals(listOf("S7", "S6", "S5", "S4", "S3"), repo.suggestSubjects(accountId, ""))
    }

    @Test
    fun suggestSubjects_blankInputCollapsesRepeatedSubjectsToTheirLatestUse() = runTest {
        add(draft(subject = "Taxi"))
        add(draft(subject = "Mercado"))
        add(draft(subject = "taxi"))

        assertEquals(listOf("taxi", "Mercado"), repo.suggestSubjects(accountId, ""))
    }
}
