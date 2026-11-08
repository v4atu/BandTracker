package com.v4atu.bandtracker.licenses

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * SPEC §6: español (por defecto) e inglés, y todo texto vive en recursos de strings. Vigila que las
 * dos traducciones tengan las mismas claves y los mismos marcadores de formato, y que ninguna pantalla
 * escriba texto en el código.
 */
class StringsParityTest {

    private val root: File = generateSequence(File(System.getProperty("user.dir")!!).absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() }

    private class Resources(val strings: Map<String, String>, val plurals: Map<String, Map<String, String>>)

    private fun load(path: String): Resources {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(root, path))
        val strings = linkedMapOf<String, String>()
        val plurals = linkedMapOf<String, Map<String, String>>()
        val children = doc.documentElement.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i) as? Element ?: continue
            when (node.tagName) {
                "string" -> strings[node.getAttribute("name")] = node.textContent
                "plurals" -> {
                    val items = node.getElementsByTagName("item")
                    plurals[node.getAttribute("name")] =
                        (0 until items.length).associate { (items.item(it) as Element).getAttribute("quantity") to items.item(it).textContent }
                }
            }
        }
        return Resources(strings, plurals)
    }

    private val es = load("app/src/main/res/values/strings.xml")
    private val en = load("app/src/main/res/values-en/strings.xml")

    private fun placeholders(text: String) = Regex("%(\\d+)\\$[sd]").findAll(text).map { it.value }.sorted().toList()

    @Test
    fun bothLanguagesHaveTheSameStrings_andTheSamePlurals() {
        assertEquals("strings que faltan en inglés o sobran", es.strings.keys.sorted(), en.strings.keys.sorted())
        assertEquals("plurales que faltan en inglés o sobran", es.plurals.keys.sorted(), en.plurals.keys.sorted())
    }

    @Test
    fun everyTextIsFilledIn_inBothLanguages() {
        (es.strings + en.strings).forEach { (name, text) -> assertTrue("«$name» está vacío", text.isNotBlank()) }
        listOf(es, en).forEach { lang ->
            lang.plurals.forEach { (name, items) -> items.forEach { (q, text) -> assertTrue("«$name/$q» está vacío", text.isNotBlank()) } }
        }
    }

    @Test
    fun translationsUseTheSameFormatPlaceholders() {
        es.strings.forEach { (name, text) ->
            assertEquals("marcadores de «$name»", placeholders(text), placeholders(en.strings.getValue(name)))
        }
        es.plurals.forEach { (name, items) ->
            items.forEach { (quantity, text) ->
                val other = en.plurals.getValue(name)[quantity] ?: return@forEach
                assertEquals("marcadores de «$name/$quantity»", placeholders(text), placeholders(other))
            }
        }
    }

    @Test
    fun everyPluralHasAtLeastOneAndOther_inBothLanguages() {
        listOf(es, en).forEach { lang ->
            lang.plurals.forEach { (name, items) ->
                assertTrue("«$name» sin 'one'", "one" in items)
                assertTrue("«$name» sin 'other'", "other" in items)
            }
        }
    }

    @Test
    fun noScreenWritesUserFacingTextInCode() {
        // Todo texto vive en recursos (SPEC §6): ni `text = "..."` ni `Text("...")` en la interfaz.
        val offenders = File(root, "app/src/main/java/com/v4atu/bandtracker/ui").walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                file.readLines().withIndex().filter { (_, line) ->
                    val code = line.substringBefore("//")
                    Regex("""\btext\s*=\s*"[^"]*\p{L}""").containsMatchIn(code) ||
                        Regex("""\b(BandText|Text)\(\s*"[^"]*\p{L}""").containsMatchIn(code)
                }.map { "${file.name}:${it.index + 1}: ${it.value.trim()}" }
            }.toList()

        assertEquals("textos fijos en el código:\n" + offenders.joinToString("\n"), emptyList<String>(), offenders)
    }
}
