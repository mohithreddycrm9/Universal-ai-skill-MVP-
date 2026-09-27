package com.skillmcp.mentor.skills.finder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class LiveOfficialSkillSearchTest {
    @get:Rule val tmp = TemporaryFolder()

    private class FakeHttp(val routes: (String) -> FetchResponse?) : SkillFinderHttp {
        val calls = mutableListOf<String>()

        override fun get(url: String, etag: String?): FetchResponse {
            calls += url
            return routes(url) ?: FetchResponse(404, null)
        }
    }

    private var now = 1_800_000_000_000L

    private fun search(http: FakeHttp) = LiveOfficialSkillSearch(http, tmp.root.resolve("live.json"), clock = { now })

    private fun org(login: String, verified: Boolean, blog: String, type: String = "Organization") =
        """{"login":"$login","type":"$type","name":"$login","blog":"$blog","html_url":"https://github.com/$login","is_verified":$verified}"""

    private fun item(full: String, ownerType: String = "Organization", fork: Boolean = false, archived: Boolean = false) =
        """{"full_name":"$full","owner":{"login":"${full.substringBefore('/')}","type":"$ownerType"},"fork":$fork,"archived":$archived,"private":false,"html_url":"https://github.com/$full","description":"Skills","default_branch":"main","license":{"spdx_id":"Apache-2.0"},"pushed_at":"2026-09-20T00:00:00Z"}"""

    private val tree = """{"truncated":false,"tree":[{"path":"skills/workers/SKILL.md","type":"blob"}]}"""
    private val skillMd = "---\nname: workers\ndescription: Build and deploy Cloudflare Workers.\n---\n# Workers\n"

    private fun ok(body: String) = FetchResponse(200, body, rateRemaining = 50)

    private fun cloudflareRoutes(vararg items: String): (String) -> FetchResponse? = { url ->
        when {
            url == "https://api.github.com/orgs/cloudflare" -> ok(org("cloudflare", true, "https://www.cloudflare.com"))
            url.startsWith("https://api.github.com/search/repositories") -> ok("""{"items":[${items.joinToString(",")}]}""")
            url.startsWith("https://api.github.com/orgs/") -> ok(org(url.substringAfterLast('/'), false, "", type = "User"))
            url.contains("/git/trees/") -> ok(tree)
            url.startsWith("https://raw.githubusercontent.com/") -> ok(skillMd)
            else -> null
        }
    }

    private val cloudflareIntent = BuildIntent(listOf("workers"), "Cloudflare")

    @Test
    fun verifiedOrgResultPassesAndIsCachedForNextTime() {
        val http = FakeHttp(cloudflareRoutes(item("cloudflare/skills")))
        val live = search(http)
        val result = live.search(cloudflareIntent, emptyList())
        assertTrue(result.toString(), result is LiveSearchResult.Found)
        val skill = (result as LiveSearchResult.Found).skills.single()
        assertEquals("Cloudflare", skill.company)
        assertEquals("https://github.com/cloudflare/skills/tree/main/skills/workers", skill.skillUrl)
        assertEquals(listOf(skill.id), live.cachedSkills().map { it.id })
        assertTrue(http.calls.all { it.startsWith("https://") })
        assertEquals(1, http.calls.count { it.contains("/search/") })
    }

    @Test
    fun forksArchivedUserAccountsAndLookalikesAreRejected() {
        val http =
            FakeHttp(
                cloudflareRoutes(
                    item("cloudflare/skills-fork", fork = true),
                    item("cloudflare/old-skills", archived = true),
                    item("cloudflare-skills/skills", ownerType = "User"),
                ),
            )
        val result = search(http).search(cloudflareIntent, emptyList())
        assertEquals(LiveSearchResult.NoneFound("Cloudflare", null), result)
        assertTrue(http.calls.none { it.startsWith("https://raw.githubusercontent.com/") })
    }

    @Test
    fun unverifiedServiceNowOrgsGiveNoOfficialSkillWithDocsAndNoSearch() {
        val http =
            FakeHttp { url ->
                when (url) {
                    "https://api.github.com/orgs/ServiceNow" -> ok(org("ServiceNow", false, "https://www.servicenow.com"))
                    "https://api.github.com/orgs/ServiceNowDevProgram" -> ok(org("ServiceNowDevProgram", false, "https://developer.servicenow.com"))
                    else -> ok("""{"items":[${item("ServiceNowDevProgram/ServiceNow-SDK-CC-Starter")}]}""")
                }
            }
        val intent = BuildIntentDetector.detect("build a ServiceNow incident app")!!
        val result = search(http).search(intent, emptyList())
        assertEquals(LiveSearchResult.NoneFound("ServiceNow", "https://developer.servicenow.com"), result)
        assertTrue("no repository search for unverified orgs", http.calls.none { it.contains("/search/") })
    }

    @Test
    fun negativeResultIsCachedFor24Hours() {
        val http = FakeHttp(cloudflareRoutes())
        search(http).search(cloudflareIntent, emptyList())
        val first = http.calls.size
        now += 60_000
        assertTrue(search(http).search(cloudflareIntent, emptyList()) is LiveSearchResult.NoneFound)
        assertEquals("cached negative answer, no requests", first, http.calls.size)
        now += LiveOfficialSkillSearch.NEGATIVE_TTL_MS
        search(http).search(cloudflareIntent, emptyList())
        assertTrue(http.calls.size > first)
    }

    @Test
    fun rateLimitFallsBackAndBacksOff() {
        val http =
            FakeHttp { url ->
                if (url.contains("/search/")) FetchResponse(403, null, rateRemaining = 0, rateResetEpochSeconds = now / 1000 + 60) else cloudflareRoutes()(url)
            }
        assertEquals(LiveSearchResult.RateLimited, search(http).search(cloudflareIntent, emptyList()))
        val calls = http.calls.size
        now += 10_000
        assertEquals(LiveSearchResult.RateLimited, search(http).search(cloudflareIntent, emptyList()))
        assertEquals("waits for the reset time", calls, http.calls.size)
    }

    @Test
    fun onlyOneSearchPerFewSeconds() {
        val http = FakeHttp(cloudflareRoutes(item("cloudflare/skills")))
        search(http).search(cloudflareIntent, emptyList())
        now += 1_000
        assertEquals(LiveSearchResult.RateLimited, search(http).search(BuildIntent(listOf("pages"), "Cloudflare"), emptyList()))
    }

    @Test
    fun offlineIsGraceful() {
        val http = FakeHttp { throw IOException("offline") }
        assertEquals(LiveSearchResult.Offline, search(http).search(cloudflareIntent, emptyList()))
    }
}
