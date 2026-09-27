package com.skillmcp.mentor.mentor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowUpSuggestionsTest {
    @Test
    fun codeRepliesOfferExplainAndTests() {
        val kinds = FollowUpSuggestions.forReply("Here:\n```kotlin\nfun a() = 1\n```")
        assertEquals(listOf(FollowUpKind.EXPLAIN_CODE, FollowUpKind.ADD_TESTS, FollowUpKind.NEXT_STEPS), kinds)
    }

    @Test
    fun listRepliesOfferChecklistAndTable() {
        val kinds = FollowUpSuggestions.forReply("Options:\n- Train\n- Bus\n1. Flight")
        assertEquals(listOf(FollowUpKind.CHECKLIST, FollowUpKind.TABLE, FollowUpKind.NEXT_STEPS), kinds)
    }

    @Test
    fun longRepliesLeadWithShorter() {
        val kinds = FollowUpSuggestions.forReply("word ".repeat(300))
        assertEquals(FollowUpKind.SHORTER, kinds.first())
        assertEquals(3, kinds.size)
    }

    @Test
    fun questionsErrorsAndEmptyRepliesGetNoChips() {
        assertTrue(FollowUpSuggestions.forReply("Which city are you in?").isEmpty())
        assertTrue(FollowUpSuggestions.forReply("⚠️ Check your API key").isEmpty())
        assertTrue(FollowUpSuggestions.forReply("  ").isEmpty())
    }

    @Test
    fun plainRepliesOfferExampleAndSimpler() {
        assertEquals(
            listOf(FollowUpKind.EXAMPLE, FollowUpKind.SIMPLER, FollowUpKind.NEXT_STEPS),
            FollowUpSuggestions.forReply("Photosynthesis turns light into chemical energy."),
        )
    }
}
