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
}
