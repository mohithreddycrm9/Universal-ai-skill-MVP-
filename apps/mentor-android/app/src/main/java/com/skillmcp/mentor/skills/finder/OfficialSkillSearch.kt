package com.skillmcp.mentor.skills.finder

/**
 * Local search over verified skills (port of the MCP server's catalog matching: every query term
 * must appear; name hits outrank description hits). A vendor word in a name never lifts a result
 * above its real ranking, and nothing unverified can be in [skills] to begin with.
 */
object OfficialSkillSearch {
    fun search(
        skills: List<OfficialSkill>,
        query: String,
        category: SkillCategory? = null,
        company: String? = null,
    ): List<OfficialSkill> {
        val terms = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        return skills
            .asSequence()
            .filter { category == null || it.category == category }
            .filter { company == null || it.company == company }
            .mapNotNull { skill ->
                val name = skill.name.lowercase()
                val haystack = "$name ${skill.description.lowercase()} ${skill.company.lowercase()} ${skill.repoFullName.lowercase()}"
                if (terms.any { it !in haystack }) return@mapNotNull null
                val q = terms.joinToString(" ")
                val score =
                    when {
                        terms.isEmpty() -> 0
                        name == q || name.replace('-', ' ') == q -> 0
                        name.startsWith(q) -> 1
                        terms.all { it in name } -> 2
                        else -> 3
                    }
                score to skill
            }
            .sortedWith(compareBy<Pair<Int, OfficialSkill>> { it.first }.thenBy { it.second.company }.thenBy { it.second.name })
            .map { it.second }
            .toList()
    }

    fun companies(skills: List<OfficialSkill>): List<String> = skills.map { it.company }.distinct().sorted()

    fun categories(skills: List<OfficialSkill>): List<SkillCategory> =
        SkillCategory.entries.filter { c -> skills.any { it.category == c } }
}
