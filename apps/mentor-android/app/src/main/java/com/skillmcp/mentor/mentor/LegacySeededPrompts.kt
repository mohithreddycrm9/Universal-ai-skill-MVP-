package com.skillmcp.mentor.mentor

/**
 * The template prompts that builds before round 8 seeded into every user's saved-prompt library.
 * Kept only so [MentorRepository] can remove copies the user never edited; nothing is seeded any more.
 */
internal object LegacySeededPrompts {
    /** title to body, exactly as they were inserted. */
    private val seeded: Map<String, String> =
        mapOf(
            "Explain simply" to "Explain this like I'm new to the topic, with a simple example.",
            "Action plan" to "Create a step-by-step plan I can follow today.",
            "Writing coach" to "Review my message for clarity, tone, and grammar. Suggest improvements.",
            "Weekly focus" to "What are the top 3 things I should focus on this week?",
            "Meeting prep" to "Help me prepare talking points for a meeting tomorrow.",
            "Outline notes" to "Turn my rough notes into a clear outline with headings.",
            "Interview prep" to "Suggest 5 interview questions for a role I describe.",
            "Small habit" to "Give me a gentle habit I can start today and track for 7 days.",
            "Plan my week" to "Help me plan my week with priorities, time blocks, and one stretch goal.",
            "Brainstorm" to "Brainstorm 10 creative ideas for a goal I describe.",
            "Compare options" to "Compare pros and cons of two options I give you.",
            "Summarize chat" to "Summarize the key points from our conversation in 5 bullets.",
            "Draft email" to "Draft a professional email. Ask me for recipient, tone, and key points.",
            "Study plan" to "Build a 7-day study plan for a subject I name.",
            "Meal plan" to "Plan meals for the week and give me a grouped grocery list.",
            "Shop compare" to "Help me compare products before I buy. Ask category and budget.",
            "Morning brief" to "Run a morning brief: my top 3 tasks for today.",
            "Polish message" to "I'll paste a draft—rewrite it in casual and professional versions.",
        )

    fun isUntouchedSeed(title: String, body: String): Boolean = seeded[title] == body
}
