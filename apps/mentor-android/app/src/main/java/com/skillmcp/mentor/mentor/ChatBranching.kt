package com.skillmcp.mentor.mentor

/** Pure rules for editing a sent message and regenerating a reply (no Android or database types). */
object ChatBranching {
    /**
     * Editing a user message rewrites history from that point: the message and everything after it are
     * removed, then the edited text is sent as a new turn. Returns an empty list when [messageId] is not
     * a user message in [messages].
     */
    fun idsToDropForEdit(messages: List<UiMessage>, messageId: String): List<String> {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index < 0 || messages[index].role != "user") return emptyList()
        return messages.subList(index, messages.size).map { it.id }
    }

    data class RegenerateContext(
        val history: List<UiMessage>,
        val userMessage: UiMessage,
        val reply: UiMessage,
    )

    /** Only the latest reply can be regenerated; it needs the user turn right before it. */
    fun regenerateContext(messages: List<UiMessage>, assistantMessageId: String): RegenerateContext? {
        val reply = messages.lastOrNull() ?: return null
        if (reply.id != assistantMessageId || reply.role != "assistant") return null
        val userIndex = messages.indexOfLast { it.role == "user" }
        if (userIndex < 0 || userIndex != messages.lastIndex - 1) return null
        val history = messages.subList(0, userIndex).filter { it.role == "user" || it.role == "assistant" }
        return RegenerateContext(history, messages[userIndex], reply)
    }

    /** All versions after a regenerate: the first regenerate records the original reply as version 1. */
    fun versionsAfterRegenerate(existing: List<String>, current: String, regenerated: String): List<String> =
        if (existing.isEmpty()) listOf(current, regenerated) else existing + regenerated

    /** Which version is showing; falls back to the newest when the content was changed elsewhere. */
    fun visibleVersionIndex(versions: List<String>, current: String): Int {
        if (versions.isEmpty()) return 0
        val i = versions.lastIndexOf(current)
        return if (i >= 0) i else versions.lastIndex
    }
}
