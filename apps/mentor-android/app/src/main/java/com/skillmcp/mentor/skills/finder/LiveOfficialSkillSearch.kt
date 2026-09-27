package com.skillmcp.mentor.skills.finder

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.File
import java.net.URLEncoder
import java.time.Instant
import java.time.temporal.ChronoUnit

sealed interface LiveSearchResult {
    /** Verified official skills found on GitHub (already added to the local cache). */
    data class Found(val skills: List<OfficialSkill>) : LiveSearchResult

    /** Nothing passed the policy. [docsUrl] is the company's official docs when known. */
    data class NoneFound(val company: String?, val docsUrl: String?) : LiveSearchResult

    data object RateLimited : LiveSearchResult

    data object Offline : LiveSearchResult
}

@JsonClass(generateAdapter = true)
internal data class ApiSearchItem(
    @Json(name = "full_name") val fullName: String = "",
    val owner: ApiOwner = ApiOwner(),
    val fork: Boolean = false,
    val archived: Boolean = false,
    val private: Boolean = false,
    @Json(name = "mirror_url") val mirrorUrl: String? = null,
    @Json(name = "html_url") val htmlUrl: String = "",
    val description: String? = null,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    val license: ApiLicense? = null,
    @Json(name = "pushed_at") val pushedAt: String? = null,
)

@JsonClass(generateAdapter = true)
internal data class ApiSearchResponse(val items: List<ApiSearchItem> = emptyList())

@JsonClass(generateAdapter = true)
data class LiveSearchCacheState(
    /** query key -> time of a search that found nothing official (kept 24h). */
    val negative: Map<String, Long> = emptyMap(),
    /** Skills found live; merged into the finder list so the next match is instant and offline. */
    val found: SkillIndexFile = SkillIndexFile(),
    val rateLimitedUntil: Long = 0L,
    val lastSearchAt: Long = 0L,
)

/**
 * Live GitHub lookup for a build request the local index can't answer. Unauthenticated: one
 * repository search per intent (the search API allows 10/min without a token), at most one search
 * every [MIN_SEARCH_INTERVAL_MS], negative results cached for 24h, and every result must pass the
 * same [OfficialSkillPolicy] as the bundled index: verified org on the company's domain, non-fork,
 * non-archived repo owned by the org, SKILL.md reachable over HTTPS.
 */
class LiveOfficialSkillSearch(
    private val http: SkillFinderHttp,
    private val cacheFile: File,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val moshi = Moshi.Builder().build()
    private val stateAdapter = moshi.adapter(LiveSearchCacheState::class.java)
    private val orgAdapter = moshi.adapter(ApiOrg::class.java)
    private val searchAdapter = moshi.adapter(ApiSearchResponse::class.java)
    private val treeAdapter = moshi.adapter(ApiTree::class.java)

    private class RateLimited : RuntimeException()

    @Synchronized
    fun state(): LiveSearchCacheState =
        runCatching { if (cacheFile.exists()) stateAdapter.fromJson(cacheFile.readText()) else null }.getOrNull() ?: LiveSearchCacheState()

    @Synchronized
    private fun save(state: LiveSearchCacheState) {
        runCatching {
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(stateAdapter.toJson(state))
        }
    }

    /** Skills found by earlier live searches (policy re-applied on every read). */
    fun cachedSkills(): List<OfficialSkill> = SkillIndexCodec.toOfficialSkills(state().found)

    fun search(
        intent: BuildIntent,
        knownVerifiedOrgs: List<String>,
    ): LiveSearchResult {
        val now = clock()
        var state = state()
        val docsUrl = intent.company?.let { c -> OfficialOrgAllowlist.orgsOf(c).firstNotNullOfOrNull { it.docsUrl } }
        val key = (intent.company.orEmpty() + "|" + intent.terms.sorted().joinToString(" ")).lowercase()
        state.negative[key]?.let { at -> if (now - at < NEGATIVE_TTL_MS) return LiveSearchResult.NoneFound(intent.company, docsUrl) }
        if (now < state.rateLimitedUntil || now - state.lastSearchAt < MIN_SEARCH_INTERVAL_MS) return LiveSearchResult.RateLimited

        fun get(url: String): String? {
            require(url.startsWith("https://")) { "HTTPS only" }
            val r = http.get(url, null)
            return when {
                r.code == 200 -> r.body
                r.code == 429 || (r.code == 403 && r.rateRemaining == 0) -> {
                    state = state.copy(rateLimitedUntil = r.rateResetEpochSeconds?.times(1000) ?: (now + 60_000))
                    throw RateLimited()
                }
                else -> null
            }
        }

        return try {
            // 1. Resolve the company to org logins and keep only orgs that pass the org policy now.
            val candidates =
                if (intent.company != null) {
                    OfficialOrgAllowlist.orgsOf(intent.company).map { it.login }
                } else {
                    knownVerifiedOrgs.filter { OfficialOrgAllowlist.find(it) != null }
                }
            val verifiedOrgs = mutableMapOf<String, ApiOrg>()
            if (intent.company != null) {
                for (login in candidates.take(3)) {
                    val org = get("https://api.github.com/orgs/$login")?.let { runCatching { orgAdapter.fromJson(it) }.getOrNull() } ?: continue
                    if (OfficialSkillPolicy.verifyOrg(org.facts()).accepted) verifiedOrgs[org.login.lowercase()] = org
                }
                if (verifiedOrgs.isEmpty()) return none(state, key, now, intent.company, docsUrl)
            }
            val searchOrgs = if (intent.company != null) verifiedOrgs.values.map { it.login } else candidates
            if (searchOrgs.isEmpty()) return none(state, key, now, intent.company, docsUrl)

            // 2. One repository search, scoped to those orgs (forks/archived excluded server-side too).
            state = state.copy(lastSearchAt = now)
            val words = intent.terms.take(3).joinToString(" ")
            val q = "$words skills ${searchOrgs.joinToString(" ") { "org:$it" }} fork:false archived:false".trim()
            val items =
                get("https://api.github.com/search/repositories?per_page=5&q=" + URLEncoder.encode(q, "UTF-8"))
                    ?.let { runCatching { searchAdapter.fromJson(it) }.getOrNull()?.items }
                    .orEmpty()
                    .ifEmpty {
                        // Topic words can be too narrow; retry once with just the org scope.
                        if (words.isBlank()) {
                            emptyList()
                        } else {
                            val broad = "skills ${searchOrgs.joinToString(" ") { "org:$it" }} fork:false archived:false"
                            get("https://api.github.com/search/repositories?per_page=5&q=" + URLEncoder.encode(broad, "UTF-8"))
                                ?.let { runCatching { searchAdapter.fromJson(it) }.getOrNull()?.items }
                                .orEmpty()
                        }
                    }

            // 3. Strict policy on every result, then SKILL.md over HTTPS.
            val orgs = mutableListOf<SkillIndexOrg>()
            val repos = mutableListOf<SkillIndexRepo>()
            val skills = mutableListOf<SkillIndexSkill>()
            for (item in items.take(3)) {
                val login = item.owner.login
                val org =
                    verifiedOrgs[login.lowercase()]
                        ?: get("https://api.github.com/orgs/$login")?.let { runCatching { orgAdapter.fromJson(it) }.getOrNull() }
                        ?: continue
                val facts = GitHubRepoFacts(item.fullName, item.owner.login, item.owner.type, item.fork, item.archived, item.private, item.mirrorUrl)
                if (!OfficialSkillPolicy.verifyRepo(facts, org.facts()).accepted) continue
                val tree =
                    get("https://api.github.com/repos/${item.fullName}/git/trees/${item.defaultBranch}?recursive=1")
                        ?.let { runCatching { treeAdapter.fromJson(it) }.getOrNull() } ?: continue
                val folders =
                    tree.tree.filter { it.type == "blob" && it.path.endsWith("SKILL.md") }
                        .map { it.path.substringBeforeLast("/SKILL.md", missingDelimiterValue = "") }
                val ranked = folders.sortedByDescending { f -> intent.terms.count { f.lowercase().contains(it) } }.take(3)
                val repoSkills =
                    ranked.mapNotNull { folder ->
                        val rawPath = if (folder.isBlank()) "SKILL.md" else "$folder/SKILL.md"
                        val rawUrl = "https://raw.githubusercontent.com/${item.fullName}/${item.defaultBranch}/$rawPath"
                        val text = runCatching { get(rawUrl) }.getOrNull() ?: return@mapNotNull null
                        val fm = SkillFrontMatter.parse(text)
                        val description = fm["description"].orEmpty().take(600)
                        if (description.isBlank()) return@mapNotNull null
                        SkillIndexSkill(
                            repo = item.fullName,
                            path = folder,
                            name = fm["name"].orEmpty().ifBlank { folder.substringAfterLast('/').ifBlank { item.fullName.substringAfter('/') } },
                            description = description,
                            license = fm["license"].orEmpty().take(120),
                            skillUrl = if (folder.isBlank()) item.htmlUrl else "https://github.com/${item.fullName}/tree/${item.defaultBranch}/$folder",
                            rawUrl = rawUrl,
                        )
                    }
                if (repoSkills.isEmpty()) continue
                orgs += SkillIndexOrg(org.login, org.name ?: org.login, org.type, org.isVerified == true, org.blog.orEmpty(), org.htmlUrl.orEmpty())
                repos +=
                    SkillIndexRepo(
                        fullName = item.fullName,
                        owner = item.owner.login,
                        ownerType = item.owner.type,
                        htmlUrl = item.htmlUrl,
                        description = item.description.orEmpty(),
                        defaultBranch = item.defaultBranch,
                        license = item.license?.spdxId?.takeIf { it != "NOASSERTION" }.orEmpty(),
                        fork = item.fork,
                        archived = item.archived,
                        pushedAt = item.pushedAt.orEmpty(),
                    )
                skills += repoSkills
            }
            val found = SkillIndexCodec.toOfficialSkills(SkillIndexFile(organizations = orgs, repositories = repos, skills = skills))
            if (found.isEmpty()) return none(state, key, now, intent.company, docsUrl)
            val merged =
                state.found.copy(
                    generatedAt = Instant.ofEpochMilli(now).truncatedTo(ChronoUnit.SECONDS).toString(),
                    organizations = (state.found.organizations + orgs).distinctBy { it.login.lowercase() },
                    repositories = (state.found.repositories + repos).distinctBy { it.fullName.lowercase() },
                    skills = (state.found.skills + skills).distinctBy { "${it.repo}:${it.path}".lowercase() },
                )
            save(state.copy(found = merged))
            LiveSearchResult.Found(found)
        } catch (_: RateLimited) {
            save(state)
            LiveSearchResult.RateLimited
        } catch (_: Exception) {
            save(state)
            LiveSearchResult.Offline
        }
    }

    private fun none(
        state: LiveSearchCacheState,
        key: String,
        now: Long,
        company: String?,
        docsUrl: String?,
    ): LiveSearchResult {
        save(state.copy(negative = state.negative.filterValues { now - it < NEGATIVE_TTL_MS } + (key to now)))
        return LiveSearchResult.NoneFound(company, docsUrl)
    }

    private fun ApiOrg.facts() = GitHubOrgFacts(login, type, isVerified, blog.orEmpty())

    companion object {
        const val NEGATIVE_TTL_MS: Long = 24L * 60 * 60 * 1000
        const val MIN_SEARCH_INTERVAL_MS: Long = 6_000
    }
}
