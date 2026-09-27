package com.skillmcp.mentor.skills.finder

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

/**
 * On-disk index format (assets/official_skills_index.json, produced by
 * tools/build_official_skill_index.py from live GitHub data, and the refreshed copy in the cache).
 */
@JsonClass(generateAdapter = true)
data class SkillIndexFile(
    val schema: Int = 1,
    val generatedAt: String = "",
    val organizations: List<SkillIndexOrg> = emptyList(),
    val repositories: List<SkillIndexRepo> = emptyList(),
    val skills: List<SkillIndexSkill> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class SkillIndexOrg(
    val login: String,
    val name: String = "",
    val type: String = "",
    val isVerified: Boolean = false,
    val blog: String = "",
    val htmlUrl: String = "",
)

@JsonClass(generateAdapter = true)
data class SkillIndexRepo(
    val fullName: String,
    val owner: String,
    val ownerType: String = "",
    val htmlUrl: String = "",
    val description: String = "",
    val defaultBranch: String = "main",
    val license: String = "",
    val fork: Boolean = false,
    val archived: Boolean = false,
    val pushedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class SkillIndexSkill(
    val repo: String,
    val path: String,
    val name: String,
    val description: String,
    val license: String = "",
    val skillUrl: String,
    val rawUrl: String,
)

/** A skill that passed [OfficialSkillPolicy]: safe to show with the verified badge. */
data class OfficialSkill(
    val id: String,
    val name: String,
    val description: String,
    val company: String,
    val orgLogin: String,
    val repoFullName: String,
    val repoUrl: String,
    val skillUrl: String,
    val license: String,
    val lastUpdated: String,
    val category: SkillCategory,
    /** Id the app's importer gives this skill once installed (owner/repo@ref:path). */
    val installedSkillId: String,
)

object SkillIndexCodec {
    private val moshi: Moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(SkillIndexFile::class.java)

    fun parse(json: String): SkillIndexFile? = runCatching { adapter.fromJson(json) }.getOrNull()

    fun encode(index: SkillIndexFile): String = adapter.toJson(index)

    fun orgFacts(org: SkillIndexOrg) = GitHubOrgFacts(org.login, org.type, org.isVerified, org.blog)

    fun repoFacts(repo: SkillIndexRepo) =
        GitHubRepoFacts(repo.fullName, repo.owner, repo.ownerType, repo.fork, repo.archived)

    /**
     * Turns an index into the list shown to the user. Every skill is re-checked here, so a stale or
     * tampered cache can never surface a fork, a user repo, an archived repo or a look-alike org.
     */
    fun toOfficialSkills(index: SkillIndexFile): List<OfficialSkill> {
        val orgs = index.organizations.associateBy { it.login.lowercase() }
        val acceptedRepos =
            index.repositories.mapNotNull { repo ->
                val org = orgs[repo.owner.lowercase()] ?: return@mapNotNull null
                val verdict = OfficialSkillPolicy.verifyRepo(repoFacts(repo), orgFacts(org))
                if (verdict.accepted && OfficialSkillPolicy.verifyUrl(repo.htmlUrl).accepted) {
                    repo.fullName.lowercase() to (repo to verdict.org!!)
                } else {
                    null
                }
            }.toMap()
        return index.skills.mapNotNull { skill ->
            val (repo, org) = acceptedRepos[skill.repo.lowercase()] ?: return@mapNotNull null
            val expectedPrefix = "https://github.com/${repo.fullName}/"
            val expectedRaw = "https://raw.githubusercontent.com/${repo.fullName}/"
            val urlsOk =
                (skill.skillUrl.startsWith(expectedPrefix) || skill.skillUrl == repo.htmlUrl) &&
                    skill.rawUrl.startsWith(expectedRaw) &&
                    OfficialSkillPolicy.verifyUrl(skill.skillUrl).accepted &&
                    OfficialSkillPolicy.verifyUrl(skill.rawUrl).accepted
            if (!urlsOk || skill.name.isBlank() || skill.description.isBlank()) return@mapNotNull null
            OfficialSkill(
                id = "${repo.fullName}:${skill.path}",
                name = skill.name.trim(),
                description = skill.description.trim(),
                company = org.company,
                orgLogin = repo.owner,
                repoFullName = repo.fullName,
                repoUrl = repo.htmlUrl,
                skillUrl = skill.skillUrl,
                license = skill.license.ifBlank { repo.license },
                lastUpdated = repo.pushedAt,
                category = SkillCategory.classify(skill.name, skill.description),
                installedSkillId = "${repo.fullName}@${repo.defaultBranch}" + if (skill.path.isBlank()) "" else ":${skill.path}",
            )
        }
    }
}
