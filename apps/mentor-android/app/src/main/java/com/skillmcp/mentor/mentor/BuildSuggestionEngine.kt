package com.skillmcp.mentor.mentor

import java.util.UUID

data class AgentEventHint(
    val kind: String,
    val summary: String? = null,
    val files: List<String> = emptyList(),
)

data class BuildSuggestion(
    val id: String,
    val label: String,
    val prompt: String,
    val because: String,
    val priority: Int,
)

data class BuildSuggestionsInput(
    val goal: String? = null,
    val activeStep: String? = null,
    val changedFiles: List<String> = emptyList(),
    val lastCommand: String? = null,
    val lastCommandExitCode: Int? = null,
    val recentEvents: List<AgentEventHint> = emptyList(),
    val messageCount: Int = 0,
    val hasInstalledSkills: Boolean = false,
    val enabledPluginIds: Set<String> = emptySet(),
    val limit: Int = 10,
)

class BuildSuggestionEngine {
    fun compute(input: BuildSuggestionsInput): List<BuildSuggestion> {
        val events = input.recentEvents.takeLast(24)
        val goal = input.goal?.trim()?.take(200)
        val raw = mutableListOf<BuildSuggestion>()

        fun add(label: String, prompt: String, because: String, priority: Int) {
            raw += BuildSuggestion(UUID.randomUUID().toString(), label, prompt, because, priority)
        }

        if (events.any { it.kind == "test_failed" }) {
            add(
                label = "Fix failing test",
                prompt = goal?.let { "The test failed while building \"$it\". Diagnose the failure and propose the smallest fix." }
                    ?: "Diagnose the failing test and propose the smallest fix.",
                because = "test_failed",
                priority = 100,
            )
        }
        if (events.any { it.kind == "lint_failed" }) {
            add(
                label = "Resolve lint errors",
                prompt = "List lint issues blocking the build and fix them in priority order.",
                because = "lint_failed",
                priority = 90,
            )
        }
        if (events.any { it.kind == "shell_pending" }) {
            val cmd = input.lastCommand ?: events.firstOrNull { it.kind == "shell_pending" }?.summary
            add(
                label = "Review shell step",
                prompt = cmd?.let { "Before I run `$it`, explain risk and expected outcome for this build." }
                    ?: "Explain why the pending shell command is needed.",
                because = "shell_pending",
                priority = 85,
            )
        }
        if (input.lastCommandExitCode != null && input.lastCommandExitCode != 0) {
            add(
                label = "Recover from command error",
                prompt = "Command exited ${input.lastCommandExitCode}. Suggest next debugging steps.",
                because = "nonzero_exit",
                priority = 80,
            )
        }
        if (events.any { it.kind == "skill_gap" || it.kind == "acquire_job_failed" }) {
            add(
                label = "Add a skill pack",
                prompt = "Recommend a skill pack from Extensions I should install for my goal and how to use it.",
                because = "skill_gap",
                priority = 75,
            )
        }
        if (!goal.isNullOrEmpty()) {
            add(
                label = "Plan next step",
                prompt = "Given my focus \"$goal\", what is the single best next step I should take now?",
                because = "goal_set",
                priority = 70,
            )
            add(
                label = "Explain simply",
                prompt = "Explain \"$goal\" in simple terms with a short example.",
                because = "learn_topic",
                priority = 68,
            )
            add(
                label = "Milestones",
                prompt = "Break \"$goal\" into 3 milestones with success criteria for each.",
                because = "goal_set",
                priority = 67,
            )
        }
        if (input.changedFiles.isNotEmpty()) {
            val files = input.changedFiles.take(3).joinToString(", ")
            add(
                label = "Review recent edits",
                prompt = "Review changes in $files for bugs, security issues, and missing tests.",
                because = "files_changed",
                priority = 65,
            )
        }

        if (input.messageCount == 0) {
            ChatSuggestions.heroStarters.shuffled().take(6).forEach { quick ->
                add(quick.label, quick.prompt, "hero", 62)
            }
            add(
                label = "What can you do?",
                prompt = "What kinds of tasks can you help me with? Give 8 examples across learning, work, and life.",
                because = "onboarding",
                priority = 60,
            )
        } else {
            val baselineSlots = (input.limit - raw.size).coerceAtLeast(4)
            raw += ChatSuggestions.baselineBuildSuggestions(baselineSlots)
        }

        if (input.hasInstalledSkills) {
            add(
                label = "Use my skills",
                prompt = "Using my installed skill packs, how should I approach my current question?",
                because = "skills_installed",
                priority = 58,
            )
        }
        if ("calc" in input.enabledPluginIds) {
            add(
                label = "Quick math",
                prompt = "I might use /calc for arithmetic—help me set up the expression for a problem I describe.",
                because = "plugin_calc",
                priority = 40,
            )
        }
        if ("fetch" in input.enabledPluginIds) {
            add(
                label = "Research a link",
                prompt = "I can use /fetch on an HTTPS page—tell me what to look for once I share a URL.",
                because = "plugin_fetch",
                priority = 39,
            )
        }

        return raw
            .distinctBy { it.label.lowercase() }
            .sortedByDescending { it.priority }
            .take(input.limit.coerceIn(4, 14))
    }
}
