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
            val paths =
                listOf(
                    "SKILL.md",
                    "skills/${parsed.repo}/SKILL.md",
                    ".cursor/skills/${parsed.repo}/SKILL.md",
                )
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
                id = "${parsed.owner}/${parsed.repo}@${parsed.ref}",
                owner = parsed.owner,
                repo = parsed.repo,
                ref = parsed.ref,
                title = title,
                markdown = "# $title\n\nSource: $usedPath\n\n$body",
            )
        }

    private data class ParsedRepo(val owner: String, val repo: String, val ref: String)

    private fun parseRepoUrl(url: String): ParsedRepo {
        val cleaned = url.removeSuffix("/")
        val githubRegex = Regex("""github\.com/([^/]+)/([^/]+)(?:/tree/([^/]+))?""")
        val match = githubRegex.find(cleaned)
            ?: throw IllegalArgumentException("Use a public GitHub URL like https://github.com/owner/repo")
        val owner = match.groupValues[1]
        val repo = match.groupValues[2].removeSuffix(".git")
        val ref = match.groupValues[3].ifBlank { "main" }
        return ParsedRepo(owner, repo, ref)
    }

    private fun fetchText(url: String): String? {
        val response = http.newCall(Request.Builder().url(url).get().build()).execute()
        response.use {
            if (!it.isSuccessful) return null
            return it.body?.string()
        }
    }
}
