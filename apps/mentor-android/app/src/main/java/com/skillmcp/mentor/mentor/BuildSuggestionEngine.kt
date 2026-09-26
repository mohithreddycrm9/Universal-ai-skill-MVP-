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
    val limit: Int = 6,
)

class BuildSuggestionEngine {
    fun compute(input: BuildSuggestionsInput): List<BuildSuggestion> {
        val events = input.recentEvents.takeLast(24)
        val goal = input.goal?.trim()?.take(200)
        val hasSignals =
            !goal.isNullOrEmpty() ||
                !input.activeStep.isNullOrBlank() ||
                events.isNotEmpty() ||
                input.changedFiles.isNotEmpty() ||
                !input.lastCommand.isNullOrBlank() ||
                input.lastCommandExitCode != null

        if (!hasSignals) return emptyList()

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
                label = "Add a GitHub skill",
                prompt = "Recommend an official GitHub repo skill to import for this goal and how to verify it.",
                because = "skill_gap",
                priority = 75,
            )
        }
        if (!goal.isNullOrEmpty()) {
            add(
                label = "Next build step",
                prompt = "Given my goal \"$goal\", what is the single best next step I should implement now?",
                because = "goal_set",
                priority = 70,
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

        return raw.sortedByDescending { it.priority }.take(input.limit.coerceIn(1, 12))
    }
}
