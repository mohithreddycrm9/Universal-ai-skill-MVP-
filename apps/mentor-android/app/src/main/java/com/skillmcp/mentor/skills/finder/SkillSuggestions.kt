package com.skillmcp.mentor.skills.finder

/** What the chat should show for one user message. */
sealed interface SkillSuggestionDecision {
    data object None : SkillSuggestionDecision

    /** 1-3 verified skills from the local (bundled + cached) index. */
    data class Ready(val skills: List<OfficialSkill>) : SkillSuggestionDecision

    /** No local match: look on GitHub (live, policy-checked). */
    data class SearchGitHub(val intent: BuildIntent) : SkillSuggestionDecision
}

/**
 * Pure decision logic for "get the official skill ready" suggestions. No model call. Every
 * candidate is re-checked against [OfficialSkillPolicy] even though the list is already filtered.
 */
object SkillSuggestionEngine {
    fun decide(
        message: String,
        skills: List<OfficialSkill>,
        enabled: Boolean,
        dismissed: Set<String>,
        activeSkillIds: Set<String>,
        max: Int = 3,
    ): SkillSuggestionDecision {
        if (!enabled) return SkillSuggestionDecision.None
        val intent = BuildIntentDetector.detect(message) ?: return SkillSuggestionDecision.None
        val eligible =
            skills.filter {
                it.id !in dismissed &&
                    it.installedSkillId !in activeSkillIds &&
                    OfficialSkillPolicy.verifyUrl(it.skillUrl).accepted &&
                    OfficialSkillPolicy.verifyUrl(it.repoUrl).accepted
            }
        val matches = OfficialSkillMatcher.match(intent, eligible, max.coerceIn(1, 3))
        if (matches.isNotEmpty()) return SkillSuggestionDecision.Ready(matches)
        // Already-dismissed or already-active matches mean the user has answered: don't search again.
        if (OfficialSkillMatcher.match(intent, skills, 1).isNotEmpty()) return SkillSuggestionDecision.None
        if (intent.company == null && intent.terms.isEmpty()) return SkillSuggestionDecision.None
        return SkillSuggestionDecision.SearchGitHub(intent)
    }
}

/** Remembers "Not now" per chat so a dismissed skill is never offered again in that chat. */
class SkillSuggestionMemory(entries: Set<String> = emptySet()) {
    private val dismissed = entries.toMutableSet()

    fun dismissedIn(conversationId: String): Set<String> =
        dismissed.filter { it.startsWith("$conversationId|") }.map { it.substringAfter('|') }.toSet()

    fun dismiss(
        conversationId: String,
        skillId: String,
    ) {
        dismissed += "$conversationId|$skillId"
        // Bounded so the stored preference never grows without limit.
        while (dismissed.size > MAX_ENTRIES) dismissed.remove(dismissed.first())
    }

    fun entries(): Set<String> = dismissed.toSet()

    companion object {
        const val MAX_ENTRIES = 300
    }
}
