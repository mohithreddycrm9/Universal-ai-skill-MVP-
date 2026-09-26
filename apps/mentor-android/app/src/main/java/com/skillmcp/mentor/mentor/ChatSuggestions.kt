package com.skillmcp.mentor.mentor

data class QuickSuggestion(
    val label: String,
    val prompt: String,
)

object ChatSuggestions {
    val heroStarters: List<QuickSuggestion> =
        listOf(
            QuickSuggestion("Plan my week", "Help me plan my week with priorities, time blocks, and one stretch goal."),
            QuickSuggestion("Explain simply", "Explain a topic I choose in simple terms with a short analogy."),
            QuickSuggestion("Draft an email", "Draft a professional email. Ask me for recipient, tone, and key points."),
            QuickSuggestion("Step-by-step", "Walk me through solving a problem step by step. Ask what the problem is."),
            QuickSuggestion("Brainstorm", "Brainstorm 10 creative ideas for a goal I describe. Include wild cards."),
            QuickSuggestion("Study plan", "Build a 7-day study plan for a subject I name, with daily tasks and a quiz."),
            QuickSuggestion("Compare options", "Compare pros and cons of two options I give you. End with a recommendation."),
            QuickSuggestion("Meeting prep", "Help me prepare for a meeting: agenda, talking points, and questions to ask."),
            QuickSuggestion("Travel ideas", "Suggest a weekend itinerary for a city I choose, with food and walking routes."),
            QuickSuggestion("Fitness routine", "Suggest a beginner-friendly weekly fitness routine (not medical advice)."),
            QuickSuggestion("Budget tips", "Help me think through a monthly budget category I name (general guidance only)."),
            QuickSuggestion("Code help", "Help me debug or understand a code snippet I paste. Ask for language if missing."),
            QuickSuggestion("Rewrite tone", "Rewrite my text to sound more confident and concise. I'll paste the draft."),
            QuickSuggestion("Daily reflection", "Guide me through a 5-minute evening reflection with 4 thoughtful questions."),
        )

    private data class Seed(
        val label: String,
        val prompt: String,
        val because: String,
        val priority: Int,
    )

    internal fun baselineBuildSuggestions(limit: Int): List<BuildSuggestion> {
        val pool =
            listOf(
                Seed("Summarize", "Summarize the key points from our conversation so far in 5 bullets.", "followup", 55),
                Seed("Go deeper", "Pick the most important idea from your last answer and explain it with an example.", "followup", 54),
                Seed("Shorter", "Give me a shorter version of your last answer in under 120 words.", "followup", 53),
                Seed("Checklist", "Turn your last answer into a numbered checklist I can follow today.", "followup", 52),
                Seed("Quiz me", "Create a 5-question quiz on what we discussed, then grade my answers.", "learn", 51),
                Seed("Role-play", "Role-play a conversation where you help me practice a skill I describe.", "practice", 50),
                Seed("Pros & cons", "List pros and cons of a decision I'm facing. Ask me for context first.", "decide", 49),
                Seed("Weekly review", "Help me run a weekly review: wins, blockers, and next week's top 3.", "productivity", 48),
                Seed("Explain like 12", "Explain a concept I name as if I'm twelve years old.", "learn", 47),
                Seed("Counterargument", "Give the strongest counterargument to your last answer, then reconcile both views.", "think", 46),
                Seed("Template", "Give me a reusable template I can fill in based on our topic.", "template", 45),
                Seed("What to ask", "What are 5 smart follow-up questions I should ask you next?", "meta", 44),
            )
        return pool.take(limit.coerceAtMost(pool.size)).map { seed ->
            BuildSuggestion(
                id = "${seed.because}-${seed.label}",
                label = seed.label,
                prompt = seed.prompt,
                because = seed.because,
                priority = seed.priority,
            )
        }
    }
}
