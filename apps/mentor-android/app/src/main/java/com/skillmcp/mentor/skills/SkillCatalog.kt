package com.skillmcp.mentor.skills

import com.skillmcp.mentor.data.db.SkillEntity

data class CatalogSkill(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val sourceUrl: String,
    val trustTier: String = "Curated",
    val needsNetwork: Boolean = true,
)

object SkillCatalog {
    /**
     * Every entry points at a folder that contains a SKILL.md (checked 2026-09-27), so installing works.
     * Earlier entries pointed at repositories without a SKILL.md and could never install.
     */
    val featured: List<CatalogSkill> =
        listOf(
            CatalogSkill(
                id = "universal-skill-trust",
                title = "Skill trust & safety",
                description = "Discover, verify, and use agent skills with a security-first workflow.",
                category = "Agent tooling",
                sourceUrl = "https://github.com/mohithreddycrm9/Universal-ai-skill-MVP-/tree/main/skills/universal-skill-trust",
                trustTier = "Official",
            ),
            CatalogSkill(
                id = "anthropic-pdf",
                title = "PDF toolkit",
                description = "Read, fill, merge, and summarize PDF documents step by step.",
                category = "Productivity",
                sourceUrl = "https://github.com/anthropics/skills/tree/main/skills/pdf",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "anthropic-docx",
                title = "Word documents",
                description = "Draft and revise .docx documents with tracked structure and styles.",
                category = "Productivity",
                sourceUrl = "https://github.com/anthropics/skills/tree/main/skills/docx",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "anthropic-internal-comms",
                title = "Internal comms",
                description = "Write status updates, newsletters, and FAQs in a clear company voice.",
                category = "Writing",
                sourceUrl = "https://github.com/anthropics/skills/tree/main/skills/internal-comms",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "anthropic-skill-creator",
                title = "Skill creator",
                description = "Design and test your own skill pack with a guided checklist.",
                category = "Agent tooling",
                sourceUrl = "https://github.com/anthropics/skills/tree/main/skills/skill-creator",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "anthropic-frontend-design",
                title = "Frontend design",
                description = "Plan distinctive, production-grade web UI with clear design choices.",
                category = "Coding",
                sourceUrl = "https://github.com/anthropics/skills/tree/main/skills/frontend-design",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "wshobson-mobile-android-design",
                title = "Android design (Material 3)",
                description = "Material 3 and Jetpack Compose layout, navigation, and component guidance.",
                category = "Coding",
                sourceUrl = "https://github.com/wshobson/agents/tree/main/plugins/ui-design/skills/mobile-android-design",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "wshobson-accessibility",
                title = "Accessibility compliance",
                description = "WCAG checks and mobile accessibility patterns for inclusive apps.",
                category = "Coding",
                sourceUrl = "https://github.com/wshobson/agents/tree/main/plugins/ui-design/skills/accessibility-compliance",
                trustTier = "Curated",
            ),
        )

    /** Remote catalog minus packs already shipped offline or installed. */
    fun featuredForUi(installed: List<SkillEntity>): List<CatalogSkill> {
        val bundledTitles = BundledSkills.packs.map { it.title.lowercase() }.toSet()
        val installedTitles = installed.map { it.title.lowercase() }.toSet()
        return featured.filter { entry ->
            val titleKey = entry.title.lowercase()
            titleKey !in bundledTitles && titleKey !in installedTitles
        }
    }

    fun isBundledInstalled(pack: BundledSkillPack, installed: List<SkillEntity>): Boolean =
        installed.any { it.title.equals(pack.title, ignoreCase = true) }
}
