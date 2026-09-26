package com.skillmcp.mentor.mentor

/**
 * Curated workflows aligned with top consumer AI adoption (writing, learning, planning,
 * shopping research, privacy/BYOK). Sources: Menlo Ventures State of Consumer AI 2026,
 * Pew Research chatbot use 2026, PYMNTS consumer AI routines 2026.
 */
data class PopularUseCase(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val prompt: String,
    val bundledSkillAsset: String? = null,
)

object UseCaseCatalog {
    val categories: List<String> =
        listOf("Write", "Learn", "Life", "Shop", "Work", "Privacy")

    val featured: List<PopularUseCase> =
        listOf(
            PopularUseCase(
                id = "rewrite-message",
                title = "Polish my message",
                subtitle = "Reword texts or emails before you send",
                category = "Write",
                prompt =
                    "I'll paste a draft message. Rewrite it to be clear and friendly. " +
                        "Offer 2 tone options: casual and professional. Ask me to paste the text.",
            ),
            PopularUseCase(
                id = "homework-help",
                title = "Homework & assignments",
                subtitle = "Learn the concept—not just the answer",
                category = "Learn",
                prompt =
                    "Act as a tutor. Ask what subject and question I have. Guide me with hints " +
                        "and check my understanding; do not do the entire assignment for me.",
                bundledSkillAsset = "skills/study-coach.md",
            ),
            PopularUseCase(
                id = "meal-grocery",
                title = "Meals & grocery list",
                subtitle = "Weekly plan + shopping list",
                category = "Life",
                prompt =
                    "Help me plan meals for the week for my household size and dietary preferences. " +
                        "End with a grouped grocery list and estimated prep time per day.",
                bundledSkillAsset = "skills/meal-planner.md",
            ),
            PopularUseCase(
                id = "shop-research",
                title = "Shop smarter",
                subtitle = "Compare options before you buy",
                category = "Shop",
                prompt =
                    "I'm considering a purchase. Ask what product category and budget I have. " +
                        "Give a comparison table, pros/cons, and what to verify before buying (no checkout links required).",
                bundledSkillAsset = "skills/shopping-research.md",
            ),
            PopularUseCase(
                id = "daily-brief",
                title = "Morning brief",
                subtitle = "Focus plan for today",
                category = "Life",
                prompt =
                    "Run a 3-minute morning brief: ask my top priority, calendar constraints, and energy level. " +
                        "Output today's top 3 tasks and one thing to skip.",
            ),
            PopularUseCase(
                id = "explain-search",
                title = "Explain & research",
                subtitle = "Understand a topic fast",
                category = "Learn",
                prompt =
                    "I want to understand a topic deeply but quickly. Ask what topic. Give a structured explainer: " +
                        "TL;DR, key terms, common misconceptions, and what to read next.",
            ),
            PopularUseCase(
                id = "meeting-notes",
                title = "Meeting follow-up",
                subtitle = "Decisions and action items",
                category = "Work",
                prompt =
                    "I'll paste rough meeting notes. Produce decisions, owners, action items, and open questions. " +
                        "Flag anything unclear.",
                bundledSkillAsset = "skills/meeting-notes.md",
            ),
            PopularUseCase(
                id = "resume-interview",
                title = "Job & interview",
                subtitle = "Resume bullets and practice",
                category = "Work",
                prompt =
                    "Help with my job search. Ask for role target and experience. Suggest 5 resume bullet improvements " +
                        "and 5 likely interview questions with sample answer outlines.",
            ),
            PopularUseCase(
                id = "translate",
                title = "Translate & localize",
                subtitle = "Natural phrasing in another language",
                category = "Write",
                prompt =
                    "Ask source and target languages. I'll paste text. Translate naturally and note idioms or formal vs informal options.",
            ),
            PopularUseCase(
                id = "budget-deals",
                title = "Budget & deals",
                subtitle = "Stretch your money further",
                category = "Shop",
                prompt =
                    "Help me think through a monthly budget tradeoff or finding discounts ethically. " +
                        "Ask my category and goal; give practical steps (general guidance, not financial advice).",
            ),
            PopularUseCase(
                id = "byok-privacy",
                title = "My keys, my models",
                subtitle = "Why Universal AI is different",
                category = "Privacy",
                prompt =
                    "Explain how this app lets me use my own API keys and Hugging Face models, track usage spend, " +
                        "and keep skill context on-device. What should I configure first?",
            ),
            PopularUseCase(
                id = "local-ollama",
                title = "Go local with Ollama",
                subtitle = "More privacy on home Wi‑Fi",
                category = "Privacy",
                prompt =
                    "Walk me through connecting Ollama on my home network to this Android app, including emulator vs phone IP tips.",
            ),
            PopularUseCase(
                id = "presentation",
                title = "Slide outline",
                subtitle = "Structure a talk or deck",
                category = "Work",
                prompt =
                    "Ask my audience and topic. Produce a slide-by-slide outline with speaker notes and one hook for the opening.",
            ),
            PopularUseCase(
                id = "health-info",
                title = "Health info (general)",
                subtitle = "Educational—not medical advice",
                category = "Life",
                prompt =
                    "I have a general health question. Ask what I want to know. Provide educational information only, " +
                        "cite uncertainty, and tell me when to contact a clinician.",
            ),
            PopularUseCase(
                id = "trip-plan",
                title = "Trip planner",
                subtitle = "Itinerary in one thread",
                category = "Life",
                prompt =
                    "Plan a trip: ask destination, dates, budget, and interests. Output day-by-day plan, packing list, and backup rainy-day ideas.",
                bundledSkillAsset = "skills/travel-planner.md",
            ),
            PopularUseCase(
                id = "code-copilot",
                title = "Code copilot",
                subtitle = "Debug, review, learn",
                category = "Work",
                prompt =
                    "I'm working on code. Ask language and goal. Help me debug or understand a snippet I paste; suggest tests.",
                bundledSkillAsset = "skills/code-reviewer.md",
            ),
        )

    fun byCategory(category: String): List<PopularUseCase> = featured.filter { it.category == category }
}
