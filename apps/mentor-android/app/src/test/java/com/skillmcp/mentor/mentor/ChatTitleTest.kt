package com.skillmcp.mentor.mentor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTitleTest {
    @Test
    fun shortFirstLineBecomesTheTitle() {
        assertEquals("Plan meals for the week", ChatTitle.fromFirstMessage("  Plan meals for the week.\nThanks"))
    }

    @Test
    fun longTextIsCutAtAWordWithEllipsis() {
        val title = ChatTitle.fromFirstMessage("Explain how photosynthesis works in plants and why leaves change colour")!!
        assertTrue(title.endsWith("…"))
        assertTrue(title.length <= ChatTitle.MAX_CHARS + 1)
        assertFalse(title.contains("colour"))
    }

    @Test
    fun quotedReplyPrefixAndBlankLinesAreSkipped() {
        assertEquals("What about Goa", ChatTitle.fromFirstMessage("\n\n> What about Goa?".replace("?", "")))
        assertNull(ChatTitle.fromFirstMessage("   \n  "))
    }

    @Test
    fun placeholderNamesAreRecognised() {
        listOf("New chat", "General", "Chat 482").forEach { assertTrue(it, ChatTitle.isPlaceholder(it)) }
        assertFalse(ChatTitle.isPlaceholder("Trip to Goa"))
    }
}
