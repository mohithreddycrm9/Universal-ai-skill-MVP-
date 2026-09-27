package com.skillmcp.mentor.mentor

import org.junit.Assert.assertTrue
import org.junit.Test

class BuildSuggestionEngineTest {
    private val engine = BuildSuggestionEngine()

    @Test
    fun generatesChipWhenTestFailed() {
        val suggestions =
            engine.compute(
                BuildSuggestionsInput(
                    goal = "Ship Android mentor app",
                    recentEvents = listOf(AgentEventHint("test_failed")),
                ),
            )
        assertTrue(suggestions.any { it.because == "test_failed" })
    }

    @Test
    fun alwaysOffersSuggestionsOnEmptyChat() {
        val suggestions = engine.compute(BuildSuggestionsInput(messageCount = 0))
        assertTrue(suggestions.size >= 4)
        assertTrue(suggestions.any { it.because == "hero" || it.because == "onboarding" })
    }

    @Test
    fun offersFollowupsWhenChatHasMessages() {
        val suggestions = engine.compute(BuildSuggestionsInput(messageCount = 3))
        assertTrue(suggestions.any { it.because == "followup" || it.because == "learn" })
    }
}
