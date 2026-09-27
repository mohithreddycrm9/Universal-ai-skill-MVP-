package com.skillmcp.mentor.mentor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSnippetTest {
    @Test
    fun shortTextIsReturnedWhole() {
        assertEquals("Book the Goa train", SearchSnippet.around("Book the\nGoa   train", "goa"))
    }

    @Test
    fun longTextIsCutAroundTheMatchOnWordBoundaries() {
        val text = "alpha ".repeat(30) + "the Mysuru palace opens at ten " + "omega ".repeat(30)
        val snippet = SearchSnippet.around(text, "mysuru", radius = 20)
        assertTrue(snippet, snippet.startsWith("…") && snippet.endsWith("…"))
        assertTrue(snippet.contains("Mysuru palace"))
        assertTrue(snippet.removePrefix("…").removeSuffix("…").split(" ").all { it in setOf("alpha", "the", "Mysuru", "palace", "opens", "at", "ten", "omega") })
    }

    @Test
    fun onePerConversationKeepsTheFirstHit() {
        val hits =
            listOf(
                MessageSearchHit("c1", "Trip", "m2", "newer"),
                MessageSearchHit("c1", "Trip", "m1", "older"),
                MessageSearchHit("c2", "Food", "m3", "x"),
            )
        assertEquals(listOf("m2", "m3"), SearchSnippet.onePerConversation(hits).map { it.messageId })
    }
}
