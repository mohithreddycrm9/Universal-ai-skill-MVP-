package com.skillmcp.mentor.skills.finder

import com.skillmcp.mentor.skills.GitHubSkillImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SkillParsingTest {
    private fun index(
        repos: List<SkillIndexRepo>,
        skills: List<SkillIndexSkill>,
        orgs: List<SkillIndexOrg> = listOf(FinderFixtures.anthropicsOrg),
    ) = SkillIndexFile(generatedAt = "2026-09-27T00:00:00Z", organizations = orgs, repositories = repos, skills = skills)

    @Test
    fun frontMatterParsesPlainQuotedAndFoldedValues() {
        val md =
            """
            ---
            name: pdf
            description: >
              Read and fill PDF
              forms step by step.
            license: "Apache-2.0"
            ---
            # PDF
            """.trimIndent()
        val fm = SkillFrontMatter.parse(md)
        assertEquals("pdf", fm["name"])
        assertEquals("Read and fill PDF forms step by step.", fm["description"])
        assertEquals("Apache-2.0", fm["license"])
        assertTrue(SkillFrontMatter.parse("# No front matter").isEmpty())
    }

    @Test
    fun indexRoundTripsThroughJson() {
        val original = index(listOf(FinderFixtures.skillsRepo), listOf(FinderFixtures.pdfSkill))
        assertEquals(original, SkillIndexCodec.parse(SkillIndexCodec.encode(original)))
        assertNull(SkillIndexCodec.parse("{not json"))
    }

    @Test
    fun onlyPolicyPassingSkillsBecomeOfficial() {
        val fork = FinderFixtures.skillsRepo.copy(fullName = "anthropics/skills-fork", fork = true, htmlUrl = "https://github.com/anthropics/skills-fork")
        val skills =
            listOf(
                FinderFixtures.pdfSkill,
                FinderFixtures.pdfSkill.copy(repo = "anthropics/skills-fork", path = "x", skillUrl = "https://github.com/anthropics/skills-fork/tree/main/x", rawUrl = "https://raw.githubusercontent.com/anthropics/skills-fork/main/x/SKILL.md"),
                // Skill claims the official repo but links somewhere else.
                FinderFixtures.pdfSkill.copy(path = "y", skillUrl = "https://github.com/someone-personal/skills/tree/main/y"),
                FinderFixtures.pdfSkill.copy(path = "z", skillUrl = "http://github.com/anthropics/skills/tree/main/z"),
                FinderFixtures.pdfSkill.copy(path = "w", description = " "),
                // Repo not in the index at all.
                FinderFixtures.pdfSkill.copy(repo = "anthropics/other", path = "v"),
            )
        val result = SkillIndexCodec.toOfficialSkills(index(listOf(FinderFixtures.skillsRepo, fork), skills))
        assertEquals(listOf("anthropics/skills:skills/pdf"), result.map { it.id })
        val pdf = result.single()
        assertEquals("Anthropic", pdf.company)
        assertEquals(SkillCategory.DOCUMENTS, pdf.category)
        assertEquals("2026-09-24T16:20:39Z", pdf.lastUpdated)
    }

    @Test
    fun userOwnedAndUnverifiedOrgIndexesYieldNothing() {
        val userOrg = FinderFixtures.anthropicsOrg.copy(type = "User")
        assertTrue(SkillIndexCodec.toOfficialSkills(index(listOf(FinderFixtures.skillsRepo), listOf(FinderFixtures.pdfSkill), listOf(userOrg))).isEmpty())
        val unverified = FinderFixtures.anthropicsOrg.copy(isVerified = false)
        assertTrue(SkillIndexCodec.toOfficialSkills(index(listOf(FinderFixtures.skillsRepo), listOf(FinderFixtures.pdfSkill), listOf(unverified))).isEmpty())
    }

    @Test
    fun licenceFallsBackToRepositoryLicence() {
        val repo = FinderFixtures.skillsRepo.copy(license = "MIT")
        val skill = SkillIndexCodec.toOfficialSkills(index(listOf(repo), listOf(FinderFixtures.pdfSkill.copy(license = "")))).single()
        assertEquals("MIT", skill.license)
    }

    @Test
    fun installedIdMatchesTheImporterId() {
        val skill = SkillIndexCodec.toOfficialSkills(index(listOf(FinderFixtures.skillsRepo), listOf(FinderFixtures.pdfSkill))).single()
        val parsed = GitHubSkillImporter.parseRepoUrl(skill.skillUrl)
        assertEquals("${parsed.owner}/${parsed.repo}@${parsed.ref}:${parsed.path}", skill.installedSkillId)
    }

    @Test
    fun categoriesComeFromNameThenDescription() {
        assertEquals(SkillCategory.DOCUMENTS, SkillCategory.classify("pdf", "anything"))
        assertEquals(SkillCategory.SECURITY, SkillCategory.classify("security-threat-model", "Repository-grounded threat modeling"))
        assertEquals(SkillCategory.CLOUD, SkillCategory.classify("azure-kubernetes", "Plan AKS clusters"))
        assertEquals(SkillCategory.DESIGN, SkillCategory.classify("brand-guidelines", "Writing tone for the brand"))
        assertEquals(SkillCategory.CODING, SkillCategory.classify("yeet", "Commit and open a pull request with gh"))
        assertEquals(SkillCategory.GENERAL, SkillCategory.classify("define-goal", "Clarify what success looks like"))
    }

    @Test
    fun bundledIndexIsRealVerifiedAndFullyOfficial() {
        val file = File("src/main/assets/official_skills_index.json")
        val index = SkillIndexCodec.parse(file.readText())!!
        assertTrue(index.organizations.all { it.isVerified && it.type == "Organization" && OfficialOrgAllowlist.find(it.login) != null })
        assertTrue(index.repositories.all { !it.fork && !it.archived && it.ownerType == "Organization" })
        val official = SkillIndexCodec.toOfficialSkills(index)
        assertEquals("every bundled skill must pass the policy", index.skills.size, official.size)
        assertTrue(official.size >= 400)
        assertEquals(official.size, official.map { it.id }.toSet().size)
        assertTrue(official.all { it.skillUrl.startsWith("https://github.com/") })
        assertTrue(index.repositories.any { it.fullName == "anthropics/skills" })
    }
}
