package com.skillmcp.mentor.ui.components.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChatEmptyStateTest {
    @Test
    fun greetingUsesHelpPrompt() {
        val greeting = timeOfDayGreeting("Mohith")
        assertEquals("Hi Mohith, what can I help with?", greeting)
    }

    @Test
    fun greetingWithoutNameOmitsThere() {
        assertEquals("What can I help with?", timeOfDayGreeting(""))
        assertEquals("What can I help with?", timeOfDayGreeting("   "))
        assertFalse(timeOfDayGreeting("").contains("there", ignoreCase = true))
    }
}
