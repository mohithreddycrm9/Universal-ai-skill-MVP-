package com.skillmcp.mentor.mentor

/** A message that matched a drawer search. */
data class MessageSearchHit(
    val conversationId: String,
    val conversationName: String,
    val messageId: String,
    val snippet: String,
)

object SearchSnippet {
    /**
     * A one-line excerpt around the first case-insensitive match of [query], with "…" where text was
     * cut, so the drawer can show why a chat matched.
     */
    fun around(content: String, query: String, radius: Int = 48): String {
        val flat = content.replace(Regex("""\s+"""), " ").trim()
        val q = query.trim()
        if (q.isEmpty()) return flat.take(radius * 2)
        val i = flat.indexOf(q, ignoreCase = true)
        if (i < 0) return flat.take(radius * 2).let { if (flat.length > it.length) "$it…" else it }
        var start = (i - radius).coerceAtLeast(0)
        var end = (i + q.length + radius).coerceAtMost(flat.length)
        // Snap to word boundaries so words are not cut in half.
        if (start > 0) flat.indexOf(' ', start).takeIf { it in start until i }?.let { start = it + 1 }
        if (end < flat.length) flat.lastIndexOf(' ', end).takeIf { it > i + q.length }?.let { end = it }
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < flat.length) "…" else ""
        return prefix + flat.substring(start, end).trim() + suffix
    }

    /** Groups hits so each chat appears once (its newest matching message). */
    fun onePerConversation(hits: List<MessageSearchHit>): List<MessageSearchHit> =
        hits.distinctBy { it.conversationId }
}
