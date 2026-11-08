package com.v4atu.bandtracker.backup

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC §7: formato JSON versionado y rechazo claro de versión mayor o datos inválidos. */
class BackupCodecTest {

    private val today = LocalDate.of(2026, 11, 7)

    private fun entry(
        uuid: String = "e1",
        type: String = "INCOME",
        amountMinor: Long = 1000,
        date: String = "2026-11-07",
        subject: String? = "Taxi",
        note: String? = "ida",
    ) = BackupEntry(uuid, type, amountMinor, date, subject, note)

    private fun account(
        uuid: String = "a1",
        name: String = "Efectivo",
        currency: String = "COP",
        vararg entries: BackupEntry,
    ) = BackupAccount(uuid, name, currency, entries.toList())

    private fun file(vararg accounts: BackupAccount) = BackupFile(BackupCodec.CURRENT_VERSION, accounts.toList())

    private fun failure(json: String): BackupError =
        (BackupCodec.decode(json, today) as DecodeResult.Failed).error

    private fun invalid(json: String): BackupError.InvalidData = failure(json) as BackupError.InvalidData

    /** JSON mínimo válido con un único registro, sobrescribiendo un campo del registro. */
    private fun jsonWithEntry(entryJson: String) =
        """{"version":1,"accounts":[{"uuid":"a1","name":"A","currency":"COP","entries":[$entryJson]}]}"""

    private fun entryJson(
        uuid: String = "\"e1\"",
        type: String = "\"INCOME\"",
        amount: String = "1000",
        date: String = "\"2026-11-07\"",
    ) = """{"uuid":$uuid,"type":$type,"amountMinor":$amount,"date":$date}"""

    // --- Codificación ---

    @Test
    fun roundTrip_preservesEverything() {
        val original = file(
            account("a1", "Efectivo", "COP", entry("e1"), entry("e2", "EXPENSE", 2500, "2026-11-01", null, null)),
            account("a2", "Ahorros ñandú", "USD"),
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(original), today)

        assertEquals(DecodeResult.Valid(original), decoded)
    }

    @Test
    fun encode_writesVersionIsoDateMinorUnitsAsNumberAndExplicitNulls() {
        val json = BackupCodec.encode(file(account("a1", "A", "USD", entry(subject = null, note = null))))

        assertTrue(json, json.contains("\"version\": 1"))
        assertTrue(json, json.contains("\"date\": \"2026-11-07\""))
        assertTrue(json, json.contains("\"amountMinor\": 1000"))
        assertTrue(json, json.contains("\"subject\": null"))
    }

    // --- JSON y estructura ---

    @Test
    fun malformedJson_isNotValidJson() {
        assertEquals(BackupError.NotValidJson, failure(""))
        assertEquals(BackupError.NotValidJson, failure("{"))
        assertEquals(BackupError.NotValidJson, failure("esto no es json"))
    }

    @Test
    fun topLevelThatIsNotAnObject_isInvalidData() {
        assertEquals(InvalidReason.STRUCTURE, invalid("[]").reason)
        assertEquals(InvalidReason.STRUCTURE, invalid("123").reason)
    }

    @Test
    fun missingRequiredField_isInvalidStructure() {
        val json = """{"version":1,"accounts":[{"uuid":"a1","currency":"COP"}]}"""

        assertEquals(InvalidReason.STRUCTURE, invalid(json).reason)
    }

    @Test
    fun unknownKeys_areRejectedNotIgnored() {
        val json = """{"version":1,"accounts":[],"extra":true}"""

        assertEquals(InvalidReason.STRUCTURE, invalid(json).reason)
    }

    @Test
    fun amountThatIsNotAnInteger_isInvalidStructure() {
        assertEquals(InvalidReason.STRUCTURE, invalid(jsonWithEntry(entryJson(amount = "1.5"))).reason)
        assertEquals(InvalidReason.STRUCTURE, invalid(jsonWithEntry(entryJson(amount = "\"abc\""))).reason)
        assertEquals(InvalidReason.STRUCTURE, invalid(jsonWithEntry(entryJson(amount = "null"))).reason)
    }

    // kotlinx-serialization acepta enteros exactos entre comillas aunque no esté en modo lenient
    // (documentado en json.md); es un dato válido y no justifica un serializador propio.
    @Test
    fun amountAsQuotedExactInteger_isAcceptedByTheLibrary() {
        assertTrue(BackupCodec.decode(jsonWithEntry(entryJson(amount = "\"1000\"")), today) is DecodeResult.Valid)
    }

    // --- Versión ---

    @Test
    fun missingOrNonNumericVersion_isInvalidAtVersion() {
        assertEquals("version", invalid("""{"accounts":[]}""").location)
        assertEquals("version", invalid("""{"version":"1","accounts":[]}""").location)
        assertEquals("version", invalid("""{"version":1.5,"accounts":[]}""").location)
        assertEquals("version", invalid("""{"version":null,"accounts":[]}""").location)
    }

    @Test
    fun versionBelowOne_isInvalid() {
        assertEquals("version", invalid("""{"version":0,"accounts":[]}""").location)
        assertEquals("version", invalid("""{"version":-3,"accounts":[]}""").location)
    }

    @Test
    fun newerVersion_isRejectedBeforeLookingAtTheRestOfTheShape() {
        // Una versión futura puede tener una estructura distinta: no debe fallar como "inválido".
        assertEquals(BackupError.UnsupportedVersion(found = 2, supported = 1), failure("""{"version":2,"accounts":"otra forma"}"""))
        assertEquals(BackupError.UnsupportedVersion(found = 99, supported = 1), failure("""{"version":99}"""))
    }

    @Test
    fun currentVersionIsAccepted() {
        assertEquals(DecodeResult.Valid(file()), BackupCodec.decode("""{"version":1,"accounts":[]}""", today))
    }

    // --- Validación de datos, con la ubicación del campo ---

    @Test
    fun blankAccountName_isRejected() {
        val result = invalid(BackupCodec.encode(file(account(name = "   "))))

        assertEquals("accounts[0].name", result.location)
        assertEquals(InvalidReason.BLANK, result.reason)
    }

    @Test
    fun blankUuids_areRejected() {
        assertEquals("accounts[0].uuid", invalid(BackupCodec.encode(file(account(uuid = " ")))).location)
        assertEquals(
            "accounts[0].entries[0].uuid",
            invalid(BackupCodec.encode(file(account("a1", "A", "COP", entry(uuid = ""))))).location,
        )
    }

    @Test
    fun unknownCurrency_isRejected() {
        val bad = invalid(BackupCodec.encode(file(account(currency = "ZZZ"))))
        assertEquals("accounts[0].currency", bad.location)
        assertEquals(InvalidReason.UNKNOWN_CURRENCY, bad.reason)

        assertEquals(InvalidReason.UNKNOWN_CURRENCY, invalid(BackupCodec.encode(file(account(currency = "cop")))).reason)
    }

    @Test
    fun unknownEntryType_isRejected() {
        val bad = invalid(jsonWithEntry(entryJson(type = "\"TRANSFER\"")))
        assertEquals("accounts[0].entries[0].type", bad.location)
        assertEquals(InvalidReason.UNKNOWN_TYPE, bad.reason)
    }

    @Test
    fun zeroOrNegativeAmount_isRejected() {
        assertEquals(InvalidReason.NON_POSITIVE_AMOUNT, invalid(jsonWithEntry(entryJson(amount = "0"))).reason)
        val negative = invalid(jsonWithEntry(entryJson(amount = "-5")))
        assertEquals(InvalidReason.NON_POSITIVE_AMOUNT, negative.reason)
        assertEquals("accounts[0].entries[0].amountMinor", negative.location)
    }

    @Test
    fun maxLongAmount_isAccepted() {
        val json = jsonWithEntry(entryJson(amount = Long.MAX_VALUE.toString()))

        assertTrue(BackupCodec.decode(json, today) is DecodeResult.Valid)
    }

    @Test
    fun badDates_areRejected() {
        for (date in listOf("\"2026-13-45\"", "\"2026-1-5\"", "\"07/11/2026\"", "\"\"", "\"2026-02-30\"")) {
            val bad = invalid(jsonWithEntry(entryJson(date = date)))
            assertEquals(date, InvalidReason.BAD_DATE, bad.reason)
            assertEquals("accounts[0].entries[0].date", bad.location)
        }
    }

    @Test
    fun futureDate_isRejectedButTodayIsAccepted() {
        val future = invalid(jsonWithEntry(entryJson(date = "\"2026-11-08\"")))
        assertEquals(InvalidReason.FUTURE_DATE, future.reason)
        assertEquals("accounts[0].entries[0].date", future.location)

        assertTrue(BackupCodec.decode(jsonWithEntry(entryJson(date = "\"2026-11-07\"")), today) is DecodeResult.Valid)
    }

    @Test
    fun duplicateAccountUuids_areRejected() {
        val bad = invalid(BackupCodec.encode(file(account("a1", "A"), account("a1", "B"))))

        assertEquals(InvalidReason.DUPLICATE_UUID, bad.reason)
        assertEquals("accounts[1].uuid", bad.location)
    }

    @Test
    fun duplicateEntryUuids_areRejectedEvenAcrossAccounts() {
        val bad = invalid(
            BackupCodec.encode(file(account("a1", "A", "COP", entry("dup")), account("a2", "B", "COP", entry("dup")))),
        )

        assertEquals(InvalidReason.DUPLICATE_UUID, bad.reason)
        assertEquals("accounts[1].entries[0].uuid", bad.location)
    }

    @Test
    fun blankSubjectAndNote_areAllowed() {
        val json = BackupCodec.encode(file(account("a1", "A", "COP", entry(subject = "  ", note = ""))))

        assertTrue(BackupCodec.decode(json, today) is DecodeResult.Valid)
    }

    @Test
    fun firstProblemIsReported_inFileOrder() {
        val bad = invalid(
            BackupCodec.encode(
                file(
                    account("a1", "A", "COP", entry("e1", amountMinor = 0)),
                    account("a2", "B", "ZZZ"),
                ),
            ),
        )

        assertEquals("accounts[0].entries[0].amountMinor", bad.location)
    }
}
