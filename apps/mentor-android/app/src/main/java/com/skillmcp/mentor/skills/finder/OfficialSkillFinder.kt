package com.skillmcp.mentor.skills.finder

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** What the finder can show right now. Every entry in [skills] passed [OfficialSkillPolicy]. */
data class SkillFinderSnapshot(
    val skills: List<OfficialSkill>,
    val source: SkillIndexSource,
    val indexGeneratedAt: String,
    val lastRefreshAt: Long,
    val rateLimited: Boolean = false,
    val failed: Boolean = false,
)

/**
 * App-side Skill Finder, a native port of the Universal Skills MCP discovery flow
 * (src/discovery/github.ts + allowlist catalogs + src/trust/publisher.ts) restricted to
 * official company organisations. No model or paid API is involved.
 */
class OfficialSkillFinder(
    private val context: Context,
    http: SkillFinderHttp = OkHttpSkillFinderHttp(),
    cacheFile: File = File(context.filesDir, "skill_finder/cache.json"),
    liveFile: File = File(context.filesDir, "skill_finder/live.json"),
) {
    private val refresher = OfficialSkillRefresher(http, SkillFinderCache(cacheFile))
    private val live = LiveOfficialSkillSearch(http, liveFile)

    @Volatile private var bundledIndex: SkillIndexFile? = null

    private fun bundled(): SkillIndexFile =
        bundledIndex ?: (
            runCatching {
                context.assets.open(ASSET).bufferedReader().use { SkillIndexCodec.parse(it.readText()) }
            }.getOrNull() ?: SkillIndexFile()
        ).also { bundledIndex = it }

    suspend fun cached(): SkillFinderSnapshot = withContext(Dispatchers.IO) { refresher.cached(bundled()).toSnapshot() }

    suspend fun refresh(force: Boolean = false): SkillFinderSnapshot =
        withContext(Dispatchers.IO) { refresher.refresh(bundled(), force).toSnapshot() }

    /** Live GitHub lookup for a build request (policy-checked; results join the local list). */
    suspend fun searchLive(intent: BuildIntent): LiveSearchResult =
        withContext(Dispatchers.IO) {
            val known = cached().skills.map { it.orgLogin }.distinct()
            live.search(intent, known)
        }

    private fun RefreshOutcome.toSnapshot() =
        SkillFinderSnapshot(
            skills = (SkillIndexCodec.toOfficialSkills(index) + live.cachedSkills()).distinctBy { it.id },
            source = source,
            indexGeneratedAt = index.generatedAt,
            lastRefreshAt = lastRefreshAt,
            rateLimited = rateLimited,
            failed = failed,
        )

    companion object {
        const val ASSET = "official_skills_index.json"
    }
}
