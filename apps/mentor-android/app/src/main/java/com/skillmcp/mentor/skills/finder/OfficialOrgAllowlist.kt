package com.skillmcp.mentor.skills.finder

/**
 * A GitHub organisation that belongs to a real company. [domains] are the company websites the
 * organisation profile must link to (its `blog` field) so a look-alike org cannot pass.
 */
data class OfficialOrg(
    val login: String,
    val company: String,
    val domains: List<String>,
    /** Company's official developer docs, shown when it has no verified official skill. */
    val docsUrl: String? = null,
    /** Extra words people use for the company in a request ("service now"). */
    val aliases: List<String> = emptyList(),
)

/**
 * Allowlist of company-owned GitHub organisations. Being listed here is necessary but not enough:
 * at fetch time the organisation must also be `type = Organization` and `is_verified = true`
 * (GitHub's verified-domain badge), and each repository must be a non-fork, non-archived,
 * non-mirror repository owned by that organisation. See [OfficialSkillPolicy].
 *
 * Matching is exact on the login (case-insensitive). "anthropic-skills", "notion" (a different
 * company) or any user account never match.
 */
object OfficialOrgAllowlist {
    val orgs: List<OfficialOrg> =
        listOf(
            OfficialOrg("anthropics", "Anthropic", listOf("anthropic.com")),
            OfficialOrg("openai", "OpenAI", listOf("openai.com")),
            OfficialOrg("google", "Google", listOf("opensource.google", "google.com")),
            OfficialOrg("google-gemini", "Google", listOf("google.dev", "google.com")),
            OfficialOrg("microsoft", "Microsoft", listOf("microsoft.com"), aliases = listOf("azure")),
            OfficialOrg("github", "GitHub", listOf("github.com")),
            OfficialOrg("cloudflare", "Cloudflare", listOf("cloudflare.com")),
            OfficialOrg("vercel", "Vercel", listOf("vercel.com")),
            OfficialOrg("vercel-labs", "Vercel", listOf("vercel.com")),
            OfficialOrg("stripe", "Stripe", listOf("stripe.com", "stripe.dev")),
            OfficialOrg("huggingface", "Hugging Face", listOf("huggingface.co"), aliases = listOf("huggingface")),
            OfficialOrg("figma", "Figma", listOf("figma.com")),
            OfficialOrg("makenotion", "Notion", listOf("notion.so", "notion.com")),
            OfficialOrg("supabase", "Supabase", listOf("supabase.com")),
            OfficialOrg("mongodb", "MongoDB", listOf("mongodb.com")),
            OfficialOrg("elastic", "Elastic", listOf("elastic.co")),
            OfficialOrg("aws", "AWS", listOf("amazon.com", "aws.amazon.com"), aliases = listOf("amazon web services")),
            OfficialOrg("awslabs", "AWS", listOf("amazon.com", "aws.amazon.com")),
            OfficialOrg("getsentry", "Sentry", listOf("sentry.io")),
            OfficialOrg("expo", "Expo", listOf("expo.dev")),
            // Checked 2026-09-27: both ServiceNow orgs exist but are not GitHub-verified
            // (is_verified=false), so the policy rejects them until GitHub verifies them.
            OfficialOrg("ServiceNow", "ServiceNow", listOf("servicenow.com"), "https://developer.servicenow.com", listOf("service now")),
            OfficialOrg("ServiceNowDevProgram", "ServiceNow", listOf("servicenow.com"), "https://developer.servicenow.com", listOf("service now")),
        )

    private val byLogin = orgs.associateBy { it.login.lowercase() }

    fun find(login: String): OfficialOrg? = byLogin[login.trim().lowercase()]

    /** All orgs of one company (e.g. Vercel: vercel + vercel-labs). */
    fun orgsOf(company: String): List<OfficialOrg> = orgs.filter { it.company.equals(company, ignoreCase = true) }

    fun companies(): List<String> = orgs.map { it.company }.distinct()
}
