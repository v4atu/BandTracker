package com.v4atu.bandtracker.backup

import com.v4atu.bandtracker.data.AccountRepository
import com.v4atu.bandtracker.data.BandDatabase
import com.v4atu.bandtracker.data.DatabaseTestBase
import com.v4atu.bandtracker.data.EntryDraft
import com.v4atu.bandtracker.data.EntryRepository
import com.v4atu.bandtracker.data.EntryResult
import com.v4atu.bandtracker.data.NameResult
import com.v4atu.bandtracker.domain.EntryType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC §7 y criterio 5: export → import reproduce los datos; el segundo import no duplica; atomicidad. */
class BackupServiceTest : DatabaseTestBase() {

    // Hoy = 2026-11-07 en Bogotá.
    private val clock = Clock.fixed(Instant.parse("2026-11-08T02:30:00Z"), ZoneId.of("America/Bogota"))
    private val today = LocalDate.of(2026, 11, 7)

    /** Cada entorno (base + repositorios) genera uuids con su propia etiqueta para no chocar con otro. */
    private class Env(val db: BandDatabase, clock: Clock, tag: Int) {
        private var n = 0
        val accounts = AccountRepository(db) { "env$tag-a${++n}" }
        val entries = EntryRepository(db, clock) { "env$tag-e${++n}" }
        val backup = BackupService(db, clock)
    }

    private var envCount = 0

    private fun env(database: BandDatabase = db) = Env(database, clock, ++envCount)

    private fun draft(
        type: EntryType = EntryType.INCOME,
        amount: Long = 1000,
        date: LocalDate = today,
        subject: String? = null,
        note: String? = null,
    ) = EntryDraft(type, amount, date, subject, note)

    private suspend fun Env.account(name: String, currency: String = "COP") =
        (accounts.create(name, currency) as NameResult.Saved).id

    private suspend fun Env.entry(accountId: Long, d: EntryDraft) =
        (entries.add(accountId, d) as EntryResult.Saved).id

    private suspend fun Env.seed(): Long {
        val cash = account("Efectivo", "COP")
        entry(cash, draft(EntryType.INCOME, 5_000_000, today.minusDays(3), "Sueldo", "Noviembre"))
        entry(cash, draft(EntryType.EXPENSE, 1_550, today, "Café", null))
        entry(cash, draft(EntryType.EXPENSE, 2_000, today, "Ñandú", "mismo día, después"))
        val usd = account("Ahorros", "USD")
        entry(usd, draft(EntryType.INCOME, 12_345, today.minusDays(40)))
        return cash
    }

    private fun success(result: ImportResult) = (result as ImportResult.Success).summary

    private fun rejected(result: ImportResult) = (result as ImportResult.Rejected).error

    // --- Exportar ---

    @Test
    fun export_ofAnEmptyDatabase_hasVersionAndNoAccounts() = runTest {
        val file = (BackupCodec.decode(env().backup.exportJson(), today) as DecodeResult.Valid).file

        assertEquals(BackupFile(BackupCodec.CURRENT_VERSION, emptyList()), file)
    }

    @Test
    fun export_includesEveryAccountAndEntryWithAllFields() = runTest {
        val e = env()
        e.seed()

        val file = (BackupCodec.decode(e.backup.exportJson(), today) as DecodeResult.Valid).file

        assertEquals(listOf("Ahorros", "Efectivo"), file.accounts.map { it.name })
        val cash = file.accounts.first { it.name == "Efectivo" }
        assertEquals("COP", cash.currency)
        assertEquals(3, cash.entries.size)
        val first = cash.entries.first()
        assertEquals("INCOME", first.type)
        assertEquals(5_000_000L, first.amountMinor)
        assertEquals("2026-11-04", first.date)
        assertEquals("Sueldo", first.subject)
        assertEquals("Noviembre", first.note)
        assertEquals(1, file.accounts.first { it.name == "Ahorros" }.entries.size)
    }

    @Test
    fun export_listsEntriesByInsertionOrder_soTieBreakSurvives() = runTest {
        val e = env()
        e.seed()

        val cash = (BackupCodec.decode(e.backup.exportJson(), today) as DecodeResult.Valid).file
            .accounts.first { it.name == "Efectivo" }

        // Orden de alta (id ascendente), no el del historial: así el desempate sobrevive al reimportar.
        assertEquals(listOf("Sueldo", "Café", "Ñandú"), cash.entries.map { it.subject })
    }

    // --- Importar en una instalación limpia ---

    @Test
    fun exportThenImportOnACleanInstall_reproducesTheSameFile() = runTest {
        val source = env()
        source.seed()
        val json = source.backup.exportJson()

        val target = env(newDatabase())
        val summary = success(target.backup.importJson(json))

        assertEquals(2, summary.accountsAdded)
        assertEquals(4, summary.entriesAdded)
        assertEquals(0, summary.accountsSkipped + summary.entriesSkipped)
        assertEquals(json, target.backup.exportJson())
    }

    @Test
    fun import_preservesTheHistoryOrderIncludingSameDayTieBreak() = runTest {
        val source = env()
        val cashId = source.seed()
        val target = env(newDatabase())
        target.backup.importJson(source.backup.exportJson())

        val sourceOrder = source.entries.page(cashId, 0, 10).map { it.subject }
        val targetCash = target.accounts.list().first { it.name == "Efectivo" }
        val targetOrder = target.entries.page(targetCash.id, 0, 10).map { it.subject }

        assertEquals(listOf("Ñandú", "Café", "Sueldo"), sourceOrder)
        assertEquals(sourceOrder, targetOrder)
    }

    @Test
    fun import_keepsUuidsSoLaterImportsRecognizeThem() = runTest {
        val source = env()
        source.seed()
        val target = env(newDatabase())
        target.backup.importJson(source.backup.exportJson())

        assertEquals(source.accounts.list().map { it.uuid }.toSet(), target.accounts.list().map { it.uuid }.toSet())
    }

    @Test
    fun importedRecordsAreFullyUsable_suggestionsAndNameRulesWork() = runTest {
        val source = env()
        source.seed()
        val target = env(newDatabase())
        target.backup.importJson(source.backup.exportJson())
        val cash = target.accounts.list().first { it.name == "Efectivo" }

        assertEquals(listOf("Café"), target.entries.suggestSubjects(cash.id, "cafe"))
        assertEquals(listOf("Ñandú"), target.entries.suggestSubjects(cash.id, "ñan"))
        assertEquals(NameResult.Rejected(com.v4atu.bandtracker.domain.NameCheck.DUPLICATE), target.accounts.create("EFECTIVO", "COP"))
    }

    // --- Segundo import: idempotente ---

    @Test
    fun secondImportOfTheSameFile_addsNothingAndChangesNothing() = runTest {
        val source = env()
        source.seed()
        val json = source.backup.exportJson()
        val target = env(newDatabase())
        target.backup.importJson(json)
        val afterFirst = target.backup.exportJson()

        val summary = success(target.backup.importJson(json))

        assertEquals(0, summary.accountsAdded + summary.entriesAdded)
        assertEquals(2, summary.accountsSkipped)
        assertEquals(4, summary.entriesSkipped)
        assertEquals(afterFirst, target.backup.exportJson())
    }

    @Test
    fun importingIntoTheSameDatabaseItCameFrom_isANoOp() = runTest {
        val e = env()
        e.seed()
        val json = e.backup.exportJson()

        val summary = success(e.backup.importJson(json))

        assertEquals(0, summary.accountsAdded + summary.entriesAdded)
        assertEquals(json, e.backup.exportJson())
    }

    // --- Fusión: nunca borra ni sobrescribe ---

    @Test
    fun merge_addsOnlyMissingEntriesToAnExistingAccount_andNeverOverwrites() = runTest {
        val source = env()
        val cash = source.account("Efectivo")
        source.entry(cash, draft(amount = 1000, subject = "Original"))
        val json = source.backup.exportJson()

        // El mismo estado en el destino, editado localmente y con un registro propio.
        val target = env(newDatabase())
        target.backup.importJson(json)
        val targetCash = target.accounts.list().single()
        val localId = target.entries.page(targetCash.id, 0, 10).single().id
        target.entries.update(localId, draft(amount = 9999, subject = "Editado"))
        target.entry(targetCash.id, draft(amount = 7, subject = "Solo local"))

        // El archivo ahora trae además un registro nuevo.
        source.entry(cash, draft(amount = 2000, subject = "Nuevo"))
        val summary = success(target.backup.importJson(source.backup.exportJson()))

        assertEquals(0, summary.accountsAdded)
        assertEquals(1, summary.accountsSkipped)
        assertEquals(1, summary.entriesAdded)
        assertEquals(1, summary.entriesSkipped)
        val subjects = target.entries.page(targetCash.id, 0, 10).associate { it.subject to it.amountMinor }
        assertEquals(mapOf("Editado" to 9999L, "Solo local" to 7L, "Nuevo" to 2000L), subjects)
    }

    @Test
    fun merge_neverDeletesLocalOnlyAccountsOrEntries() = runTest {
        val target = env()
        val local = target.account("Solo local")
        target.entry(local, draft(subject = "Mío"))
        val source = env(newDatabase())
        source.seed()

        target.backup.importJson(source.backup.exportJson())

        assertEquals(setOf("Solo local", "Efectivo", "Ahorros"), target.accounts.list().map { it.name }.toSet())
        assertEquals(1, target.entries.page(local, 0, 10).size)
    }

    @Test
    fun entryWhoseUuidExistsUnderAnotherLocalAccount_isSkippedNotMoved() = runTest {
        val target = env()
        val a = target.account("A")
        target.entry(a, draft(subject = "Ya existe"))
        val uuid = target.entries.page(a, 0, 10).single().uuid
        val file = BackupFile(
            1,
            listOf(
                BackupAccount("nueva", "B", "COP", listOf(BackupEntry(uuid, "INCOME", 1, "2026-11-01", "Otro", null))),
            ),
        )

        val summary = success(target.backup.importJson(BackupCodec.encode(file)))

        assertEquals(1, summary.accountsAdded)
        assertEquals(0, summary.entriesAdded)
        assertEquals(1, summary.entriesSkipped)
        assertEquals("Ya existe", target.entries.page(a, 0, 10).single().subject)
    }

    // --- Colisión de nombres: se renombra la cuenta importada ---

    @Test
    fun nameCollisionWithAnotherLocalAccount_renamesTheImportedOne() = runTest {
        val target = env()
        target.account("Café")
        val file = BackupFile(1, listOf(BackupAccount("f1", "Cafe", "COP", emptyList())))

        val summary = success(target.backup.importJson(BackupCodec.encode(file)))

        assertEquals(listOf(AccountRename("Cafe", "Cafe (2)")), summary.renamedAccounts)
        assertEquals(setOf("Café", "Cafe (2)"), target.accounts.list().map { it.name }.toSet())
        assertEquals("f1", target.accounts.list().first { it.name == "Cafe (2)" }.uuid)
    }

    @Test
    fun renameUsesTheNextFreeNumber() = runTest {
        val target = env()
        target.account("Cafe")
        target.account("Cafe (2)")
        val file = BackupFile(1, listOf(BackupAccount("f1", "CAFÉ", "COP", emptyList())))

        val summary = success(target.backup.importJson(BackupCodec.encode(file)))

        assertEquals(listOf(AccountRename("CAFÉ", "CAFÉ (3)")), summary.renamedAccounts)
    }

    @Test
    fun twoNewAccountsInTheFileWithTheSameName_secondOneIsRenamed() = runTest {
        val target = env()
        val file = BackupFile(
            1,
            listOf(BackupAccount("f1", "Cafe", "COP", emptyList()), BackupAccount("f2", "Café", "USD", emptyList())),
        )

        val summary = success(target.backup.importJson(BackupCodec.encode(file)))

        assertEquals(listOf(AccountRename("Café", "Café (2)")), summary.renamedAccounts)
        assertEquals(setOf("Cafe", "Café (2)"), target.accounts.list().map { it.name }.toSet())
    }

    @Test
    fun renamedAccountIsRecognizedByUuidOnTheNextImport_noDuplicates() = runTest {
        val target = env()
        target.account("Café")
        val json = BackupCodec.encode(BackupFile(1, listOf(BackupAccount("f1", "Cafe", "COP", emptyList()))))
        target.backup.importJson(json)

        val summary = success(target.backup.importJson(json))

        assertEquals(0, summary.accountsAdded)
        assertEquals(1, summary.accountsSkipped)
        assertTrue(summary.renamedAccounts.isEmpty())
        assertEquals(2, target.accounts.list().size)
    }

    @Test
    fun accountWithTheSameUuidKeepsItsLocalNameEvenIfTheFileNameDiffers() = runTest {
        val target = env()
        target.account("Local")
        val uuid = target.accounts.list().single().uuid
        val file = BackupFile(1, listOf(BackupAccount(uuid, "Nombre del archivo", "COP", emptyList())))

        success(target.backup.importJson(BackupCodec.encode(file)))

        assertEquals("Local", target.accounts.list().single().name)
    }

    // --- Texto importado ---

    @Test
    fun blankSubjectAndNote_areStoredAsNullAndTextIsTrimmed() = runTest {
        val target = env()
        val file = BackupFile(
            1,
            listOf(
                BackupAccount(
                    "f1", "A", "COP",
                    listOf(
                        BackupEntry("x1", "INCOME", 1, "2026-11-01", "  ", ""),
                        BackupEntry("x2", "INCOME", 1, "2026-11-02", "  Taxi ", " ida "),
                    ),
                ),
            ),
        )

        target.backup.importJson(BackupCodec.encode(file))

        val rows = target.entries.page(target.accounts.list().single().id, 0, 10).associateBy { it.uuid }
        assertEquals(null, rows.getValue("x1").subject)
        assertEquals(null, rows.getValue("x1").note)
        assertEquals("Taxi", rows.getValue("x2").subject)
        assertEquals("ida", rows.getValue("x2").note)
    }

    // --- Rechazos: no cambian nada ---

    @Test
    fun invalidFiles_areRejectedWithAClearErrorAndNothingChanges() = runTest {
        val target = env()
        target.seed()
        val before = target.backup.exportJson()

        assertEquals(BackupError.NotValidJson, rejected(target.backup.importJson("no es json")))
        assertEquals(BackupError.UnsupportedVersion(2, 1), rejected(target.backup.importJson("""{"version":2,"accounts":[]}""")))
        val future = BackupFile(1, listOf(BackupAccount("n", "Nueva", "COP", listOf(BackupEntry("z", "INCOME", 1, "2026-11-08", null, null)))))
        assertEquals(
            BackupError.InvalidData("accounts[0].entries[0].date", InvalidReason.FUTURE_DATE),
            rejected(target.backup.importJson(BackupCodec.encode(future))),
        )

        assertEquals(before, target.backup.exportJson())
    }

    @Test
    fun aFutureDatedRecordAbortsTheWholeImport_evenWhenOtherDataIsValid() = runTest {
        val target = env()
        val file = BackupFile(
            1,
            listOf(
                BackupAccount("ok", "Valida", "COP", listOf(BackupEntry("e1", "INCOME", 1, "2026-11-01", null, null))),
                BackupAccount("mala", "Mala", "COP", listOf(BackupEntry("e2", "INCOME", 1, "2027-01-01", null, null))),
            ),
        )

        rejected(target.backup.importJson(BackupCodec.encode(file)))

        assertEquals(0, target.accounts.list().size)
    }

    @Test
    fun aFailureMidImport_rollsBackEverythingAlreadyWritten() = runTest {
        val target = env()
        target.account("Local", "COP")
        val localUuid = target.accounts.list().single().uuid
        val file = BackupFile(
            1,
            listOf(
                BackupAccount("nueva", "Nueva", "USD", listOf(BackupEntry("n1", "INCOME", 5, "2026-11-01", "x", null))),
                // Misma cuenta local pero con otra moneda: solo se detecta contra la base, ya con la primera escrita.
                BackupAccount(localUuid, "Local", "USD", emptyList()),
            ),
        )

        val error = rejected(target.backup.importJson(BackupCodec.encode(file)))

        assertEquals(BackupError.InvalidData("accounts[1].currency", InvalidReason.CURRENCY_MISMATCH), error)
        assertEquals(listOf("Local"), target.accounts.list().map { it.name })
        assertEquals(0, target.db.entryDao().count(target.accounts.list().single().id))
        assertEquals(null, target.db.accountDao().findByUuid("nueva"))
    }
}
