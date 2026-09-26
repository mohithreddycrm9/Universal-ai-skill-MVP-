package com.skillmcp.mentor.skills

import android.content.Context
import com.skillmcp.mentor.data.db.SkillEntity
data class BundledSkillPack(
    val assetPath: String,
    val title: String,
    val category: String,
    val description: String,
)

object BundledSkills {
    val packs: List<BundledSkillPack> =
        listOf(
            BundledSkillPack(
                "skills/study-coach.md",
                "Study coach",
                "Learning",
                "Break topics into lessons, quizzes, and spaced review.",
            ),
            BundledSkillPack(
                "skills/email-writer.md",
                "Professional email",
                "Writing",
                "Draft clear emails with tone and structure options.",
            ),
            BundledSkillPack(
                "skills/travel-planner.md",
                "Travel planner",
                "Life",
                "Itineraries, packing lists, and local tips.",
            ),
            BundledSkillPack(
                "skills/meeting-notes.md",
                "Meeting notes",
                "Productivity",
                "Summaries, decisions, and action items.",
            ),
            BundledSkillPack(
                "skills/code-reviewer.md",
                "Code reviewer",
                "Coding",
                "Review diffs for bugs, style, and security basics.",
            ),
            BundledSkillPack(
                "skills/research-brief.md",
                "Research brief",
                "Research",
                "Structured briefs with sources and open questions.",
            ),
            BundledSkillPack(
                "skills/markdown-outline.md",
                "Markdown outliner",
                "Writing",
                "Turn notes into consistent heading outlines.",
            ),
            BundledSkillPack(
                "skills/csv-normalize.md",
                "CSV normalize",
                "Data",
                "Clean and normalize tabular data descriptions.",
            ),
            BundledSkillPack(
                "skills/meal-planner.md",
                "Meal & grocery",
                "Life",
                "Weekly meals and a grouped shopping list.",
            ),
            BundledSkillPack(
                "skills/shopping-research.md",
                "Shopping research",
                "Shop",
                "Compare products before you buy.",
            ),
        )
}

class BundledSkillInstaller(private val context: Context) {
    fun loadMarkdown(assetPath: String): String =
        context.assets.open(assetPath).bufferedReader().use { it.readText() }

    fun toEntity(pack: BundledSkillPack): SkillEntity {
        val markdown = loadMarkdown(pack.assetPath)
        val title =
            markdown.lineSequence().firstOrNull { it.startsWith("#") }?.removePrefix("#")?.trim()
                ?: pack.title
        return SkillEntity(
            id = "bundled:${pack.assetPath}",
            owner = "bundled",
            repo = pack.assetPath,
            ref = "local",
            title = title,
            markdown = markdown,
            addedAt = System.currentTimeMillis(),
        )
    }
}
