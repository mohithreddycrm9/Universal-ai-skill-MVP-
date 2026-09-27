package com.skillmcp.mentor.data

/** One turn in an export. */
data class ExportTurn(val role: String, val content: String)

/** Pure formatting for sharing a chat through the Android share sheet (Markdown or plain text). */
object ChatExportFormat {
    fun markdown(
        title: String,
        turns: List<ExportTurn>,
        exportedOn: String,
        userLabel: String,
        assistantLabel: String,
    ): String =
        buildString {
            appendLine("# ${title.trim().ifEmpty { assistantLabel }}")
            appendLine()
            appendLine("_${exportedOn}_")
            turns.filter { it.content.isNotBlank() }.forEach { turn ->
                appendLine()
                appendLine("**${label(turn.role, userLabel, assistantLabel)}**")
                appendLine()
                appendLine(turn.content.trim())
            }
        }.trimEnd() + "\n"

    /** Plain text for apps that don't render Markdown: no # or ** markers, blank line between turns. */
    fun plainText(
        title: String,
        turns: List<ExportTurn>,
        userLabel: String,
        assistantLabel: String,
    ): String =
        buildString {
            appendLine(title.trim().ifEmpty { assistantLabel })
            turns.filter { it.content.isNotBlank() }.forEach { turn ->
                appendLine()
                appendLine("${label(turn.role, userLabel, assistantLabel)}:")
                appendLine(turn.content.trim())
            }
        }.trimEnd() + "\n"

    private fun label(role: String, user: String, assistant: String): String =
        if (role == "user") user else assistant
}
