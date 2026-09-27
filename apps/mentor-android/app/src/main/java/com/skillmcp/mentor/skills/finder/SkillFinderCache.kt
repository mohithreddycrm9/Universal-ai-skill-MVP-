package com.skillmcp.mentor.skills.finder

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.File

@JsonClass(generateAdapter = true)
data class CachedResponse(
    val etag: String? = null,
    val body: String,
    val fetchedAt: Long,
)

@JsonClass(generateAdapter = true)
data class SkillFinderCacheState(
    val responses: Map<String, CachedResponse> = emptyMap(),
    val lastRefreshAt: Long = 0L,
    val rateLimitedUntil: Long = 0L,
    val lastRateRemaining: Int? = null,
    val index: SkillIndexFile? = null,
    /** `generatedAt` of the bundled index the cached [index] was refreshed from. */
    val bundleGeneratedAt: String = "",
)

/** Small JSON file cache (ETags, response bodies, the last verified index) for offline use. */
class SkillFinderCache(private val file: File) {
    private val adapter = Moshi.Builder().build().adapter(SkillFinderCacheState::class.java)

    @Synchronized
    fun read(): SkillFinderCacheState =
        runCatching { if (file.exists()) adapter.fromJson(file.readText()) else null }.getOrNull() ?: SkillFinderCacheState()

    @Synchronized
    fun write(state: SkillFinderCacheState) {
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(adapter.toJson(state))
            if (!tmp.renameTo(file)) {
                file.writeText(tmp.readText())
                tmp.delete()
            }
        }
    }
}
