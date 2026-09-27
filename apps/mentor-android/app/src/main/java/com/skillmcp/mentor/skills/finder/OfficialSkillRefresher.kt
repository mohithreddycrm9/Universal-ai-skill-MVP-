package com.skillmcp.mentor.skills.finder

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.time.Instant
import java.time.temporal.ChronoUnit

@JsonClass(generateAdapter = true)
internal data class ApiOwner(val login: String = "", val type: String = "")

@JsonClass(generateAdapter = true)
internal data class ApiOrg(
    val login: String = "",
    val type: String = "",
    val name: String? = null,
    val blog: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "is_verified") val isVerified: Boolean? = null,
)

@JsonClass(generateAdapter = true)
internal data class ApiLicense(@Json(name = "spdx_id") val spdxId: String? = null)

@JsonClass(generateAdapter = true)
internal data class ApiRepo(
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
internal data class ApiTreeEntry(val path: String = "", val type: String = "")

@JsonClass(generateAdapter = true)
internal data class ApiTree(val tree: List<ApiTreeEntry> = emptyList(), val truncated: Boolean = false)

enum class SkillIndexSource { BUNDLED, CACHE, LIVE }

data class RefreshOutcome(
    val index: SkillIndexFile,
    val source: SkillIndexSource,
    val lastRefreshAt: Long,
    val rateLimited: Boolean = false,
    val failed: Boolean = false,
)

private class RateLimitedException : RuntimeException()

/**
 * Keeps the verified index fresh without an account: unauthenticated GitHub API (60 requests/hour),
 * one batch of ~3 requests per official repository at most once per 24 hours, `If-None-Match`
 * ETags (304 answers do not use quota), and a stop as soon as the quota runs low or GitHub
 * answers 403/429. Any failure leaves the last verified copy (cache, then bundled) in place.
 */
class OfficialSkillRefresher(
    private val http: SkillFinderHttp,
    private val cache: SkillFinderCache,
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxNewSkillFetches: Int = 25,
) {
    private val moshi = Moshi.Builder().build()
    private val orgAdapter = moshi.adapter(ApiOrg::class.java)
    private val repoAdapter = moshi.adapter(ApiRepo::class.java)
    private val treeAdapter = moshi.adapter(ApiTree::class.java)

    /** Best index available right now, without touching the network. */
    fun cached(bundled: SkillIndexFile): RefreshOutcome {
        val state = cache.read()
        // A newer app build ships a newer verified bundle; a cache refreshed from an older one is dropped.
        val cachedIndex = state.index?.takeIf { state.bundleGeneratedAt >= bundled.generatedAt }
        return if (cachedIndex != null) {
            RefreshOutcome(cachedIndex, SkillIndexSource.CACHE, state.lastRefreshAt)
        } else {
            RefreshOutcome(bundled, SkillIndexSource.BUNDLED, state.lastRefreshAt)
        }
    }

    fun refresh(bundled: SkillIndexFile, force: Boolean = false): RefreshOutcome {
        var state = cache.read()
        val base = cached(bundled)
        val now = clock()
        if (!force && state.lastRefreshAt > 0 && now - state.lastRefreshAt < CACHE_TTL_MS) return base
        if (now < state.rateLimitedUntil) return base.copy(rateLimited = true)

        val responses = state.responses.toMutableMap()
        var remaining = state.lastRateRemaining

        fun fetch(url: String): String? {
            require(url.startsWith("https://")) { "HTTPS only" }
            val isApi = url.startsWith("https://api.github.com/")
            if (isApi && (remaining ?: Int.MAX_VALUE) < MIN_REMAINING) throw RateLimitedException()
            val cached = responses[url]
            val response = http.get(url, cached?.etag)
            if (isApi && response.rateRemaining != null) remaining = response.rateRemaining
            return when {
                response.code == 304 && cached != null -> cached.body
                response.code == 200 && response.body != null -> {
                    responses[url] = CachedResponse(response.etag, response.body, now)
                    response.body
                }
                response.code == 429 || (response.code == 403 && response.rateRemaining == 0) -> {
                    val reset = response.rateResetEpochSeconds?.times(1000) ?: (now + RATE_LIMIT_BACKOFF_MS)
                    state = state.copy(rateLimitedUntil = reset)
                    throw RateLimitedException()
                }
                else -> null
            }
        }

        return try {
            val index = buildLiveIndex(base.index, ::fetch)
            state =
                state.copy(
                    responses = responses.filterValues { now - it.fetchedAt < RESPONSE_KEEP_MS },
                    lastRefreshAt = now,
                    lastRateRemaining = remaining,
                    index = index,
                    bundleGeneratedAt = bundled.generatedAt,
                )
            cache.write(state)
            RefreshOutcome(index, SkillIndexSource.LIVE, now)
        } catch (_: RateLimitedException) {
            cache.write(state.copy(responses = responses, lastRateRemaining = remaining))
            base.copy(rateLimited = true)
        } catch (_: Exception) {
            cache.write(state.copy(responses = responses, lastRateRemaining = remaining))
            base.copy(failed = true)
        }
    }

    private fun buildLiveIndex(
        base: SkillIndexFile,
        fetch: (String) -> String?,
    ): SkillIndexFile {
        val orgs =
            base.organizations
                .filter { OfficialOrgAllowlist.find(it.login) != null }
                .map { old ->
                    val live = fetch("https://api.github.com/orgs/${old.login}")?.let { runCatching { orgAdapter.fromJson(it) }.getOrNull() }
                    if (live == null || !live.login.equals(old.login, ignoreCase = true)) {
                        old
                    } else {
                        SkillIndexOrg(
                            login = live.login,
                            name = live.name ?: old.name,
                            type = live.type,
                            // The field is only omitted by the API, never "false" by omission; when
                            // absent we keep the value recorded when the index was verified.
                            isVerified = live.isVerified ?: old.isVerified,
                            blog = live.blog.orEmpty(),
                            htmlUrl = live.htmlUrl ?: old.htmlUrl,
                        )
                    }
                }
        val orgByLogin = orgs.associateBy { it.login.lowercase() }
        val repos = mutableListOf<SkillIndexRepo>()
        val skills = mutableListOf<SkillIndexSkill>()
        var newFetches = 0
        for (old in base.repositories) {
            val org = orgByLogin[old.owner.lowercase()] ?: continue
            val live = fetch("https://api.github.com/repos/${old.fullName}")?.let { runCatching { repoAdapter.fromJson(it) }.getOrNull() }
            val repo =
                if (live == null) {
                    old
                } else {
                    val facts = GitHubRepoFacts(live.fullName, live.owner.login, live.owner.type, live.fork, live.archived, live.private, live.mirrorUrl)
                    if (!OfficialSkillPolicy.verifyRepo(facts, SkillIndexCodec.orgFacts(org)).accepted ||
                        !live.fullName.equals(old.fullName, ignoreCase = true)
                    ) {
                        continue
                    }
                    SkillIndexRepo(
                        fullName = live.fullName,
                        owner = live.owner.login,
                        ownerType = live.owner.type,
                        htmlUrl = live.htmlUrl,
                        description = live.description.orEmpty(),
                        defaultBranch = live.defaultBranch,
                        license = live.license?.spdxId?.takeIf { it != "NOASSERTION" }.orEmpty(),
                        fork = live.fork,
                        archived = live.archived,
                        pushedAt = live.pushedAt.orEmpty(),
                    )
                }
            repos += repo
            val oldSkills = base.skills.filter { it.repo.equals(old.fullName, ignoreCase = true) }
            val tree =
                fetch("https://api.github.com/repos/${repo.fullName}/git/trees/${repo.defaultBranch}?recursive=1")
                    ?.let { runCatching { treeAdapter.fromJson(it) }.getOrNull() }
            if (tree == null || tree.truncated) {
                skills += oldSkills
                continue
            }
            val folders =
                tree.tree.filter { it.type == "blob" && it.path.endsWith("SKILL.md") }
                    .map { it.path.substringBeforeLast("/SKILL.md", missingDelimiterValue = "") }
                    .toSet()
            skills += oldSkills.filter { it.path in folders }
            val known = oldSkills.map { it.path }.toSet()
            val knownNames = oldSkills.map { it.name.lowercase() }.toMutableSet()
            for (folder in folders.filter { it !in known }.sorted()) {
                if (newFetches >= maxNewSkillFetches) break
                newFetches++
                val rawPath = if (folder.isBlank()) "SKILL.md" else "$folder/SKILL.md"
                val rawUrl = "https://raw.githubusercontent.com/${repo.fullName}/${repo.defaultBranch}/$rawPath"
                val text = runCatching { fetch(rawUrl) }.getOrNull() ?: continue
                val fm = SkillFrontMatter.parse(text)
                val name = fm["name"].orEmpty().ifBlank { folder.substringAfterLast('/') }
                val description = fm["description"].orEmpty().take(600)
                if (name.isBlank() || description.isBlank() || !knownNames.add(name.lowercase())) continue
                skills +=
                    SkillIndexSkill(
                        repo = repo.fullName,
                        path = folder,
                        name = name,
                        description = description,
                        license = fm["license"].orEmpty().take(120),
                        skillUrl = if (folder.isBlank()) repo.htmlUrl else "https://github.com/${repo.fullName}/tree/${repo.defaultBranch}/$folder",
                        rawUrl = rawUrl,
                    )
            }
        }
        return base.copy(
            generatedAt = Instant.ofEpochMilli(clock()).truncatedTo(ChronoUnit.SECONDS).toString(),
            organizations = orgs,
            repositories = repos,
            skills = skills,
        )
    }

    companion object {
        const val CACHE_TTL_MS: Long = 24L * 60 * 60 * 1000
        private const val RESPONSE_KEEP_MS: Long = 14L * 24 * 60 * 60 * 1000
        private const val RATE_LIMIT_BACKOFF_MS: Long = 60L * 60 * 1000
        private const val MIN_REMAINING = 3
    }
}
