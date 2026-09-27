package com.skillmcp.mentor.ui.chat

import com.skillmcp.mentor.skills.finder.OfficialSkill

data class SkillSuggestionItem(
    val skill: OfficialSkill,
    /** Found by a live GitHub search rather than the bundled/cached index. */
    val foundOnGitHub: Boolean = false,
    /** Installed and turned on for this chat. */
    val active: Boolean = false,
    /** SKILL.md already downloaded, so "Use this skill" needs no network. */
    val prefetched: Boolean = false,
)

/** The "Ready: <skill>" card for one chat. */
data class SkillSuggestionUi(
    val conversationId: String,
    val items: List<SkillSuggestionItem> = emptyList(),
    val searchingCompany: String? = null,
    val searching: Boolean = false,
    /** Set when a live search found nothing that passes the official-only policy. */
    val noOfficialCompany: String? = null,
    val docsUrl: String? = null,
    /** GitHub could not be checked (offline or rate limited). */
    val unavailableCompany: String? = null,
)
