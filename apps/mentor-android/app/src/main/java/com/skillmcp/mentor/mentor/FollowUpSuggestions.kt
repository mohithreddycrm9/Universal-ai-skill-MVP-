package com.skillmcp.mentor.mentor

/** Follow-up chips shown under the latest reply. Labels/prompts are localized in the UI. */
enum class FollowUpKind { SHORTER, EXAMPLE, SIMPLER, NEXT_STEPS, TABLE, CHECKLIST, EXPLAIN_CODE, ADD_TESTS }

object FollowUpSuggestions {
    private const val LONG_REPLY_CHARS = 900
    private val listLine = Regex("""^\s*(?:[-*•]|\d+[.)])\s+\S""")

    fun forReply(reply: String, max: Int = 3): List<FollowUpKind> {
        val text = reply.trim()
        // Errors, empty replies and replies that ask the user something get no chips.
        if (text.isEmpty() || text.startsWith("⚠") || text.endsWith("?")) return emptyList()
        val hasCode = text.contains("```")
        val listLines = text.lines().count { listLine.containsMatchIn(it) }
        val out = mutableListOf<FollowUpKind>()
        when {
            hasCode -> out += listOf(FollowUpKind.EXPLAIN_CODE, FollowUpKind.ADD_TESTS)
            listLines >= 3 -> out += listOf(FollowUpKind.CHECKLIST, FollowUpKind.TABLE)
            else -> out += listOf(FollowUpKind.EXAMPLE, FollowUpKind.SIMPLER)
        }
        if (text.length > LONG_REPLY_CHARS) out.add(0, FollowUpKind.SHORTER)
        out += FollowUpKind.NEXT_STEPS
        return out.distinct().take(max)
    }
}
