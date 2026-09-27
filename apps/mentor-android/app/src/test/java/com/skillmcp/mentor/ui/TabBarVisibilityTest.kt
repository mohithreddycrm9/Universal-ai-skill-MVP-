package com.skillmcp.mentor.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabBarVisibilityTest {
    @Test
    fun hiddenInsideAConversation() {
        assertTrue(isInConversation("chat", hasMessages = true, isSending = false))
        assertTrue(isInConversation("chat", hasMessages = false, isSending = true))
    }

    @Test
    fun shownOnEmptyChatAndOtherTabs() {
        assertFalse(isInConversation("chat", hasMessages = false, isSending = false))
        assertFalse(isInConversation("discover", hasMessages = true, isSending = true))
        assertFalse(isInConversation("settings", hasMessages = true, isSending = false))
    }
}
