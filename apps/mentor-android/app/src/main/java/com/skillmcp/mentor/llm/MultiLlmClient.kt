package com.skillmcp.mentor.llm

import com.skillmcp.mentor.mentor.ChatMessageDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MultiLlmClient(
    private val http: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .build(),
    private val moshi: Moshi = Moshi.Builder().build(),
) {
    private val json = "application/json; charset=utf-8".toMediaType()

    fun chat(
        profile: LlmProfile,
        systemPrompt: String,
        history: List<ChatMessageDto>,
        userMessage: String,
        extraContext: String = "",
    ): Result<LlmChatResult> =
        runCatching {
            val started = System.currentTimeMillis()
            val system = buildSystemPrompt(systemPrompt, extraContext)
            val result =
                when (profile.kind) {
                    LlmProviderKind.OPENAI_COMPAT -> openAiCompat(profile, system, history, userMessage)
                    LlmProviderKind.ANTHROPIC -> anthropic(profile, system, history, userMessage)
                    LlmProviderKind.GEMINI -> googleGenerative(profile, system, history, userMessage)
                    LlmProviderKind.OLLAMA -> ollama(profile, system, history, userMessage)
                }
            result.copy(latencyMs = System.currentTimeMillis() - started)
        }

    private fun buildSystemPrompt(systemPrompt: String, extraContext: String): String =
        buildString {
            append(systemPrompt)
            if (extraContext.isNotBlank()) {
                append("\n\n## Reference material\n")
                append(extraContext.take(12_000))
            }
        }

    private fun openAiCompat(
        profile: LlmProfile,
        system: String,
        history: List<ChatMessageDto>,
        userMessage: String,
    ): LlmChatResult {
        val base = profile.baseUrl.trimEnd('/') + "/"
        val url = "${base}chat/completions"
        val messages =
            JSONArray().apply {
                put(jsonMessage("system", system))
                history.takeLast(20).forEach { put(jsonMessage(it.role, it.content)) }
                put(jsonMessage("user", userMessage))
            }
        val body =
            JSONObject()
                .put("model", profile.model)
                .put("messages", messages)
                .put("temperature", 0.7)
                .toString()
        val responseText = postJson(url, body, profile) { builder ->
            if (profile.apiKey.isNotBlank()) {
                builder.header("Authorization", "Bearer ${profile.apiKey}")
            }
        }
        val json = JSONObject(responseText)
        val content =
            json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()
                ?: error("Empty OpenAI-compatible response")
        val usageJson = json.optJSONObject("usage")
        val usage =
            usageJson?.let {
                TokenUsage(
                    promptTokens = it.optInt("prompt_tokens"),
                    completionTokens = it.optInt("completion_tokens"),
                    totalTokens = it.optInt("total_tokens"),
                )
            }
        return LlmChatResult(content, usage, profile.model, 0)
    }

    private fun anthropic(
        profile: LlmProfile,
        system: String,
        history: List<ChatMessageDto>,
        userMessage: String,
    ): LlmChatResult {
        val url = profile.baseUrl.trimEnd('/') + "/messages"
        val messages =
            JSONArray().apply {
                history.takeLast(20).filter { it.role == "user" || it.role == "assistant" }.forEach {
                    put(
                        JSONObject()
                            .put("role", if (it.role == "assistant") "assistant" else "user")
                            .put(
                                "content",
                                JSONArray().put(JSONObject().put("type", "text").put("text", it.content)),
                            ),
                    )
                }
                put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "content",
                            JSONArray().put(JSONObject().put("type", "text").put("text", userMessage)),
                        ),
                )
            }
        val body =
            JSONObject()
                .put("model", profile.model)
                .put("max_tokens", 4096)
                .put("system", system)
                .put("messages", messages)
                .toString()
        val responseText =
            postJson(url, body, profile) { builder ->
                builder.header("x-api-key", profile.apiKey)
                builder.header("anthropic-version", "2023-06-01")
            }
        val json = JSONObject(responseText)
        val content =
            json.optJSONArray("content")
                ?.optJSONObject(0)
                ?.optString("text")
                ?.trim()
                ?: error("Empty Anthropic response")
        val usageJson = json.optJSONObject("usage")
        val usage =
            usageJson?.let {
                val input = it.optInt("input_tokens")
                val output = it.optInt("output_tokens")
                TokenUsage(input, output, input + output)
            }
        return LlmChatResult(content, usage, profile.model, 0)
    }

    private fun googleGenerative(
        profile: LlmProfile,
        system: String,
        history: List<ChatMessageDto>,
        userMessage: String,
    ): LlmChatResult {
        val base = profile.baseUrl.trimEnd('/') + "/"
        val modelPath = "models/${profile.model}:generateContent"
        val url = "${base}$modelPath?key=${profile.apiKey}"
        val contents = JSONArray()
        history.takeLast(20).forEach { msg ->
            val role = if (msg.role == "assistant") "model" else "user"
            contents.put(
                JSONObject()
                    .put("role", role)
                    .put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", msg.content)),
                    ),
            )
        }
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", userMessage))),
        )
        val body =
            JSONObject()
                .put(
                    "system_instruction",
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))),
                )
                .put("contents", contents)
                .toString()
        val responseText = postJson(url, body, profile) { }
        val json = JSONObject(responseText)
        val content =
            json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                ?.trim()
                ?: error("Empty generative API response")
        val usageMeta = json.optJSONObject("usageMetadata")
        val usage =
            usageMeta?.let {
                val prompt = it.optInt("promptTokenCount")
                val completion = it.optInt("candidatesTokenCount")
                TokenUsage(prompt, completion, prompt + completion)
            }
        return LlmChatResult(content, usage, profile.model, 0)
    }

    private fun ollama(
        profile: LlmProfile,
        system: String,
        history: List<ChatMessageDto>,
        userMessage: String,
    ): LlmChatResult {
        val base = profile.baseUrl.trimEnd('/') + "/"
        if (profile.baseUrl.contains("/v1")) {
            return openAiCompat(profile, system, history, userMessage)
        }
        val url = "${base}api/chat"
        val messages =
            JSONArray().apply {
                put(jsonMessage("system", system))
                history.takeLast(20).forEach { put(jsonMessage(it.role, it.content)) }
                put(jsonMessage("user", userMessage))
            }
        val body =
            JSONObject()
                .put("model", profile.model)
                .put("messages", messages)
                .put("stream", false)
                .toString()
        val responseText = postJson(url, body, profile) { }
        val json = JSONObject(responseText)
        val content = json.optJSONObject("message")?.optString("content")?.trim() ?: error("Empty Ollama response")
        return LlmChatResult(content, null, profile.model, 0)
    }

    private fun jsonMessage(role: String, content: String): JSONObject =
        JSONObject().put("role", role).put("content", content)

    private fun postJson(
        url: String,
        body: String,
        profile: LlmProfile,
        headers: (Request.Builder) -> Unit,
    ): String {
        val builder =
            Request.Builder()
                .url(url)
                .post(body.toRequestBody(json))
                .header("Content-Type", "application/json")
        headers(builder)
        http.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                error("${profile.kind.label} HTTP ${response.code}: ${text.take(600)}")
            }
            return text
        }
    }
}
