package com.skillmcp.mentor.mentor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacySeededPromptsTest {
    @Test
    fun exactSeedIsRemovable() {
        assertTrue(LegacySeededPrompts.isUntouchedSeed("Brainstorm", "Brainstorm 10 creative ideas for a goal I describe."))
    }

    @Test
    fun editedOrUserPromptsAreKept() {
        assertFalse(LegacySeededPrompts.isUntouchedSeed("Brainstorm", "Brainstorm 5 ideas for my bakery."))
        assertFalse(LegacySeededPrompts.isUntouchedSeed("My prompt", "Explain simply"))
    }
}
