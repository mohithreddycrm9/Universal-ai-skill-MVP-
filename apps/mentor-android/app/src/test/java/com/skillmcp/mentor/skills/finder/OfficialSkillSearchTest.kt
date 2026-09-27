package com.skillmcp.mentor.skills.finder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialSkillSearchTest {
    private val skills =
        listOf(
            FinderFixtures.officialSkill("pdf", "Read, fill and merge PDF documents."),
            FinderFixtures.officialSkill("pdf-forms", "Fill PDF forms.", company = "OpenAI", repo = "openai/skills"),
            FinderFixtures.officialSkill("docx", "Work with Word documents and PDF exports."),
            FinderFixtures.officialSkill("azure-kubernetes", "Plan AKS clusters.", company = "Microsoft", repo = "microsoft/skills"),
        )

    @Test
    fun nameMatchesOutrankDescriptionMatches() {
        assertEquals(listOf("pdf", "pdf-forms", "docx"), OfficialSkillSearch.search(skills, "pdf").map { it.name })
    }

    @Test
    fun everyTermMustMatch() {
        assertEquals(listOf("pdf-forms"), OfficialSkillSearch.search(skills, "fill forms").map { it.name })
        assertTrue(OfficialSkillSearch.search(skills, "pdf kubernetes").isEmpty())
    }

    @Test
    fun filtersByCategoryAndCompany() {
        assertEquals(listOf("azure-kubernetes"), OfficialSkillSearch.search(skills, "", category = SkillCategory.CLOUD).map { it.name })
        assertEquals(listOf("pdf-forms"), OfficialSkillSearch.search(skills, "", company = "OpenAI").map { it.name })
        assertEquals(listOf("Anthropic", "Microsoft", "OpenAI"), OfficialSkillSearch.companies(skills))
        assertEquals(listOf(SkillCategory.DOCUMENTS, SkillCategory.CLOUD), OfficialSkillSearch.categories(skills))
    }

    @Test
    fun noMatchIsEmpty() {
        assertTrue(OfficialSkillSearch.search(skills, "community-only-thing").isEmpty())
    }
}
