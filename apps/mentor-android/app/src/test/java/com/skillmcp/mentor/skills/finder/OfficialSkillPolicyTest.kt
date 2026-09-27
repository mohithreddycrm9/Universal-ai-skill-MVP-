package com.skillmcp.mentor.skills.finder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialSkillPolicyTest {
    private val anthropics = GitHubOrgFacts("anthropics", "Organization", isVerified = true, blog = "https://anthropic.com")
    private val skillsRepo = GitHubRepoFacts("anthropics/skills", "anthropics", "Organization", fork = false, archived = false)

    @Test
    fun verifiedOrgRepoIsAccepted() {
        val verdict = OfficialSkillPolicy.verifyRepo(skillsRepo, anthropics)
        assertTrue(verdict.reasons.toString(), verdict.accepted)
        assertEquals("Anthropic", verdict.org?.company)
    }

    @Test
    fun forkIsRejected() {
        val verdict = OfficialSkillPolicy.verifyRepo(skillsRepo.copy(fork = true), anthropics)
        assertFalse(verdict.accepted)
        assertTrue(RejectReason.FORK in verdict.reasons)
    }

    @Test
    fun archivedIsRejected() {
        val verdict = OfficialSkillPolicy.verifyRepo(skillsRepo.copy(archived = true), anthropics)
        assertTrue(RejectReason.ARCHIVED in verdict.reasons)
    }

    @Test
    fun mirrorAndPrivateAreRejected() {
        val verdict = OfficialSkillPolicy.verifyRepo(skillsRepo.copy(mirrorUrl = "https://gitlab.com/x/y", isPrivate = true), anthropics)
        assertTrue(RejectReason.MIRROR in verdict.reasons)
        assertTrue(RejectReason.PRIVATE in verdict.reasons)
    }

    @Test
    fun personalAccountIsRejectedEvenWithAllowlistedName() {
        val userOwner = GitHubOrgFacts("anthropics", "User", isVerified = false, blog = "https://anthropic.com")
        val repo = skillsRepo.copy(ownerType = "User")
        val verdict = OfficialSkillPolicy.verifyRepo(repo, userOwner)
        assertFalse(verdict.accepted)
        assertTrue(RejectReason.OWNER_NOT_ORGANIZATION in verdict.reasons)
        assertTrue(RejectReason.ORG_NOT_VERIFIED in verdict.reasons)
    }

    @Test
    fun lookalikeUserRepoIsRejected() {
        val lookalike = GitHubOrgFacts("anthropic-skills", "User", isVerified = null, blog = "https://anthropic.com")
        val repo = GitHubRepoFacts("anthropic-skills/skills", "anthropic-skills", "User", fork = false, archived = false)
        val verdict = OfficialSkillPolicy.verifyRepo(repo, lookalike)
        assertFalse(verdict.accepted)
        assertTrue(RejectReason.NOT_ALLOWLISTED in verdict.reasons)
    }

    @Test
    fun lookalikeOrgNamesDoNotMatchTheAllowlist() {
        listOf("anthropic", "anthropics-ai", "openai-official", "g00gle", "notion", "micro-soft", "vercel_labs").forEach {
            assertEquals(it, null, OfficialOrgAllowlist.find(it))
        }
        assertEquals("Notion", OfficialOrgAllowlist.find("makenotion")?.company)
        assertEquals("Anthropic", OfficialOrgAllowlist.find("Anthropics")?.company)
    }

    @Test
    fun unverifiedOrMissingVerificationIsRejected() {
        assertTrue(RejectReason.ORG_NOT_VERIFIED in OfficialSkillPolicy.verifyOrg(anthropics.copy(isVerified = false)).reasons)
        assertTrue(RejectReason.ORG_NOT_VERIFIED in OfficialSkillPolicy.verifyOrg(anthropics.copy(isVerified = null)).reasons)
    }

    @Test
    fun orgWhoseWebsiteIsNotTheCompanyIsRejected() {
        val wrongBlog = anthropics.copy(blog = "https://anthropic.com.evil.example.net")
        assertTrue(RejectReason.ORG_DOMAIN_MISMATCH in OfficialSkillPolicy.verifyOrg(wrongBlog).reasons)
        assertTrue(RejectReason.ORG_DOMAIN_MISMATCH in OfficialSkillPolicy.verifyOrg(anthropics.copy(blog = "")).reasons)
        assertTrue(OfficialSkillPolicy.verifyOrg(anthropics.copy(blog = "www.anthropic.com")).accepted)
        assertTrue(OfficialSkillPolicy.blogMatches("http://amazon.com/aws/", listOf("amazon.com")))
        assertTrue(OfficialSkillPolicy.blogMatches("https://docs.sentry.io", listOf("sentry.io")))
        assertFalse(OfficialSkillPolicy.blogMatches("https://notsentry.io", listOf("sentry.io")))
    }

    @Test
    fun repoOwnedBySomeoneElseIsRejected() {
        val transferred = skillsRepo.copy(fullName = "anthropics/skills", ownerLogin = "someone-personal")
        assertTrue(RejectReason.OWNER_MISMATCH in OfficialSkillPolicy.verifyRepo(transferred, anthropics).reasons)
        val renamed = skillsRepo.copy(fullName = "someone-personal/skills")
        assertTrue(RejectReason.OWNER_MISMATCH in OfficialSkillPolicy.verifyRepo(renamed, anthropics).reasons)
    }

    @Test
    fun urlsMustBeHttpsGithubAndAllowlistedOwner() {
        assertTrue(OfficialSkillPolicy.verifyUrl("https://github.com/anthropics/skills/tree/main/skills/pdf").accepted)
        assertTrue(OfficialSkillPolicy.verifyUrl("https://raw.githubusercontent.com/openai/skills/main/skills/x/SKILL.md").accepted)
        assertTrue(OfficialSkillPolicy.verifyUrl("https://api.github.com/repos/google/skills").accepted)
        assertEquals(listOf(RejectReason.INSECURE_URL), OfficialSkillPolicy.verifyUrl("http://github.com/anthropics/skills").reasons)
        assertEquals(listOf(RejectReason.NOT_GITHUB), OfficialSkillPolicy.verifyUrl("https://github.com.evil.example.net/anthropics/skills").reasons)
        assertEquals(listOf(RejectReason.NOT_GITHUB), OfficialSkillPolicy.verifyUrl("https://gitlab.com/anthropics/skills").reasons)
        assertEquals(listOf(RejectReason.NOT_GITHUB), OfficialSkillPolicy.verifyUrl("https://user@github.com/anthropics/skills").reasons)
        assertEquals(listOf(RejectReason.NOT_ALLOWLISTED), OfficialSkillPolicy.verifyUrl("https://github.com/anthropic-skills/skills").reasons)
        assertEquals(listOf(RejectReason.NOT_ALLOWLISTED), OfficialSkillPolicy.verifyUrl("https://github.com/someone-personal/anthropics").reasons)
        assertFalse(OfficialSkillPolicy.verifyUrl("not a url").accepted)
    }
}
