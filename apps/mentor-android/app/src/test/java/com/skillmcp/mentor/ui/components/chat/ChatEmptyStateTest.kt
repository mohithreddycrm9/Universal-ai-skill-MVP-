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
}
