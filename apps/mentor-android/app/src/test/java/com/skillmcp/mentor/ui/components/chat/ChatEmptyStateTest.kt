package com.skillmcp.mentor.ui.components.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChatEmptyStateTest {
    @Test
    fun greetingWithoutNameOmitsThere() {
        val greeting = timeOfDayGreeting("")
        assertFalse(greeting.contains("there"))
        assertEquals(true, greeting.endsWith("👋"))
    }
}
