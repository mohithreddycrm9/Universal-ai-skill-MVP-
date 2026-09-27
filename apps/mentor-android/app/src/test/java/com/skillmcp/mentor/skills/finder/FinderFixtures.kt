package com.skillmcp.mentor.skills.finder

object FinderFixtures {
    val anthropicsOrg =
        SkillIndexOrg(
            login = "anthropics",
            name = "Anthropic",
            type = "Organization",
            isVerified = true,
            blog = "https://anthropic.com",
            htmlUrl = "https://github.com/anthropics",
        )

    val skillsRepo =
        SkillIndexRepo(
            fullName = "anthropics/skills",
            owner = "anthropics",
            ownerType = "Organization",
            htmlUrl = "https://github.com/anthropics/skills",
            description = "Public repository for Agent Skills",
            defaultBranch = "main",
            license = "",
            pushedAt = "2026-09-24T16:20:39Z",
        )

    val pdfSkill =
        SkillIndexSkill(
            repo = "anthropics/skills",
            path = "skills/pdf",
            name = "pdf",
            description = "Use this skill whenever the user wants to do anything with PDF files.",
            license = "Proprietary. LICENSE.txt has complete terms",
            skillUrl = "https://github.com/anthropics/skills/tree/main/skills/pdf",
            rawUrl = "https://raw.githubusercontent.com/anthropics/skills/main/skills/pdf/SKILL.md",
        )

    fun officialSkill(
        name: String,
        description: String,
        company: String = "Anthropic",
        repo: String = "anthropics/skills",
    ): OfficialSkill =
        OfficialSkill(
            id = "$repo:skills/$name",
            name = name,
            description = description,
            company = company,
            orgLogin = repo.substringBefore('/'),
            repoFullName = repo,
            repoUrl = "https://github.com/$repo",
            skillUrl = "https://github.com/$repo/tree/main/skills/$name",
            license = "MIT",
            lastUpdated = "2026-09-24T16:20:39Z",
            category = SkillCategory.classify(name, description),
            installedSkillId = "$repo@main:skills/$name",
        )
}
