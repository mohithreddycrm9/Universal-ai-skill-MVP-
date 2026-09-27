package com.skillmcp.mentor.skills.finder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class OfficialSkillRefresherTest {
    @get:Rule val tmp = TemporaryFolder()

    private val orgUrl = "https://api.github.com/orgs/anthropics"
    private val repoUrl = "https://api.github.com/repos/anthropics/skills"
    private val treeUrl = "https://api.github.com/repos/anthropics/skills/git/trees/main?recursive=1"
    private val newRawUrl = "https://raw.githubusercontent.com/anthropics/skills/main/skills/xlsx/SKILL.md"

    private val bundled =
        SkillIndexFile(
            generatedAt = "2026-09-27T00:00:00Z",
            organizations = listOf(FinderFixtures.anthropicsOrg),
            repositories = listOf(FinderFixtures.skillsRepo),
            skills =
                listOf(
                    FinderFixtures.pdfSkill,
                    FinderFixtures.pdfSkill.copy(
                        path = "skills/removed",
                        name = "removed",
                        skillUrl = "https://github.com/anthropics/skills/tree/main/skills/removed",
                        rawUrl = "https://raw.githubusercontent.com/anthropics/skills/main/skills/removed/SKILL.md",
                    ),
                ),
        )

    private val orgJson = """{"login":"anthropics","type":"Organization","name":"Anthropic","blog":"https://anthropic.com","html_url":"https://github.com/anthropics","is_verified":true}"""

    private fun repoJson(
        fork: Boolean = false,
        archived: Boolean = false,
        ownerType: String = "Organization",
    ) = """{"full_name":"anthropics/skills","owner":{"login":"anthropics","type":"$ownerType"},"fork":$fork,"archived":$archived,"private":false,"mirror_url":null,"html_url":"https://github.com/anthropics/skills","description":"Public repository for Agent Skills","default_branch":"main","license":{"spdx_id":"Apache-2.0"},"pushed_at":"2026-09-26T10:00:00Z"}"""

    private val treeJson = """{"truncated":false,"tree":[{"path":"skills/pdf/SKILL.md","type":"blob"},{"path":"skills/xlsx/SKILL.md","type":"blob"},{"path":"README.md","type":"blob"}]}"""
    private val xlsxMd = "---\nname: xlsx\ndescription: Work with spreadsheets.\n---\n# XLSX\n"

    private class FakeHttp(var routes: Map<String, () -> FetchResponse>) : SkillFinderHttp {
        val calls = mutableListOf<Pair<String, String?>>()

        override fun get(url: String, etag: String?): FetchResponse {
            calls += url to etag
            return routes[url]?.invoke() ?: FetchResponse(404, null)
        }
    }

    private fun ok(body: String, etag: String? = null, remaining: Int? = 50) = { FetchResponse(200, body, etag, remaining, 1_790_000_000L) }

    private fun liveRoutes(repo: String = repoJson()) =
        mapOf(
            orgUrl to ok(orgJson, "\"org-etag\""),
            repoUrl to ok(repo, "\"repo-etag\""),
            treeUrl to ok(treeJson, "\"tree-etag\""),
            newRawUrl to ok(xlsxMd, remaining = null),
        )

    private var now = 1_000_000_000_000L

    private fun refresher(http: FakeHttp) = OfficialSkillRefresher(http, SkillFinderCache(tmp.root.resolve("cache.json")), clock = { now })

    @Test
    fun liveRefreshUpdatesMetadataAddsNewAndDropsRemovedSkills() {
        val http = FakeHttp(liveRoutes())
        val outcome = refresher(http).refresh(bundled)
        assertEquals(SkillIndexSource.LIVE, outcome.source)
        val names = SkillIndexCodec.toOfficialSkills(outcome.index).map { it.name }
        assertEquals(listOf("pdf", "xlsx"), names)
        assertEquals("2026-09-26T10:00:00Z", outcome.index.repositories.single().pushedAt)
        assertEquals("Apache-2.0", outcome.index.repositories.single().license)
        assertTrue(http.calls.all { it.first.startsWith("https://") })
        assertEquals(4, http.calls.size)
    }

    @Test
    fun cacheIsReusedFor24HoursThenEtagsAreSent() {
        val http = FakeHttp(liveRoutes())
        refresher(http).refresh(bundled)
        http.calls.clear()

        now += 23L * 60 * 60 * 1000
        val cached = refresher(http).refresh(bundled)
        assertTrue("no network inside 24h", http.calls.isEmpty())
        assertEquals(SkillIndexSource.CACHE, cached.source)
        assertEquals(2, SkillIndexCodec.toOfficialSkills(cached.index).size)

        now += 2L * 60 * 60 * 1000
        http.routes = mapOf(orgUrl to { FetchResponse(304, null) }, repoUrl to { FetchResponse(304, null) }, treeUrl to { FetchResponse(304, null) })
        val revalidated = refresher(http).refresh(bundled)
        assertEquals(SkillIndexSource.LIVE, revalidated.source)
        assertEquals("\"org-etag\"", http.calls.first { it.first == orgUrl }.second)
        assertEquals("\"tree-etag\"", http.calls.first { it.first == treeUrl }.second)
        assertEquals(listOf("pdf", "xlsx"), SkillIndexCodec.toOfficialSkills(revalidated.index).map { it.name })
    }

    @Test
    fun rateLimitServesSavedListAndBacksOffUntilReset() {
        val resetSeconds = now / 1000 + 1800
        val http = FakeHttp(mapOf(orgUrl to { FetchResponse(403, null, null, 0, resetSeconds) }))
        val outcome = refresher(http).refresh(bundled)
        assertTrue(outcome.rateLimited)
        assertEquals(SkillIndexSource.BUNDLED, outcome.source)
        assertEquals(2, SkillIndexCodec.toOfficialSkills(outcome.index).size)

        http.calls.clear()
        now += 60_000
        assertTrue(refresher(http).refresh(bundled, force = true).rateLimited)
        assertTrue("no requests before the reset time", http.calls.isEmpty())
    }

    @Test
    fun lowRemainingQuotaStopsBeforeMoreApiCalls() {
        val http = FakeHttp(liveRoutes() + (orgUrl to ok(orgJson, remaining = 1)))
        val outcome = refresher(http).refresh(bundled)
        assertTrue(outcome.rateLimited)
        assertEquals(listOf(orgUrl), http.calls.map { it.first })
    }

    @Test
    fun repoThatBecomesForkArchivedOrUserOwnedDisappears() {
        listOf(repoJson(fork = true), repoJson(archived = true), repoJson(ownerType = "User")).forEach { repo ->
            tmp.root.resolve("cache.json").delete()
            val outcome = refresher(FakeHttp(liveRoutes(repo))).refresh(bundled)
            assertTrue(repo, SkillIndexCodec.toOfficialSkills(outcome.index).isEmpty())
        }
    }

    @Test
    fun orgThatLosesVerificationDisappears() {
        val unverified = orgJson.replace("\"is_verified\":true", "\"is_verified\":false")
        val outcome = refresher(FakeHttp(liveRoutes() + (orgUrl to ok(unverified)))).refresh(bundled)
        assertTrue(SkillIndexCodec.toOfficialSkills(outcome.index).isEmpty())
    }

    @Test
    fun networkFailureFallsBackGracefully() {
        val http = FakeHttp(mapOf(orgUrl to { throw IOException("offline") }))
        val outcome = refresher(http).refresh(bundled)
        assertTrue(outcome.failed)
        assertFalse(outcome.rateLimited)
        assertEquals(bundled, outcome.index)
    }

    @Test
    fun savedIndexSurvivesForOfflineUseAndIsReplacedByANewerBundle() {
        refresher(FakeHttp(liveRoutes())).refresh(bundled)
        val offline = refresher(FakeHttp(emptyMap())).cached(bundled)
        assertEquals(SkillIndexSource.CACHE, offline.source)
        assertEquals(2, offline.index.skills.size)

        val newerBundle = bundled.copy(generatedAt = "2099-01-01T00:00:00Z")
        assertEquals(SkillIndexSource.BUNDLED, refresher(FakeHttp(emptyMap())).cached(newerBundle).source)
    }

    @Test
    fun corruptCacheFileIsIgnored() {
        tmp.root.resolve("cache.json").writeText("{broken")
        assertEquals(SkillIndexSource.BUNDLED, refresher(FakeHttp(emptyMap())).cached(bundled).source)
    }
}
