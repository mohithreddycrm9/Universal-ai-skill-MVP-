package com.skillmcp.mentor.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * No LLM cost, price or currency may be shown anywhere in the UI.
 * Scans every Kotlin file under ui/ plus every strings.xml (all locales). [PricingCopyScanner] is
 * tested on its own so the guard is proven able to fail.
 */
class NoCurrencyInStringsTest {
    @Test
    fun scannerFlagsPricingCopy() {
        listOf(
            "Costs \$5 per month", "₹10", "€2", "£3", "12 USD", "INR 40", "Estimated cost", "Your spend today",
            "Price per token", "¥100",
        ).forEach { assertTrue(it, PricingCopyScanner.violations(it).isNotEmpty()) }
    }

    @Test
    fun scannerIgnoresTemplatesAndNeutralCopy() {
        listOf(
            "Hi \${name}, what can I help with?", "Messages sent: \$count", "Daily message cap",
            "Budget-friendly meal plan", "Customize", "Costume ideas", "\$it",
        ).forEach { assertEquals(it, emptyList<String>(), PricingCopyScanner.violations(it)) }
    }

    @Test
    fun stringResourcesContainNoPricingCopy() {
        val files =
            File("src/main/res").walkTopDown().filter { it.isFile && it.name == "strings.xml" }.toList()
        assertTrue("No strings.xml found under res/", files.size >= 4)
        val stringValue = Regex("""<string[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        val problems =
            files.flatMap { file ->
                stringValue.findAll(file.readText()).flatMap { m ->
                    PricingCopyScanner.violations(m.groupValues[1]).map { "${file.path}: ${m.groupValues[1]} ($it)" }
                }
            }
        assertEquals(problems.joinToString("\n"), 0, problems.size)
    }

    @Test
    fun everyUiKotlinFileContainsNoPricingCopy() {
        val files =
            File("src/main/java/com/skillmcp/mentor/ui").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("Expected many UI files, found ${files.size}", files.size > 40)
        val problems = mutableListOf<String>()
        files.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trimStart()
                if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) return@forEachIndexed
                PricingCopyScanner.stringLiterals(line).forEach { literal ->
                    PricingCopyScanner.violations(literal).forEach { problems += "${file.path}:${index + 1}: $literal ($it)" }
                }
            }
        }
        assertEquals(problems.joinToString("\n"), 0, problems.size)
    }
}

object PricingCopyScanner {
    private val literal = Regex(""""(?:[^"\\]|\\.)*"""")

    /** Kotlin string templates (${...} and $identifier) are code, not copy. */
    private val template = Regex("""\$\{[^}]*}|\$[A-Za-z_][A-Za-z0-9_]*""")

    private val rules =
        listOf(
            "currency symbol" to Regex("""[₹€£¥]|\$\s*\d"""),
            "currency code" to Regex("""(?i)\b(USD|INR|EUR|GBP)\b"""),
            "pricing word" to Regex("""(?i)\b(price|prices|pricing|cost|costs|spend|spent|spending|billing|billed)\b"""),
        )

    fun stringLiterals(line: String): List<String> =
        literal.findAll(line).map { it.value.removeSurrounding("\"") }.toList()

    fun violations(text: String): List<String> {
        val copy = text.replace("\\$", "$").replace(template, " ")
        return rules.filter { (_, regex) -> regex.containsMatchIn(copy) }.map { it.first }
    }
}
