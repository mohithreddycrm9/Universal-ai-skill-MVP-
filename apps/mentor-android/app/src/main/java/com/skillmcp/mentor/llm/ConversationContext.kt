package com.skillmcp.mentor.llm

import com.skillmcp.mentor.mentor.ChatMessageDto

/** Role stored for the "Switched to …" divider rows. They are UI-only and never sent to a model. */
const val ROLE_MODEL_SWITCH = "model_switch"

/** A stored chat turn as the history builder sees it (no Room types, so it is unit-testable). */
data class StoredTurn(
    val role: String,
    val content: String,
    /** Extracted attachment text (PDF) or a short note that an image was attached. */
    val attachmentText: String = "",
)

/**
 * Builds the history a (possibly different) model receives when a chat continues, then trims it to
 * that model's context window. Everything here is pure: no network, no Android, no cost.
 */
object ConversationContext {
    private const val FAILED_REPLY_PREFIX = "⚠️"

    /**
     * Stored turns -> provider-neutral history:
     * - keeps only user/assistant turns (switch dividers and failed "⚠️" replies are dropped),
     * - folds attachment text into the user turn so any model can read it,
     * - merges consecutive same-role turns (Anthropic and Gemini require strict alternation),
     * - drops leading assistant turns (Anthropic/Gemini history must start with the user).
     */
    fun neutralHistory(turns: List<StoredTurn>): List<ChatMessageDto> {
        val cleaned =
            turns
                .filter { it.role == "user" || it.role == "assistant" }
                .filterNot { it.role == "assistant" && it.content.trimStart().startsWith(FAILED_REPLY_PREFIX) }
                .map { t ->
                    val body =
                        if (t.role == "user" && t.attachmentText.isNotBlank()) {
                            t.content.trimEnd() + "\n\n" + t.attachmentText.trim()
                        } else {
                            t.content
                        }
                    ChatMessageDto(t.role, body)
                }
                .filter { it.content.isNotBlank() }
        val merged = mutableListOf<ChatMessageDto>()
        cleaned.forEach { m ->
            val last = merged.lastOrNull()
            if (last != null && last.role == m.role) {
                merged[merged.lastIndex] = last.copy(content = last.content + "\n\n" + m.content)
            } else {
                merged += m
            }
        }
        return merged.dropWhile { it.role != "user" }
    }

    /** Wire role for a provider: Gemini calls the assistant "model"; everyone else uses "assistant". */
    fun wireRole(kind: LlmProviderKind, role: String): String =
        when {
            role == "assistant" && kind == LlmProviderKind.GEMINI -> "model"
            role == "assistant" -> "assistant"
            else -> "user"
        }

    /** Rough, provider-agnostic token estimate (≈4 chars per token plus per-message overhead). */
    fun estimateTokens(text: String): Int = (text.length + 3) / 4 + 4

    /**
     * Conservative context window per model (tokens). Unknown models get a small safe default.
     * Ollama serves 4k by default unless the user raised num_ctx, so local models stay conservative.
     */
    fun contextWindowTokens(profile: LlmProfile): Int {
        val m = profile.model.lowercase()
        return when (profile.kind) {
            LlmProviderKind.ANTHROPIC -> 200_000
            LlmProviderKind.GEMINI -> 1_000_000
            LlmProviderKind.OPENAI_COMPAT ->
                when {
                    m.startsWith("gpt-4.1") -> 1_000_000
                    m.startsWith("gpt-4o") || m.startsWith("gpt-4-turbo") || m.startsWith("o1") ||
                        m.startsWith("o3") || m.startsWith("o4") || m.startsWith("gpt-5") -> 128_000
                    m.startsWith("gpt-3.5") -> 16_000
                    else -> 32_000
                }
            LlmProviderKind.HUGGING_FACE -> 8_192
            LlmProviderKind.OLLAMA -> 4_096
        }
    }

    data class Trimmed(
        val history: List<ChatMessageDto>,
        val droppedCount: Int,
        /** The outgoing user message (unanswered earlier user turns are folded in to keep alternation). */
        val userMessage: String = "",
    )

    /**
     * Keeps the system prompt and the newest turns that fit in [windowTokens] minus [reserveForReply].
     * Oldest turns go first; the kept history still starts with a user turn. The new user message is
     * always sent (the provider reports an error if even that is too large).
     */
    fun trimToWindow(
        systemPrompt: String,
        history: List<ChatMessageDto>,
        userMessage: String,
        windowTokens: Int,
        reserveForReply: Int = minOf(4_096, windowTokens / 4),
    ): Trimmed {
        var budget = windowTokens - reserveForReply - estimateTokens(systemPrompt) - estimateTokens(userMessage)
        val kept = ArrayDeque<ChatMessageDto>()
        for (m in history.asReversed()) {
            val cost = estimateTokens(m.content)
            if (cost > budget) break
            budget -= cost
            kept.addFirst(m)
        }
        while (kept.isNotEmpty() && kept.first().role != "user") kept.removeFirst()
        return Trimmed(kept.toList(), history.size - kept.size)
    }

    /** Full pipeline used before every send/regenerate. */
    fun forModel(
        profile: LlmProfile,
        systemPrompt: String,
        turns: List<StoredTurn>,
        userMessage: String,
    ): Trimmed {
        val neutral = neutralHistory(turns).toMutableList()
        // A user turn whose reply failed would sit next to the new user message; fold it into the new one.
        var outgoing = userMessage
        while (neutral.lastOrNull()?.role == "user") {
            outgoing = neutral.removeAt(neutral.lastIndex).content + "\n\n" + outgoing
        }
        val trimmed = trimToWindow(systemPrompt, neutral, outgoing, contextWindowTokens(profile))
        return trimmed.copy(userMessage = outgoing)
    }

    /** Short caption for a reply ("gpt-4o-mini", "llama3.2"), falling back to the profile name. */
    fun modelLabel(profile: LlmProfile): String = profile.model.trim().ifEmpty { profile.name }

    /** Note stored with a user turn that had an image, so later (text-only) models know about it. */
    fun imageNote(mimeType: String?): String = "[The user attached an image" + (mimeType?.let { " ($it)" } ?: "") + ".]"
}
