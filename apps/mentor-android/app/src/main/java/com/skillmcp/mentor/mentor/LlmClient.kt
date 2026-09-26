package com.skillmcp.mentor.mentor

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    val role: String,
    val content: String,
)

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val temperature: Double = 0.4,
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
    val choices: List<Choice> = emptyList(),
) {
    @JsonClass(generateAdapter = true)
    data class Choice(val message: ChatMessageDto?)
}

class LlmClient(
    private val http: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build(),
    private val moshi: Moshi = Moshi.Builder().build(),
) {
    private val json = "application/json; charset=utf-8".toMediaType()
    private val requestAdapter = moshi.adapter(ChatCompletionRequest::class.java)
    private val responseAdapter = moshi.adapter(ChatCompletionResponse::class.java)

    suspend fun chat(
        baseUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        history: List<ChatMessageDto>,
        userMessage: String,
        skillContext: String = "",
    ): Result<String> =
        runCatching {
            val normalizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = "${normalizedBase}chat/completions"
            val system =
                buildString {
                    append(systemPrompt)
                    if (skillContext.isNotBlank()) {
                        append("\n\n## Imported skills\n")
                        append(skillContext.take(12_000))
                    }
                }
            val messages =
                listOf(ChatMessageDto("system", system)) +
                    history.takeLast(20) +
                    ChatMessageDto("user", userMessage)

            val body =
                requestAdapter.toJson(ChatCompletionRequest(model = model, messages = messages))
                    .toRequestBody(json)

            val requestBuilder =
                Request.Builder()
                    .url(url)
                    .post(body)
                    .header("Content-Type", "application/json")

            if (apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }

            http.newCall(requestBuilder.build()).execute().use { response ->
                val text = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    error("LLM HTTP ${response.code}: ${text.take(500)}")
                }
                val parsed = responseAdapter.fromJson(text)
                parsed?.choices?.firstOrNull()?.message?.content?.trim()
                    ?: error("Empty LLM response")
            }
        }
}
