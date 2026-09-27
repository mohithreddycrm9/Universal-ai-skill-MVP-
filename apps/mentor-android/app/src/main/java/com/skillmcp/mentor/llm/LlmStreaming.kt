package com.skillmcp.mentor.llm

import com.skillmcp.mentor.mentor.ChatMessageDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import okio.source
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LlmStreaming(
    private val https: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .build(),
    private val ollamaHttp: OkHttpClient = OllamaHttpClient.create(30, 300),
) {
    private fun httpFor(profile: LlmProfile): OkHttpClient =
        if (profile.kind == LlmProviderKind.OLLAMA) ollamaHttp else https
    private val json = "application/json; charset=utf-8".toMediaType()

    fun streamChat(
        profile: LlmProfile,
        systemPrompt: String,
        history: List<ChatMessageDto>,
        userMessage: String,
        extraContext: String = "",
        vision: ChatVisionAttachment? = null,
        onChunk: (String) -> Unit,
        temperature: Double = 0.7,
        isCancelled: () -> Boolean = { false },
    ): Result<LlmChatResult> =
        runCatching {
            val started = System.currentTimeMillis()
            val system =
                buildString {
                    append(systemPrompt)
                    if (extraContext.isNotBlank()) {
                        append("\n\n## Reference material\n")
                        append(extraContext.take(12_000))
                    }
                }
            if (vision != null && !VisionCapabilities.supportsVision(profile)) {
                error("This model does not support image input. Connect OpenAI, Google, or Anthropic for vision.")
            }
            when (profile.kind) {
                LlmProviderKind.HUGGING_FACE ->
                    streamHuggingFace(profile, system, history, userMessage, onChunk, temperature, isCancelled)
                LlmProviderKind.OPENAI_COMPAT,
                LlmProviderKind.OLLAMA,
                -> streamOpenAiCompat(profile, system, history, userMessage, vision, onChunk, temperature, isCancelled)
                else -> {
                    val client = MultiLlmClient(https, ollamaHttp)
                    val result =
                        client.chat(profile, systemPrompt, history, userMessage, extraContext, vision).getOrThrow()
                    onChunk(result.content)
                    result.copy(latencyMs = System.currentTimeMillis() - started)
                }
            }.copy(latencyMs = System.currentTimeMillis() - started)
        }

    private fun streamHuggingFace(
        profile: LlmProfile,
        system: String,
        history: List<ChatMessageDto>,
        userMessage: String,
        onChunk: (String) -> Unit,
        temperature: Double,
        isCancelled: () -> Boolean = { false },
    ): LlmChatResult {
        val resolved =
            profile.copy(
                baseUrl = profile.baseUrl.ifBlank { HuggingFaceDefaults.ROUTER_BASE_URL },
            )
        return try {
            streamOpenAiCompat(resolved, system, history, userMessage, null, onChunk, temperature, isCancelled)
        } catch (routerError: Exception) {
            val client = MultiLlmClient(https, ollamaHttp)
            val result =
                client.chat(
                    profile = resolved,
                    systemPrompt = system,
                    history = history,
                    userMessage = userMessage,
                    vision = null,
                ).getOrElse { throw routerError }
            onChunk(result.content)
            result
        }
    }

    private fun streamOpenAiCompat(
        profile: LlmProfile,
        system: String,
        history: List<ChatMessageDto>,
        userMessage: String,
        vision: ChatVisionAttachment?,
        onChunk: (String) -> Unit,
        temperature: Double,
        isCancelled: () -> Boolean = { false },
    ): LlmChatResult {
        val resolvedBase =
            if (profile.kind == LlmProviderKind.HUGGING_FACE && profile.baseUrl.isBlank()) {
                HuggingFaceDefaults.ROUTER_BASE_URL
            } else {
                profile.baseUrl
            }
        val base = resolvedBase.trimEnd('/') + "/"
        val url =
            if (profile.kind == LlmProviderKind.OLLAMA && !base.contains("/v1")) {
                "${base}api/chat"
            } else {
                "${base}chat/completions"
            }
        val messages = ProviderMessages.openAi(system, history, userMessage, vision)
        val body =
            JSONObject()
                .put("model", profile.model)
                .put("messages", messages)
                .put("stream", true)
                .put("temperature", temperature)
                .toString()
        val builder =
            Request.Builder()
                .url(url)
                .post(body.toRequestBody(json))
                .header("Content-Type", "application/json")
        if (profile.apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer ${profile.apiKey}")
        }
        val full = StringBuilder()
        var promptTokens = 0
        var completionTokens = 0
        val call = httpFor(profile).newCall(builder.build())
        call.execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: ""
                error("Stream HTTP ${response.code}")
            }
            val source = response.body?.source()?.buffer ?: error("Empty stream body")
            while (!source.exhausted()) {
                if (isCancelled()) {
                    call.cancel()
                    break
                }
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload == "[DONE]") break
                val jsonLine = JSONObject(payload)
                if (profile.kind == LlmProviderKind.OLLAMA && !base.contains("/v1")) {
                    val chunk = jsonLine.optJSONObject("message")?.optString("content") ?: ""
                    if (chunk.isNotEmpty()) {
                        full.append(chunk)
                        onChunk(full.toString())
                    }
                } else {
                    val delta =
                        jsonLine.optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("delta")
                            ?.optString("content")
                            ?: ""
                    if (delta.isNotEmpty()) {
                        full.append(delta)
                        onChunk(full.toString())
                    }
                    jsonLine.optJSONObject("usage")?.let {
                        promptTokens = it.optInt("prompt_tokens", promptTokens)
                        completionTokens = it.optInt("completion_tokens", completionTokens)
                    }
                }
            }
        }
        val content = full.toString().trim()
        if (content.isEmpty()) error("Empty streamed response")
        val usage =
            if (promptTokens > 0 || completionTokens > 0) {
                TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens)
            } else {
                null
            }
        return LlmChatResult(content, usage, profile.model, 0)
    }
}
