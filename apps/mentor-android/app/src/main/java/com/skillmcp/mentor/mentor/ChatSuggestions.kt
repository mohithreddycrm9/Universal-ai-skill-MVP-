package com.skillmcp.mentor.mentor

data class QuickSuggestion(
    val label: String,
    val prompt: String,
)

object ChatSuggestions {
    val heroStarters: List<QuickSuggestion> =
        listOf(
            QuickSuggestion("Polish message", "I'll paste a draft—rewrite it casual and professional versions."),
            QuickSuggestion("Meal plan", "Plan meals for the week and give me a grouped grocery list."),
            QuickSuggestion("Shop compare", "Help me compare products before I buy. Ask category and budget."),
            QuickSuggestion("Homework help", "Tutor me on a subject—hints first, not full answers."),
            QuickSuggestion("Morning brief", "Run a morning brief: my top 3 tasks for today."),
            QuickSuggestion("Plan my week", "Help me plan my week with priorities, time blocks, and one stretch goal."),
            QuickSuggestion("Explain simply", "Explain a topic I choose in simple terms with a short analogy."),
            QuickSuggestion("Draft an email", "Draft a professional email. Ask me for recipient, tone, and key points."),
            QuickSuggestion("Translate", "Ask languages, then translate text I paste with natural phrasing."),
            QuickSuggestion("Study plan", "Build a 7-day study plan for a subject I name, with daily tasks and a quiz."),
            QuickSuggestion("Meeting prep", "Help me prepare for a meeting: agenda, talking points, and questions to ask."),
            QuickSuggestion("Trip plan", "Plan a trip: ask destination, dates, budget; give day-by-day itinerary."),
            QuickSuggestion("Code help", "Help me debug or understand a code snippet I paste."),
            QuickSuggestion("Rewrite tone", "Rewrite my text to sound more confident and concise."),
            QuickSuggestion("Slide outline", "Create a slide outline with speaker notes for a talk I describe."),
            QuickSuggestion("Daily reflection", "Guide me through a 5-minute evening reflection."),
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
                Seed("Meal ideas", "Suggest 3 quick dinners from ingredients I list.", "life", 51),
                Seed("Quiz me", "Create a 5-question quiz on what we discussed, then grade my answers.", "learn", 50),
                Seed("Compare", "Compare two options I'm deciding between in a table.", "decide", 49),
                Seed("Weekly review", "Help me run a weekly review: wins, blockers, and next week's top 3.", "productivity", 48),
                Seed("Explain like 12", "Explain a concept I name as if I'm twelve years old.", "learn", 47),
                Seed("Email edit", "Tighten my last draft for clarity and tone.", "write", 46),
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
