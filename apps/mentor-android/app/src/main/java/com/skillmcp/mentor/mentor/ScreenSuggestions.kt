package com.skillmcp.mentor.mentor

enum class SuggestionScreen {
    CHAT,
    MODELS,
    USAGE,
    EXTENSIONS,
    SETTINGS,
}

object ScreenSuggestions {
    val models: List<QuickSuggestion> =
        listOf(
            QuickSuggestion("Pick a model", "Help me choose an LLM for everyday chat vs coding vs long documents."),
            QuickSuggestion("HF search tips", "How do I pick a Hugging Face model id and what does :fastest mean?"),
            QuickSuggestion("Compare providers", "Compare OpenAI-compatible APIs vs local Ollama for privacy and cost."),
            QuickSuggestion("Test failed", "My model test connection failed—what should I check in order?"),
            QuickSuggestion("Cheaper setup", "Suggest a low-cost model stack for a student budget."),
            QuickSuggestion("Latency", "Which settings reduce latency on mobile networks?"),
            QuickSuggestion("Multilingual", "Which provider setup works best for multilingual chat?"),
            QuickSuggestion("Vision models", "Do any of my provider types support images and how do I configure them?"),
        )

    val usage: List<QuickSuggestion> =
        listOf(
            QuickSuggestion("Explain my spend", "Explain my usage dashboard numbers and what drives estimated USD."),
            QuickSuggestion("Cut costs", "Give me 5 practical ways to lower LLM spend without losing quality."),
            QuickSuggestion("Budget guardrails", "How should I set daily and weekly spend limits in Settings?"),
            QuickSuggestion("Model comparison", "Which model in my usage breakdown is most expensive per request?"),
            QuickSuggestion("Token tips", "How can I write prompts that use fewer tokens?"),
            QuickSuggestion("Weekly report", "Summarize what my usage pattern says about how I use the app."),
            QuickSuggestion("Anomaly check", "Could anything in my usage indicate a misconfigured provider?"),
            QuickSuggestion("Offline option", "When should I use local Ollama based on my usage trends?"),
        )

    val extensions: List<QuickSuggestion> =
        listOf(
            QuickSuggestion("Which skill?", "Which built-in skill pack fits learning a new language?"),
            QuickSuggestion("Install order", "What order should I install skill packs for productivity?"),
            QuickSuggestion("Plugin ideas", "What are creative uses for /calc, /time, and /fetch plugins?"),
            QuickSuggestion("Per-chat skills", "How do per-conversation skill toggles work?"),
            QuickSuggestion("Study coach", "I installed Study coach—give me a sample first session."),
            QuickSuggestion("Code reviewer", "I installed Code reviewer—what should I paste for a useful review?"),
            QuickSuggestion("Custom skill URL", "What makes a good public skill repository URL to import?"),
            QuickSuggestion("Combine skills", "Can I use multiple skill packs together? Best practices?"),
        )

    val settings: List<QuickSuggestion> =
        listOf(
            QuickSuggestion("System prompt", "Write a strong default system prompt for a friendly general assistant."),
            QuickSuggestion("Focus topic", "How should I use the focus topic field effectively?"),
            QuickSuggestion("Voice setup", "Walk me through voice input and spoken replies settings."),
            QuickSuggestion("Theme & readability", "Recommend theme, accent, and font scale for long reading sessions."),
            QuickSuggestion("Sync relay", "Explain WebSocket sync and how to connect phone to desktop safely."),
            QuickSuggestion("Backup", "How does encrypted backup work and what should I put in the upload URL?"),
            QuickSuggestion("Spend limits", "Recommend daily and weekly USD caps for light vs heavy use."),
            QuickSuggestion("Privacy checklist", "Give me a privacy checklist for API keys on mobile."),
        )

    fun forScreen(screen: SuggestionScreen): List<QuickSuggestion> =
        when (screen) {
            SuggestionScreen.CHAT -> ChatSuggestions.heroStarters
            SuggestionScreen.MODELS -> models
            SuggestionScreen.USAGE -> usage
            SuggestionScreen.EXTENSIONS -> extensions
            SuggestionScreen.SETTINGS -> settings
        }
}
