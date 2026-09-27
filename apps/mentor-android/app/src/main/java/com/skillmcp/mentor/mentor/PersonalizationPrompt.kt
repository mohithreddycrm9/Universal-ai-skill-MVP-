package com.skillmcp.mentor.mentor

import com.skillmcp.mentor.data.MentorPrefs

/**
 * Builds the system prompt sent with every message from the base assistant prompt plus the user's
 * personalization ("custom instructions"). Everything stays on device until the user sends a message.
 */
object PersonalizationPrompt {
    const val MAX_FIELD_CHARS = 1500

    fun compose(
        basePrompt: String,
        displayName: String,
        aboutMe: String,
        responseInstructions: String,
        enabled: Boolean,
    ): String {
        val base = basePrompt.trim()
        if (!enabled) return base
        val sections =
            buildList {
                if (base.isNotEmpty()) add(base)
                displayName.trim().take(80).takeIf { it.isNotEmpty() }?.let { add("The user's name is $it.") }
                aboutMe.trim().take(MAX_FIELD_CHARS).takeIf { it.isNotEmpty() }?.let {
                    add("What the user wants you to know about them:\n$it")
                }
                responseInstructions.trim().take(MAX_FIELD_CHARS).takeIf { it.isNotEmpty() }?.let {
                    add("How the user wants you to respond:\n$it")
                }
            }
        return sections.joinToString("\n\n")
    }

    fun compose(prefs: MentorPrefs): String =
        compose(
            basePrompt = prefs.assistantSystemPrompt,
            displayName = prefs.displayName,
            aboutMe = prefs.aboutMe,
            responseInstructions = prefs.responseInstructions,
            enabled = prefs.personalizationEnabled,
        )

    /** Appends a one-tap trait ("Be concise") to the instructions without duplicating it. */
    fun appendTrait(current: String, trait: String): String {
        val t = trait.trim()
        if (t.isEmpty() || current.lines().any { it.trim().equals(t, ignoreCase = true) }) return current
        val base = current.trimEnd()
        return (if (base.isEmpty()) t else "$base\n$t").take(MAX_FIELD_CHARS)
    }
}
