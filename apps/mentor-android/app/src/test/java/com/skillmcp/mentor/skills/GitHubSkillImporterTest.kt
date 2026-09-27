package com.skillmcp.mentor.skills

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubSkillImporterTest {
    @Test
    fun plainRepoDefaultsToMainAndRootPaths() {
        val parsed = GitHubSkillImporter.parseRepoUrl("https://github.com/owner/my-skill.git/")
        assertEquals(GitHubSkillImporter.ParsedRepo("owner", "my-skill", "main", ""), parsed)
        assertEquals(
            listOf("SKILL.md", "skills/my-skill/SKILL.md", ".cursor/skills/my-skill/SKILL.md"),
            GitHubSkillImporter.candidateSkillPaths(parsed),
        )
    }

    @Test
    fun treeUrlWithFolderPointsAtThatFoldersSkill() {
        val parsed = GitHubSkillImporter.parseRepoUrl("https://github.com/anthropics/skills/tree/main/skills/pdf")
        assertEquals("skills/pdf", parsed.path)
        assertEquals(listOf("skills/pdf/SKILL.md"), GitHubSkillImporter.candidateSkillPaths(parsed))
    }

    @Test
    fun linkToTheSkillFileItselfIsAccepted() {
        val parsed = GitHubSkillImporter.parseRepoUrl("https://github.com/o/r/tree/dev/a/b/SKILL.md")
        assertEquals("dev", parsed.ref)
        assertEquals("a/b", parsed.path)
    }

    @Test
    fun everyCatalogEntryPointsAtASkillFolder() {
        SkillCatalog.featured.forEach { entry ->
            val parsed = GitHubSkillImporter.parseRepoUrl(entry.sourceUrl)
            assertTrue(entry.sourceUrl, parsed.path.isNotBlank())
        }
        assertEquals(SkillCatalog.featured.size, SkillCatalog.featured.map { it.id }.toSet().size)
    }
}
