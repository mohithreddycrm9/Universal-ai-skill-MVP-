package com.skillmcp.mentor.llm

enum class LlmProviderKind(val label: String) {
    OPENAI_COMPAT("OpenAI-compatible"),
    HUGGING_FACE("Hugging Face"),
    ANTHROPIC("Anthropic Claude"),
    GEMINI("Google Generative API"),
    OLLAMA("Ollama (local)"),
}

data class LlmProfile(
    val id: String,
    val name: String,
    val kind: LlmProviderKind,
    val baseUrl: String,
    val model: String,
    val apiKey: String = "",
    val linkedAccount: String = "",
    val inputCostPer1M: Double = 0.0,
    val outputCostPer1M: Double = 0.0,
    val isBuiltIn: Boolean = false,
)

fun defaultLlmProfiles(): List<LlmProfile> =
    listOf(
        LlmProfile(
            id = "openai",
            name = "OpenAI",
            kind = LlmProviderKind.OPENAI_COMPAT,
            baseUrl = "https://api.openai.com/v1/",
            model = "gpt-4o-mini",
            inputCostPer1M = 0.15,
            outputCostPer1M = 0.60,
            isBuiltIn = true,
        ),
        LlmProfile(
            id = "google-gen",
            name = "Google AI",
            kind = LlmProviderKind.GEMINI,
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/",
            model = "gemini-2.0-flash",
            inputCostPer1M = 0.10,
            outputCostPer1M = 0.40,
            isBuiltIn = true,
        ),
        LlmProfile(
            id = "huggingface",
            name = "Hugging Face",
            kind = LlmProviderKind.HUGGING_FACE,
            baseUrl = HuggingFaceDefaults.ROUTER_BASE_URL,
            model = "meta-llama/Meta-Llama-3-8B-Instruct:fastest",
            inputCostPer1M = 0.0,
            outputCostPer1M = 0.0,
            isBuiltIn = true,
        ),
        LlmProfile(
            id = "anthropic",
            name = "Anthropic",
            kind = LlmProviderKind.ANTHROPIC,
            baseUrl = "https://api.anthropic.com/v1/",
            model = "claude-3-5-haiku-latest",
            inputCostPer1M = 0.80,
            outputCostPer1M = 4.00,
            isBuiltIn = true,
        ),
        LlmProfile(
            id = "ollama",
            name = "Ollama",
            kind = LlmProviderKind.OLLAMA,
            baseUrl = "http://10.0.2.2:11434/",
            model = "llama3.2",
            inputCostPer1M = 0.0,
            outputCostPer1M = 0.0,
            isBuiltIn = true,
        ),
    )
