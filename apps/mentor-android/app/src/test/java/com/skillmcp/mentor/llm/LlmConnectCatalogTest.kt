package com.skillmcp.mentor.llm

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LlmConnectCatalogTest {
    @Test
    fun openAiRequiresApiKey() {
        val profile =
            LlmProfile(
                id = "openai",
                name = "OpenAI",
                kind = LlmProviderKind.OPENAI_COMPAT,
                baseUrl = "https://api.openai.com/v1/",
                model = "gpt-4o-mini",
                apiKey = "",
            )
        assertFalse(profile.isConfigured())
        assertTrue(profile.copy(apiKey = "sk-test").isConfigured())
    }

    @Test
    fun ollamaUsesUrlAndModel() {
        val profile =
            LlmProfile(
                id = "ollama",
                name = "Ollama",
                kind = LlmProviderKind.OLLAMA,
                baseUrl = "http://10.0.2.2:11434/",
                model = "llama3.2",
            )
        assertTrue(profile.isConfigured())
    }
}
