package com.skillmcp.mentor.skills

data class CatalogSkill(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val sourceUrl: String,
    val trustTier: String = "Curated",
)

object SkillCatalog {
    val featured: List<CatalogSkill> =
        listOf(
            CatalogSkill(
                id = "universal-skill-trust",
                title = "Skill trust & safety",
                description = "Discover, verify, and use agent skills with a security-first workflow.",
                category = "Agent tooling",
                sourceUrl = "https://github.com/mohithreddycrm9/Universal-ai-skill-MVP-",
                trustTier = "Official",
            ),
            CatalogSkill(
                id = "anthropic-skills",
                title = "Anthropic skill examples",
                description = "Reference skill packs for writing, analysis, and workflows.",
                category = "Productivity",
                sourceUrl = "https://github.com/anthropics/skills",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "cursor-skills",
                title = "Cursor agent skills",
                description = "Patterns for IDE agents, hooks, and project context.",
                category = "Coding",
                sourceUrl = "https://github.com/getcursor/cursor",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "markdown-outline",
                title = "Markdown outliner",
                description = "Structure long documents and outlines with consistent headings.",
                category = "Writing",
                sourceUrl = "https://github.com/mohithreddycrm9/Universal-ai-skill-MVP-/tree/main/examples/skills/benign/markdown-outline",
                trustTier = "Curated",
            ),
        )
}
