package com.skillmcp.mentor.plugins

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object BuiltinToolHints {
    private val timeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d yyyy · HH:mm z")

    fun extraContextForMessage(userMessage: String, pluginRunner: PluginRunner): String {
        val parts = mutableListOf<String>()
        val lower = userMessage.lowercase()
        if (mentionsDateOrTime(lower)) {
            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            parts += "## Device date & time\n${now.format(timeFormatter)} (${zone.id})"
        }
        val mathExpr = extractSimpleMath(userMessage)
        if (mathExpr != null) {
            val result = pluginRunner.runBuiltInCalc(mathExpr)
            if (result.isNotBlank()) {
                parts += "## Built-in calculator\n$mathExpr = $result"
            }
        }
        return parts.joinToString("\n\n")
    }

    private fun mentionsDateOrTime(lower: String): Boolean {
        val keys =
            listOf(
                "what time",
                "what's the time",
                "what date",
                "today's date",
                "tomorrow",
                "yesterday",
                "timezone",
                "time zone",
                "what day is",
            )
        return keys.any { lower.contains(it) } || lower.startsWith("/time")
    }

    /** Picks a short arithmetic line like "15% of 240" or "2+2". */
    private fun extractSimpleMath(message: String): String? {
        val trimmed = message.trim()
        if (trimmed.startsWith("/")) return null
        val percent = Regex("""(\d+(?:\.\d+)?)\s*%\s*of\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE).find(trimmed)
        if (percent != null) {
            val (pct, base) = percent.destructured
            return "$base * ($pct / 100)"
        }
        val exprOnly = Regex("""^[\d\s+\-*/().%]+$""")
        if (trimmed.length <= 40 && exprOnly.matches(trimmed)) return trimmed
        return null
    }
}
