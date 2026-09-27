package com.skillmcp.mentor.skills

import com.skillmcp.mentor.skills.finder.OfficialSkillPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillCatalogOfficialOnlyTest {
    @Test
    fun catalogHasNoCommunityEntries() {
        assertTrue(SkillCatalog.featured.none { "wshobson" in it.sourceUrl })
        SkillCatalog.featured.forEach { assertTrue(it.sourceUrl, SkillCatalog.isAllowed(it)) }
    }

    @Test
    fun everyNonFirstPartyEntryPassesTheFinderPolicy() {
        SkillCatalog.featured
            .filterNot { it.sourceUrl.startsWith("https://github.com/${SkillCatalog.FIRST_PARTY_OWNER}/") }
            .forEach { assertTrue(it.sourceUrl, OfficialSkillPolicy.verifyUrl(it.sourceUrl).accepted) }
    }

    @Test
    fun communityOrLookalikeEntriesAreFilteredFromTheUi() {
        val community = CatalogSkill("x", "X", "d", "Coding", "https://github.com/wshobson/agents/tree/main/plugins/ui-design/skills/x")
        val lookalike = CatalogSkill("y", "Y", "d", "Coding", "https://github.com/anthropic-skills/skills/tree/main/y")
        assertFalse(SkillCatalog.isAllowed(community))
        assertFalse(SkillCatalog.isAllowed(lookalike))
        assertEquals(SkillCatalog.featured.size, SkillCatalog.featuredForUi(emptyList()).size)
    }
}
