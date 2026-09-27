package com.skillmcp.mentor.skills

import com.skillmcp.mentor.data.db.SkillEntity
import com.skillmcp.mentor.skills.finder.OfficialSkillPolicy

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
                trustTier = "First-party",
            ),
            CatalogSkill(
                id = "anthropic-pdf",
                title = "PDF toolkit",
                description = "Read, fill, merge, and summarize PDF documents step by step.",
                category = "Productivity",
                sourceUrl = "https://github.com/anthropics/skills/tree/main/skills/pdf",
                trustTier = "Verified official · Anthropic",
            ),
            CatalogSkill(
                id = "anthropic-docx",
                title = "Word documents",
                description = "Draft and revise .docx documents with tracked structure and styles.",
                category = "Productivity",
                sourceUrl = "https://github.com/anthropics/skills/tree/main/skills/docx",
                trustTier = "Verified official · Anthropic",
            ),
            CatalogSkill(
                id = "anthropic-internal-comms",
                title = "Internal comms",
                description = "Write status updates, newsletters, and FAQs in a clear company voice.",
                category = "Writing",
                sourceUrl = "https://github.com/anthropics/skills/tree/main/skills/internal-comms",
                trustTier = "Verified official · Anthropic",
            ),
            CatalogSkill(
                id = "anthropic-skill-creator",
                title = "Skill creator",
                description = "Design and test your own skill pack with a guided checklist.",
                category = "Agent tooling",
                sourceUrl = "https://github.com/anthropics/skills/tree/main/skills/skill-creator",
                trustTier = "Verified official · Anthropic",
            ),
            CatalogSkill(
                id = "anthropic-frontend-design",
                title = "Frontend design",
                description = "Plan distinctive, production-grade web UI with clear design choices.",
                category = "Coding",
                sourceUrl = "https://github.com/anthropics/skills/tree/main/skills/frontend-design",
                trustTier = "Verified official · Anthropic",
            ),
        )

    /**
     * The app publisher's own first-party skill (this repository). It is the only entry allowed
     * outside the official-company allowlist; everything else must pass [OfficialSkillPolicy].
     */
    const val FIRST_PARTY_OWNER = "mohithreddycrm9/Universal-ai-skill-MVP-"

    /** Official-only: allowlisted company org over HTTPS, or the app's own first-party skill. */
    fun isAllowed(entry: CatalogSkill): Boolean =
        entry.sourceUrl.startsWith("https://github.com/$FIRST_PARTY_OWNER/") ||
            OfficialSkillPolicy.verifyUrl(entry.sourceUrl).accepted

    /** Remote catalog minus packs already shipped offline or installed; community sources never shown. */
    fun featuredForUi(installed: List<SkillEntity>): List<CatalogSkill> {
        val bundledTitles = BundledSkills.packs.map { it.title.lowercase() }.toSet()
        val installedTitles = installed.map { it.title.lowercase() }.toSet()
        return featured.filter(::isAllowed).filter { entry ->
            val titleKey = entry.title.lowercase()
            titleKey !in bundledTitles && titleKey !in installedTitles
        }
    }

    fun isBundledInstalled(pack: BundledSkillPack, installed: List<SkillEntity>): Boolean =
        installed.any { it.title.equals(pack.title, ignoreCase = true) }
}
