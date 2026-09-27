package com.skillmcp.mentor.skills.finder

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** One HTTP response, with the headers the finder needs for ETags and GitHub rate limits. */
data class FetchResponse(
    val code: Int,
    val body: String?,
    val etag: String? = null,
    val rateRemaining: Int? = null,
    val rateResetEpochSeconds: Long? = null,
)

fun interface SkillFinderHttp {
    /** GET [url], sending `If-None-Match: [etag]` when present. Throws on network failure. */
    fun get(url: String, etag: String?): FetchResponse
}

/** HTTPS-only OkHttp client. Unauthenticated: no token is ever sent. */
class OkHttpSkillFinderHttp(
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(false)
            .build(),
) : SkillFinderHttp {
    override fun get(url: String, etag: String?): FetchResponse {
        require(url.startsWith("https://")) { "Skill Finder only fetches over HTTPS" }
        val builder =
            Request.Builder()
                .url(url)
                .header("User-Agent", "Lumina-SkillFinder")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("Accept", if (url.startsWith("https://api.github.com/")) "application/vnd.github+json" else "text/plain")
        if (!etag.isNullOrBlank()) builder.header("If-None-Match", etag)
        client.newCall(builder.get().build()).execute().use { response ->
            val body = if (response.code == 200) response.body?.string() else null
            return FetchResponse(
                code = response.code,
                body = body,
                etag = response.header("ETag"),
                rateRemaining = response.header("X-RateLimit-Remaining")?.toIntOrNull(),
                rateResetEpochSeconds = response.header("X-RateLimit-Reset")?.toLongOrNull(),
            )
        }
    }
}
