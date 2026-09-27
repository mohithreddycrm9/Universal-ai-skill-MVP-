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
            QuickSuggestion("Pick a model", "Help me choose an LLM for writing vs coding vs long documents on mobile."),
            QuickSuggestion("HF search tips", "How do I pick a Hugging Face model id and what does :fastest mean?"),
            QuickSuggestion("Compare providers", "Compare cloud APIs vs local Ollama for privacy, cost, and speed."),
            QuickSuggestion("Cheaper stack", "Suggest a low-cost multi-model setup for a student."),
            QuickSuggestion("Switch models", "When should I switch models mid-conversation in this app?"),
            QuickSuggestion("Test failed", "My model test connection failed—what should I check in order?"),
            QuickSuggestion("Multilingual", "Best provider setup for multilingual chat?"),
            QuickSuggestion("Latency", "Tips to reduce latency on mobile networks."),
            QuickSuggestion("Reasoning vs fast", "When to use Fast vs Balanced vs Deep presets?"),
            QuickSuggestion("On-device path", "How do I run models on my home Ollama server from this phone?"),
        )

    val usage: List<QuickSuggestion> =
        listOf(
            QuickSuggestion("Explain spend", "Explain my usage dashboard and what drives estimated USD."),
            QuickSuggestion("Cut costs", "5 ways to lower LLM spend without losing quality for everyday chat."),
            QuickSuggestion("Budget caps", "How should I set daily and weekly spend limits?"),
            QuickSuggestion("Heavy vs light", "What usage pattern looks like 'light' vs 'power' user?"),
            QuickSuggestion("Token tips", "Write prompts that use fewer tokens."),
            QuickSuggestion("Model cost", "Which model in my breakdown costs the most per request?"),
            QuickSuggestion("Weekly report", "Summarize my usage pattern and one habit to improve."),
            QuickSuggestion("Anomaly check", "Could my usage indicate a misconfigured provider?"),
            QuickSuggestion("Student budget", "Recommend spend caps for homework-only use."),
            QuickSuggestion("Local savings", "When does Ollama save money vs cloud?"),
        )

    val extensions: List<QuickSuggestion> =
        listOf(
            QuickSuggestion("Meal skill", "Install meal planner skill and run my first weekly plan."),
            QuickSuggestion("Shop skill", "Use shopping research skill to compare two phones."),
            QuickSuggestion("Study coach", "Sample first session with Study coach installed."),
            QuickSuggestion("Built-in /calc", "Show a real-world example using /calc in chat."),
            QuickSuggestion("Optional /fetch", "When is /fetch appropriate for research?"),
            QuickSuggestion("Which skill?", "Which built-in skill fits learning a new language?"),
            QuickSuggestion("Combine skills", "Best practices using multiple skill packs together."),
            QuickSuggestion("Custom skill", "What makes a good skill pack URL to import?"),
            QuickSuggestion("Per-chat toggle", "How do per-conversation skill toggles work?"),
            QuickSuggestion("Code review", "What to paste for a useful code review skill session?"),
        )

    val settings: List<QuickSuggestion> =
        listOf(
            QuickSuggestion("System prompt", "Write a strong default system prompt for everyday assistant use."),
            QuickSuggestion("Focus topic", "How should I use focus topic for a semester goal?"),
            QuickSuggestion("Voice setup", "Walk me through voice input and spoken replies."),
            QuickSuggestion("Privacy keys", "Checklist for storing API keys safely on Android."),
            QuickSuggestion("Sync relay", "Explain WebSocket sync phone ↔ desktop safely."),
            QuickSuggestion("Backup", "How encrypted backup works and what URL to use."),
            QuickSuggestion("Spend limits", "Recommended USD caps for light daily chat."),
            QuickSuggestion("Theme", "Best theme settings for reading long answers."),
            QuickSuggestion("BYOK why", "Why bring-your-own-key matters vs single-vendor apps."),
            QuickSuggestion("Share to app", "How Share into Lumina works from other apps."),
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
