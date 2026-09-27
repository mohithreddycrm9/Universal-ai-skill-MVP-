package com.skillmcp.mentor.mentor

/** Titles new chats from the first message, like the ChatGPT / Gemini apps (no "Chat 123" placeholders). */
object ChatTitle {
    const val MAX_CHARS = 42
    private val placeholder = Regex("""^(New chat|General|Chat \d{1,4})$""")

    fun isPlaceholder(name: String): Boolean = placeholder.matches(name.trim())

    fun fromFirstMessage(text: String): String? {
        val line =
            text.lineSequence()
                .map { it.trim().removePrefix(">").trim() }
                .firstOrNull { it.isNotEmpty() }
                ?.replace(Regex("""\s+"""), " ")
                ?: return null
        if (line.length <= MAX_CHARS) return line.trimEnd('.', ',', ':', ';')
        val cut = line.take(MAX_CHARS)
        val atWord = cut.substringBeforeLast(' ', cut).takeIf { it.length >= MAX_CHARS / 2 } ?: cut
        return atWord.trimEnd('.', ',', ':', ';', ' ') + "…"
    }
}
