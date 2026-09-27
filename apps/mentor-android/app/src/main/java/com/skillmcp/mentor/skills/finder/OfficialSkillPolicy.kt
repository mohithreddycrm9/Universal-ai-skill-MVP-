package com.skillmcp.mentor.skills.finder

import java.net.URI

/** Organisation facts from `GET /orgs/{org}`. */
data class GitHubOrgFacts(
    val login: String,
    val type: String,
    val isVerified: Boolean?,
    val blog: String,
)

/** Repository facts from `GET /repos/{owner}/{repo}`. */
data class GitHubRepoFacts(
    val fullName: String,
    val ownerLogin: String,
    val ownerType: String,
    val fork: Boolean,
    val archived: Boolean,
    val isPrivate: Boolean = false,
    val mirrorUrl: String? = null,
)

enum class RejectReason {
    NOT_ALLOWLISTED,
    OWNER_NOT_ORGANIZATION,
    ORG_NOT_VERIFIED,
    ORG_DOMAIN_MISMATCH,
    OWNER_MISMATCH,
    FORK,
    ARCHIVED,
    PRIVATE,
    MIRROR,
    INSECURE_URL,
    NOT_GITHUB,
}

data class PolicyVerdict(
    val org: OfficialOrg?,
    val reasons: List<RejectReason>,
) {
    val accepted: Boolean get() = org != null && reasons.isEmpty()
}

/** Strict official-only rule for the Skill Finder. Pure functions: no I/O. */
object OfficialSkillPolicy {
    private val githubHosts = setOf("github.com", "raw.githubusercontent.com", "api.github.com")

    fun verifyOrg(facts: GitHubOrgFacts): PolicyVerdict {
        val org = OfficialOrgAllowlist.find(facts.login) ?: return PolicyVerdict(null, listOf(RejectReason.NOT_ALLOWLISTED))
        val reasons = mutableListOf<RejectReason>()
        if (!facts.type.equals("Organization", ignoreCase = true)) reasons += RejectReason.OWNER_NOT_ORGANIZATION
        if (facts.isVerified != true) reasons += RejectReason.ORG_NOT_VERIFIED
        if (!blogMatches(facts.blog, org.domains)) reasons += RejectReason.ORG_DOMAIN_MISMATCH
        return PolicyVerdict(org, reasons)
    }

    fun verifyRepo(repo: GitHubRepoFacts, orgFacts: GitHubOrgFacts): PolicyVerdict {
        val orgVerdict = verifyOrg(orgFacts)
        val reasons = orgVerdict.reasons.toMutableList()
        val declaredOwner = repo.fullName.substringBefore('/')
        if (!repo.ownerLogin.equals(orgFacts.login, ignoreCase = true) ||
            !declaredOwner.equals(repo.ownerLogin, ignoreCase = true)
        ) {
            reasons += RejectReason.OWNER_MISMATCH
        }
        if (!repo.ownerType.equals("Organization", ignoreCase = true)) reasons += RejectReason.OWNER_NOT_ORGANIZATION
        if (repo.fork) reasons += RejectReason.FORK
        if (repo.archived) reasons += RejectReason.ARCHIVED
        if (repo.isPrivate) reasons += RejectReason.PRIVATE
        if (!repo.mirrorUrl.isNullOrBlank()) reasons += RejectReason.MIRROR
        return PolicyVerdict(orgVerdict.org, reasons.distinct())
    }

    /**
     * A skill or repository link is acceptable only over HTTPS on a GitHub host, with the first
     * path segment (the owner) exactly matching an allowlisted organisation.
     */
    fun verifyUrl(url: String): PolicyVerdict {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return PolicyVerdict(null, listOf(RejectReason.NOT_GITHUB))
        if (!uri.scheme.equals("https", ignoreCase = true)) return PolicyVerdict(null, listOf(RejectReason.INSECURE_URL))
        val host = uri.host?.lowercase() ?: return PolicyVerdict(null, listOf(RejectReason.NOT_GITHUB))
        if (host !in githubHosts || uri.port != -1 || uri.userInfo != null) {
            return PolicyVerdict(null, listOf(RejectReason.NOT_GITHUB))
        }
        val segments = uri.path.orEmpty().split('/').filter { it.isNotBlank() }
        val owner = if (host == "api.github.com") segments.getOrNull(1) else segments.firstOrNull()
        val org = owner?.let { OfficialOrgAllowlist.find(it) } ?: return PolicyVerdict(null, listOf(RejectReason.NOT_ALLOWLISTED))
        return PolicyVerdict(org, emptyList())
    }

    internal fun blogMatches(blog: String, domains: List<String>): Boolean {
        val trimmed = blog.trim()
        if (trimmed.isEmpty()) return false
        val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
        val host = runCatching { URI(withScheme).host }.getOrNull()?.lowercase()?.removePrefix("www.") ?: return false
        return domains.any { d -> host == d || host.endsWith(".$d") }
    }
}
