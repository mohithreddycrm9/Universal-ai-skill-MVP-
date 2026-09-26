package com.skillmcp.mentor.llm

enum class ModelPreset(val label: String, val temperature: Double, val hint: String) {
    FAST("Fast", 0.35, "Quick answers, lower creativity"),
    BALANCED("Balanced", 0.7, "Default everyday assistant"),
    DEEP("Deep", 0.9, "Longer, more exploratory replies"),
}
