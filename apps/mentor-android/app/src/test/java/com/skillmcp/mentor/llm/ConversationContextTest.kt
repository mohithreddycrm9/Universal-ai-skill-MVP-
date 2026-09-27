package com.skillmcp.mentor.llm

import com.skillmcp.mentor.mentor.ChatMessageDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationContextTest {
    private fun profile(kind: LlmProviderKind, model: String) = LlmProfile("p", "P", kind, "http://x/", model)

    @Test
    fun neutralHistoryKeepsRepliesFromEveryModelAndDropsDividersAndFailures() {
        val turns =
            listOf(
                StoredTurn("user", "Plan a trip"),
                StoredTurn("assistant", "Day 1… (written by gpt-4o-mini)"),
                StoredTurn(ROLE_MODEL_SWITCH, "llama3.2"),
                StoredTurn("user", "Make it cheaper"),
                StoredTurn("assistant", "⚠️ Network error"),
                StoredTurn("user", "Hello?"),
                StoredTurn("assistant", "Cheaper plan (written by llama3.2)"),
            )
        val h = ConversationContext.neutralHistory(turns)
        assertEquals(listOf("user", "assistant", "user", "assistant"), h.map { it.role })
        assertEquals("Day 1… (written by gpt-4o-mini)", h[1].content)
        // The failed reply is dropped and the two unanswered user turns are merged (strict alternation).
        assertEquals("Make it cheaper\n\nHello?", h[2].content)
        assertTrue(h.none { it.content == "llama3.2" })
    }

    @Test
    fun attachmentTextTravelsWithTheUserTurn() {
        val h =
            ConversationContext.neutralHistory(
                listOf(StoredTurn("user", "Summarise", attachmentText = "--- PDF text ---\nQ3 revenue grew"), StoredTurn("assistant", "Revenue grew")),
            )
        assertEquals("Summarise\n\n--- PDF text ---\nQ3 revenue grew", h[0].content)
    }

    @Test
    fun historyNeverStartsWithAnAssistantTurn() {
        val h = ConversationContext.neutralHistory(listOf(StoredTurn("assistant", "Hi!"), StoredTurn("user", "Hey")))
        assertEquals(listOf("user"), h.map { it.role })
    }

    @Test
    fun wireRolesPerProvider() {
        assertEquals("model", ConversationContext.wireRole(LlmProviderKind.GEMINI, "assistant"))
        assertEquals("assistant", ConversationContext.wireRole(LlmProviderKind.ANTHROPIC, "assistant"))
        assertEquals("assistant", ConversationContext.wireRole(LlmProviderKind.OLLAMA, "assistant"))
        assertEquals("user", ConversationContext.wireRole(LlmProviderKind.GEMINI, "user"))
    }

    @Test
    fun trimmingDropsOldestTurnsAndKeepsTheLatest() {
        val history = (1..40).flatMap { listOf(ChatMessageDto("user", "q$it " + "x".repeat(400)), ChatMessageDto("assistant", "a$it " + "y".repeat(400))) }
        val t = ConversationContext.trimToWindow("system", history, "latest question", windowTokens = 4_096)
        assertTrue(t.droppedCount > 0)
        assertEquals(history.last(), t.history.last())
        assertEquals("user", t.history.first().role)
        val used = t.history.sumOf { ConversationContext.estimateTokens(it.content) }
        assertTrue("fits in window minus reply reserve", used <= 4_096 - 1_024)
    }

    @Test
    fun smallHistoryIsSentWhole() {
        val history = listOf(ChatMessageDto("user", "hi"), ChatMessageDto("assistant", "hello"))
        val t = ConversationContext.trimToWindow("sys", history, "next", windowTokens = 128_000)
        assertEquals(history, t.history)
        assertEquals(0, t.droppedCount)
    }

    @Test
    fun hugeSystemPromptLeavesNoRoomForHistoryButTheUserMessageStillGoes() {
        val history = listOf(ChatMessageDto("user", "hi"), ChatMessageDto("assistant", "hello"))
        val t = ConversationContext.trimToWindow("s".repeat(20_000), history, "next", windowTokens = 4_096)
        assertTrue(t.history.isEmpty())
        assertEquals(2, t.droppedCount)
    }

    @Test
    fun switchingToASmallLocalModelTrimsWhileALargeCloudModelKeepsEverything() {
        val turns = (1..30).flatMap { listOf(StoredTurn("user", "question $it " + "z".repeat(600)), StoredTurn("assistant", "answer $it " + "w".repeat(600))) }
        val local = ConversationContext.forModel(profile(LlmProviderKind.OLLAMA, "llama3.2"), "sys", turns, "go on")
        val cloud = ConversationContext.forModel(profile(LlmProviderKind.ANTHROPIC, "claude-3-5-sonnet"), "sys", turns, "go on")
        assertEquals(60, cloud.history.size)
        assertTrue(local.history.size < cloud.history.size)
        assertTrue(local.history.last().content.startsWith("answer 30"))
    }

    @Test
    fun unansweredUserTurnIsFoldedIntoTheOutgoingMessage() {
        val turns = listOf(StoredTurn("user", "first"), StoredTurn("assistant", "ok"), StoredTurn("user", "lost"), StoredTurn("assistant", "⚠️ timeout"))
        val t = ConversationContext.forModel(profile(LlmProviderKind.GEMINI, "gemini-2.0-flash"), "sys", turns, "retry")
        assertEquals(listOf("user", "assistant"), t.history.map { it.role })
        assertEquals("lost\n\nretry", t.userMessage)
    }

    @Test
    fun contextWindowsAreConservative() {
        assertEquals(4_096, ConversationContext.contextWindowTokens(profile(LlmProviderKind.OLLAMA, "llama3.2")))
        assertEquals(128_000, ConversationContext.contextWindowTokens(profile(LlmProviderKind.OPENAI_COMPAT, "gpt-4o-mini")))
        assertEquals(200_000, ConversationContext.contextWindowTokens(profile(LlmProviderKind.ANTHROPIC, "claude-3-5-haiku")))
        assertEquals(32_000, ConversationContext.contextWindowTokens(profile(LlmProviderKind.OPENAI_COMPAT, "some-new-model")))
    }

    @Test
    fun modelLabelFallsBackToProfileName() {
        assertEquals("llama3.2", ConversationContext.modelLabel(profile(LlmProviderKind.OLLAMA, "llama3.2")))
        assertEquals("P", ConversationContext.modelLabel(profile(LlmProviderKind.OLLAMA, " ")))
    }
}
