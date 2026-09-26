package com.skillmcp.mentor.llm

import com.skillmcp.mentor.mentor.ChatMessageDto

/**
 * Best-effort on-device replies when Gemini Nano / AICore is unavailable on the device.
 * Uses a concise local template so new users can chat without an API key.
 */
class OnDeviceLlmClient {
    fun chat(
        systemPrompt: String,
        history: List<ChatMessageDto>,
        userMessage: String,
    ): LlmChatResult {
        val trimmed = userMessage.trim()
        val reply =
            buildString {
                appendLine("_(On-device mode — no API key. For full quality, connect a cloud model in Models.)_")
                appendLine()
                when {
                    trimmed.endsWith("?") ->
                        append(
                            "Here’s a structured take on your question:\n" +
                                "1) Clarify the goal\n2) List 2–3 options\n3) Pick one small next step.\n\n" +
                                "You asked: \"$trimmed\"",
                        )
                    trimmed.length < 40 ->
                        append("Got it. Tell me a bit more context and I’ll outline concrete steps for: \"$trimmed\"")
                    else -> {
                        val words = trimmed.split(Regex("\\s+")).take(12).joinToString(" ")
                        append("Summary of what I heard: $words…\n\n")
                        append("Suggested next actions:\n")
                        append("• Break this into one task you can finish in 15 minutes\n")
                        append("• Note one risk or assumption to double-check\n")
                        append("• Ask me to expand any section")
                    }
                }
                if (history.isNotEmpty()) {
                    appendLine()
                    appendLine("_(I’m not using your full chat history on-device; cloud models do that better.)_")
                }
            }
        return LlmChatResult(
            content = reply.trim(),
            usage =
                TokenUsage(
                    promptTokens = (systemPrompt.length + userMessage.length) / 4,
                    completionTokens = reply.length / 4,
                    totalTokens = (systemPrompt.length + userMessage.length + reply.length) / 4,
                ),
            model = "gemini-nano-local-fallback",
            latencyMs = 0,
        )
    }
}
