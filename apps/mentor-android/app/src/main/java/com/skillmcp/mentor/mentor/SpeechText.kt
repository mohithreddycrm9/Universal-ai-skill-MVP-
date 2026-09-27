package com.skillmcp.mentor.mentor

/** Turns a Markdown reply into text that sounds natural when read aloud. */
object SpeechText {
    private val fencedCode = Regex("""```[\s\S]*?(```|$)""")
    private val link = Regex("""\[([^\]]+)]\([^)]*\)""")
    private val emphasis = Regex("""(\*\*|__|\*|_|~~|`)""")
    private val heading = Regex("""(?m)^\s{0,3}#{1,6}\s*""")
    private val bullet = Regex("""(?m)^\s*(?:[-*•]|\d+[.)])\s+""")
    private val quote = Regex("""(?m)^\s*>\s?""")
    private val tableRule = Regex("""(?m)^\s*\|?\s*:?-{3,}.*$""")

    fun fromMarkdown(markdown: String, codePlaceholder: String = "(code)"): String =
        markdown
            .replace(fencedCode, " $codePlaceholder ")
            .replace(link, "$1")
            .replace(tableRule, "")
            .replace(heading, "")
            .replace(bullet, "")
            .replace(quote, "")
            .replace(emphasis, "")
            .replace("|", ", ")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
}
