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
        val staticTextLine = Regex("Text\\s*\\(\\s*\"[^\"\\$]*\"")
        files.forEach { file ->
            file.readLines().forEach { line ->
                if (!line.contains("Text(\"")) return@forEach
                if (line.contains("\${")) return@forEach
                if (!staticTextLine.containsMatchIn(line)) return@forEach
                assertFalse(
                    "${file.path} contains currency in static Text literal: $line",
                    line.contains('$') || line.contains('₹') || line.contains('\u20B9'),
                )
            }
        }
    }
}
