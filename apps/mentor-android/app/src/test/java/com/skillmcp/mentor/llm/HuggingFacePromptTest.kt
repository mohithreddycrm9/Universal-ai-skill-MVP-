package com.skillmcp.mentor.llm

import com.skillmcp.mentor.mentor.ChatMessageDto
import org.junit.Assert.assertTrue
import org.junit.Test

class HuggingFacePromptTest {
    @Test
    fun includesUserTurnAndAssistantPrefix() {
        val prompt =
            HuggingFacePrompt.toServerlessPrompt(
                system = "Be helpful",
                history = listOf(ChatMessageDto("user", "Hi")),
                userMessage = "What's 2+2?",
            )
        assertTrue(prompt.contains("Be helpful"))
        assertTrue(prompt.contains("What's 2+2?"))
        assertTrue(prompt.endsWith("Assistant:"))
    }

    @Test
    fun stripsProviderSuffixForServerlessPath() {
        assertTrue(HuggingFaceDefaults.serverlessModelId("org/model:fastest") == "org/model")
    }
}
