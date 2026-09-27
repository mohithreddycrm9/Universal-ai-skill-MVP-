package com.skillmcp.mentor.ui

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class NoCurrencyInStringsTest {
    @Test
    fun stringResourcesDoNotContainCurrencySymbols() {
        val resDir = File("src/main/res")
        val stringFiles =
            resDir
                .walkTopDown()
                .filter { it.isFile && it.name == "strings.xml" }
                .toList()
        assertFalse("No strings.xml found under res/", stringFiles.isEmpty())
        stringFiles.forEach { file ->
            val text = file.readText()
            assertFalse("${file.path} contains dollar sign", text.contains('$'))
            assertFalse("${file.path} contains rupee sign", text.contains('₹') || text.contains("\u20B9"))
        }
    }

    @Test
    fun composeUiKotlinDoesNotContainCurrencySymbolsInUserVisibleLiterals() {
        val uiDir = File("src/main/java/com/skillmcp/mentor/ui")
        val files =
            uiDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .toList()
        assertFalse("No UI kotlin files found", files.isEmpty())
        val bannedInUi =
            listOf(
                Regex("""\$\d"""),
                Regex("""(?i)\bUSD\b"""),
                Regex("""(?i)\bprice\b"""),
                Regex("""(?i)\bspend\b"""),
                Regex("""(?i)\bcost\b"""),
            )
        val stringLiteral = Regex(""""(?:[^"\\]|\\.)*"""")
        files.forEach { file ->
            file.readLines().forEach { line ->
                if (line.trimStart().startsWith("//")) return@forEach
                stringLiteral.findAll(line).forEach { match ->
                    val literal = match.value
                    if (literal.contains('$')) return@forEach
                    if (literal.contains('₹') || literal.contains('\u20B9')) {
                        assertFalse("${file.path} contains currency symbol in: $line", true)
                    }
                    bannedInUi.forEach { pattern ->
                        assertFalse(
                            "${file.path} contains banned pricing copy ($pattern): $line",
                            pattern.containsMatchIn(literal),
                        )
                    }
                }
            }
        }
    }
}
