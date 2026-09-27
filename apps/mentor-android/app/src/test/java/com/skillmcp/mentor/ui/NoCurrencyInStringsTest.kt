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
}
