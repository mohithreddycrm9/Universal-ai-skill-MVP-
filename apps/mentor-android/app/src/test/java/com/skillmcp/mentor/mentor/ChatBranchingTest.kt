package com.skillmcp.mentor.mentor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatBranchingTest {
    private val thread =
        listOf(
            UiMessage("u1", "user", "Plan a trip"),
            UiMessage("a1", "assistant", "Where to?"),
            UiMessage("u2", "user", "Goa, 3 days"),
            UiMessage("a2", "assistant", "Day 1: beaches"),
        )

    @Test
    fun editingAUserMessageDropsItAndEverythingAfter() {
        assertEquals(listOf("u2", "a2"), ChatBranching.idsToDropForEdit(thread, "u2"))
        assertEquals(listOf("u1", "a1", "u2", "a2"), ChatBranching.idsToDropForEdit(thread, "u1"))
    }

    @Test
    fun onlyUserMessagesCanBeEdited() {
        assertEquals(emptyList<String>(), ChatBranching.idsToDropForEdit(thread, "a1"))
        assertEquals(emptyList<String>(), ChatBranching.idsToDropForEdit(thread, "missing"))
    }

    @Test
    fun regenerateUsesHistoryBeforeTheLastUserTurn() {
        val ctx = ChatBranching.regenerateContext(thread, "a2")!!
        assertEquals("u2", ctx.userMessage.id)
        assertEquals(listOf("u1", "a1"), ctx.history.map { it.id })
        assertEquals("a2", ctx.reply.id)
    }

    @Test
    fun onlyTheLatestReplyCanBeRegenerated() {
        assertNull(ChatBranching.regenerateContext(thread, "a1"))
        assertNull(ChatBranching.regenerateContext(thread.dropLast(1), "u2"))
        assertNull(ChatBranching.regenerateContext(listOf(UiMessage("a0", "assistant", "hi")), "a0"))
    }

    @Test
    fun versionsKeepTheOriginalThenAppend() {
        val first = ChatBranching.versionsAfterRegenerate(emptyList(), "v1", "v2")
        assertEquals(listOf("v1", "v2"), first)
        assertEquals(listOf("v1", "v2", "v3"), ChatBranching.versionsAfterRegenerate(first, "v2", "v3"))
    }

    @Test
    fun visibleIndexFollowsTheShownContent() {
        val versions = listOf("v1", "v2", "v3")
        assertEquals(0, ChatBranching.visibleVersionIndex(versions, "v1"))
        assertEquals(2, ChatBranching.visibleVersionIndex(versions, "edited elsewhere"))
        assertEquals(0, ChatBranching.visibleVersionIndex(emptyList(), "x"))
    }

    @Test
    fun switchingModelThenRegeneratingRetriesTheLastReply() {
        val withSwitch = thread + UiMessage("s1", com.skillmcp.mentor.llm.ROLE_MODEL_SWITCH, "llama3.2")
        val ctx = ChatBranching.regenerateContext(withSwitch, thread.last().id)
        org.junit.Assert.assertNotNull(ctx)
        org.junit.Assert.assertEquals(thread.last().id, ctx!!.reply.id)
    }
}
