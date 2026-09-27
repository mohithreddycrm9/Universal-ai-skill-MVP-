package com.skillmcp.mentor.skills.finder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BuildIntentSuggestionTest {
    private val skills = SkillIndexCodec.toOfficialSkills(SkillIndexCodec.parse(File("src/main/assets/official_skills_index.json").readText())!!)

    private fun top(message: String): List<OfficialSkill> {
        val intent = BuildIntentDetector.detect(message)
        assertNotNull(message, intent)
        return OfficialSkillMatcher.match(intent!!, skills)
    }

    @Test
    fun detectsEnglishBuildPhrases() {
        listOf(
            "Build a PDF report of my expenses",
            "can you make a Next.js app",
            "Create a slide deck for Monday",
            "generate an Excel spreadsheet of sales",
            "help me set up a Postgres database",
            "Deploy to Supabase",
            "I want to develop an Android app",
        ).forEach { assertNotNull(it, BuildIntentDetector.detect(it)) }
    }

    @Test
    fun detectsHindiTeluguTamilPhrases() {
        listOf(
            "मुझे एक पीडीएफ रिपोर्ट बनाओ",
            "एक प्रेजेंटेशन तैयार करो",
            "mujhe ek pdf report banao",
            "నాకు ఒక వెబ్‌సైట్ తయారు చేయి",
            "ஒரு இணையதளம் உருவாக்க உதவுங்கள்",
        ).forEach { assertNotNull(it, BuildIntentDetector.detect(it)) }
    }

    @Test
    fun ordinaryQuestionsAreNotBuildRequests() {
        listOf("What's the weather in Pune?", "Tell me a joke", "who won the match", "").forEach {
            assertNull(it, BuildIntentDetector.detect(it))
        }
    }

    @Test
    fun resolvesTheCompanyNamedInTheRequest() {
        assertEquals("ServiceNow", BuildIntentDetector.detect("build a ServiceNow app")?.company)
        assertEquals("ServiceNow", BuildIntentDetector.detect("create a service now catalog item")?.company)
        assertEquals("Supabase", BuildIntentDetector.detect("deploy to supabase")?.company)
        assertEquals("AWS", BuildIntentDetector.detect("build an app on amazon web services")?.company)
        assertNull(BuildIntentDetector.detect("build a pdf report")?.company)
    }

    @Test
    fun pdfFindsAnthropicPdf() {
        val result = top("Build a PDF report")
        assertEquals("anthropics/skills:skills/pdf", result.first().id)
        assertEquals("Anthropic", result.first().company)
        assertTrue(result.size in 1..3)
    }

    @Test
    fun nextJsFindsAVercelSkill() {
        val result = top("make a Next.js app")
        assertEquals("Vercel", result.first().company)
        assertEquals("vercel-labs/agent-skills", result.first().repoFullName)
    }

    @Test
    fun slideDeckFindsPptxAndSupabaseFindsSupabase() {
        assertEquals("pptx", top("create a slide deck").first().name)
        assertEquals("Supabase", top("deploy to Supabase").first().company)
        assertEquals("pdf", top("मुझे एक पीडीएफ रिपोर्ट बनाओ").first().name)
    }

    @Test
    fun serviceNowHasNoLocalOfficialSkillSoItSearchesGitHub() {
        assertTrue(skills.none { it.company == "ServiceNow" })
        val decision = SkillSuggestionEngine.decide("build a ServiceNow incident app", skills, true, emptySet(), emptySet())
        assertTrue(decision is SkillSuggestionDecision.SearchGitHub)
        assertEquals("ServiceNow", (decision as SkillSuggestionDecision.SearchGitHub).intent.company)
    }

    @Test
    fun engineRespectsToggleDismissalsAndActiveSkills() {
        assertEquals(SkillSuggestionDecision.None, SkillSuggestionEngine.decide("build a pdf report", skills, false, emptySet(), emptySet()))
        val ready = SkillSuggestionEngine.decide("build a pdf report", skills, true, emptySet(), emptySet()) as SkillSuggestionDecision.Ready
        assertTrue(ready.skills.size in 1..3)
        val first = ready.skills.first()
        val afterDismiss = SkillSuggestionEngine.decide("build a pdf report", skills, true, setOf(first.id), emptySet())
        if (afterDismiss is SkillSuggestionDecision.Ready) assertFalse(afterDismiss.skills.any { it.id == first.id })
        val afterActive = SkillSuggestionEngine.decide("build a pdf report", skills, true, emptySet(), setOf(first.installedSkillId))
        if (afterActive is SkillSuggestionDecision.Ready) assertFalse(afterActive.skills.any { it.id == first.id })
        // Everything dismissed: no card and no GitHub search (the user already answered).
        val all = ready.skills.map { it.id }.toSet() + skills.filter { it.name.contains("pdf") }.map { it.id }
        val none = SkillSuggestionEngine.decide("build a pdf report", skills.filter { it.id in all }, true, all, emptySet())
        assertEquals(SkillSuggestionDecision.None, none)
    }

    @Test
    fun policyIsRecheckedBeforeSuggesting() {
        val tampered =
            FinderFixtures.officialSkill("pdf", "PDF tools", company = "Anthropic", repo = "anthropic-skills/skills")
        val decision = SkillSuggestionEngine.decide("build a pdf report", listOf(tampered), true, emptySet(), emptySet())
        assertFalse(decision is SkillSuggestionDecision.Ready)
    }

    @Test
    fun dismissalMemoryIsPerChatAndBounded() {
        val memory = SkillSuggestionMemory()
        memory.dismiss("chat-a", "anthropics/skills:skills/pdf")
        assertEquals(setOf("anthropics/skills:skills/pdf"), memory.dismissedIn("chat-a"))
        assertTrue(memory.dismissedIn("chat-b").isEmpty())
        assertEquals(memory.entries(), SkillSuggestionMemory(memory.entries()).entries())
        repeat(SkillSuggestionMemory.MAX_ENTRIES + 50) { memory.dismiss("c", "s$it") }
        assertEquals(SkillSuggestionMemory.MAX_ENTRIES, memory.entries().size)
    }
}
