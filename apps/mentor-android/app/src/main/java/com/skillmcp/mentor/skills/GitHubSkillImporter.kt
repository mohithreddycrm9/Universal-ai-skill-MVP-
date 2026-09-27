package com.skillmcp.mentor.skills

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ImportedSkill(
    val id: String,
    val owner: String,
    val repo: String,
    val ref: String,
    val title: String,
    val markdown: String,
)

class GitHubSkillImporter(
    private val http: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build(),
) {
    fun importFromRepoUrl(repoUrl: String): Result<ImportedSkill> =
        runCatching {
            val parsed = parseRepoUrl(repoUrl.trim())
            val paths = candidateSkillPaths(parsed)
            var markdown: String? = null
            var usedPath = "SKILL.md"
            for (path in paths) {
                val rawUrl = "https://raw.githubusercontent.com/${parsed.owner}/${parsed.repo}/${parsed.ref}/$path"
                val text = fetchText(rawUrl)
                if (text != null && text.contains("#")) {
                    markdown = text
                    usedPath = path
                    break
                }
            }
            val body = markdown ?: error("No SKILL.md found in ${parsed.owner}/${parsed.repo}@${parsed.ref}")
            val title = body.lineSequence().firstOrNull { it.startsWith("#") }?.removePrefix("#")?.trim()
                ?: "${parsed.owner}/${parsed.repo}"
            ImportedSkill(
                id = "${parsed.owner}/${parsed.repo}@${parsed.ref}" + if (parsed.path.isBlank()) "" else ":${parsed.path}",
                owner = parsed.owner,
                repo = parsed.repo,
                ref = parsed.ref,
                title = title,
                markdown = "# $title\n\nSource: $usedPath\n\n$body",
            )
        }

    internal data class ParsedRepo(val owner: String, val repo: String, val ref: String, val path: String = "")

    internal companion object {
        /**
         * Accepts `github.com/owner/repo`, `…/tree/<ref>` and `…/tree/<ref>/<folder>` (a skill folder
         * inside a larger repo, e.g. anthropics/skills/tree/main/skills/pdf).
         */
        fun parseRepoUrl(url: String): ParsedRepo {
            val cleaned = url.trim().removeSuffix("/")
            val githubRegex = Regex("""github\.com/([^/]+)/([^/]+)(?:/tree/([^/]+)(?:/(.+))?)?""")
            val match = githubRegex.find(cleaned)
                ?: throw IllegalArgumentException("Use a public repository URL (https://…/owner/repo)")
            val owner = match.groupValues[1]
            val repo = match.groupValues[2].removeSuffix(".git")
            val ref = match.groupValues[3].ifBlank { "main" }
            val path = match.groupValues[4].removeSuffix("/SKILL.md").trim('/')
            return ParsedRepo(owner, repo, ref, path)
        }

        fun candidateSkillPaths(parsed: ParsedRepo): List<String> =
            if (parsed.path.isNotBlank()) {
                listOf("${parsed.path}/SKILL.md")
            } else {
                listOf("SKILL.md", "skills/${parsed.repo}/SKILL.md", ".cursor/skills/${parsed.repo}/SKILL.md")
            }
    }

    private fun fetchText(url: String): String? {
        val response = http.newCall(Request.Builder().url(url).get().build()).execute()
        response.use {
            if (!it.isSuccessful) return null
            return it.body?.string()
        }
    }
}
