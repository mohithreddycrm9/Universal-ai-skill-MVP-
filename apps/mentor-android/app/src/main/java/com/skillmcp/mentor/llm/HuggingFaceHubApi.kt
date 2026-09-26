package com.skillmcp.mentor.llm

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class HuggingFaceModelSummary(
    val id: String,
    val pipelineTag: String?,
    val downloads: Long,
    val likes: Int,
)

class HuggingFaceHubApi(
    private val http: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build(),
) {
    private val chatPipelineTags =
        setOf(
            "text-generation",
            "text2text-generation",
            "conversational",
            "image-text-to-text",
        )

    fun searchModels(
        query: String,
        accessToken: String = "",
        limit: Int = 24,
    ): Result<List<HuggingFaceModelSummary>> =
        runCatching {
            val q = query.trim()
            if (q.length < 2) return Result.success(emptyList())
            val encoded = URLEncoder.encode(q, Charsets.UTF_8.name())
            val url =
                "https://huggingface.co/api/models?search=$encoded&limit=$limit&sort=downloads&direction=-1"
            val builder =
                Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
            if (accessToken.isNotBlank()) {
                builder.header("Authorization", "Bearer $accessToken")
            }
            http.newCall(builder.build()).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    error("Hub search HTTP ${response.code}: ${body.take(300)}")
                }
                val array = JSONArray(body)
                buildList {
                    for (i in 0 until array.length()) {
                        val row = array.getJSONObject(i)
                        val id = row.optString("id").ifBlank { row.optString("modelId") }
                        if (id.isBlank()) continue
                        val tag = row.optString("pipeline_tag").ifBlank { null }
                        if (tag != null && tag !in chatPipelineTags) continue
                        add(
                            HuggingFaceModelSummary(
                                id = id,
                                pipelineTag = tag,
                                downloads = row.optLong("downloads"),
                                likes = row.optInt("likes"),
                            ),
                        )
                    }
                }
            }
        }
}
