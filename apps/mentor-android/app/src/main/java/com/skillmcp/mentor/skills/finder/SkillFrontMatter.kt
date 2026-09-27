package com.skillmcp.mentor.skills.finder

/** Minimal YAML front-matter reader for SKILL.md (`name`, `description`, `license`). */
object SkillFrontMatter {
    fun parse(markdown: String): Map<String, String> {
        val lines = markdown.lines()
        if (lines.firstOrNull()?.trim() != "---") return emptyMap()
        val out = linkedMapOf<String, String>()
        var key: String? = null
        for (line in lines.drop(1)) {
            if (line.trim() == "---") break
            val k = key
            if (k != null && (line.startsWith(" ") || line.startsWith("\t"))) {
                out[k] = (out[k].orEmpty() + " " + line.trim()).trim()
                continue
            }
            val colon = line.indexOf(':')
            if (colon <= 0) continue
            key = line.substring(0, colon).trim()
            var value = line.substring(colon + 1).trim()
            if (value in setOf("|", ">", "|-", ">-")) value = ""
            out[key] = value.trim('"', '\'')
        }
        return out
    }
}
