package com.skillmcp.mentor.mentor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalizationPromptTest {
    @Test
    fun emptyPersonalizationReturnsBasePrompt() {
        assertEquals("Base.", PersonalizationPrompt.compose(" Base. ", "", "", "", enabled = true))
    }

    @Test
    fun includesNameAboutAndResponseSectionsInOrder() {
        val out = PersonalizationPrompt.compose("Base.", "Asha", "Teacher in Pune.", "Be concise", enabled = true)
        assertEquals(
            "Base.\n\nThe user's name is Asha.\n\n" +
                "What the user wants you to know about them:\nTeacher in Pune.\n\n" +
                "How the user wants you to respond:\nBe concise",
            out,
        )
    }

    @Test
    fun disabledSendsOnlyTheBasePrompt() {
        val out = PersonalizationPrompt.compose("Base.", "Asha", "Teacher", "Be concise", enabled = false)
        assertEquals("Base.", out)
        assertFalse(out.contains("Asha"))
    }

    @Test
    fun fieldsAreCapped() {
        val long = "x".repeat(5000)
        val out = PersonalizationPrompt.compose("", "", long, "", enabled = true)
        assertTrue(out.length < PersonalizationPrompt.MAX_FIELD_CHARS + 60)
    }

    @Test
    fun appendTraitAddsOnceOnItsOwnLine() {
        val once = PersonalizationPrompt.appendTrait("Use metric units", "Be concise")
        assertEquals("Use metric units\nBe concise", once)
        assertEquals(once, PersonalizationPrompt.appendTrait(once, "be concise"))
        assertEquals("Be concise", PersonalizationPrompt.appendTrait("", "Be concise"))
    }
}
