package com.skillmcp.mentor.mentor

import com.skillmcp.mentor.data.UserPreferences
import com.skillmcp.mentor.data.db.BuildEventEntity
import com.skillmcp.mentor.data.db.ChatMessageEntity
import com.skillmcp.mentor.data.db.MentorDao
import com.skillmcp.mentor.data.db.ProjectEntity
import com.skillmcp.mentor.data.db.SkillEntity
import com.skillmcp.mentor.skills.GitHubSkillImporter
import com.skillmcp.mentor.sync.SyncCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

data class UiMessage(
    val id: String,
    val role: String,
    val content: String,
)

class MentorRepository(
    private val dao: MentorDao,
    private val llmClient: LlmClient,
    private val buildSuggestionEngine: BuildSuggestionEngine,
    private val userPreferences: UserPreferences,
    private val skillImporter: GitHubSkillImporter,
    private val syncCoordinator: SyncCoordinator,
) {
    val defaultProjectId = "default"

    suspend fun ensureDefaultProject() {
        val existing = dao.allProjects().any { it.id == defaultProjectId }
        if (!existing) {
            dao.upsertProject(
                ProjectEntity(
                    id = defaultProjectId,
                    name = "My project",
                    goal = userPreferences.current().buildGoal,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun observeMessages(projectId: String = defaultProjectId): Flow<List<UiMessage>> =
        dao.observeMessages(projectId).map { list ->
            list.map { UiMessage(it.id, it.role, it.content) }
        }

    fun observeSkills() = dao.observeSkills()

    fun observeBuildEvents(projectId: String = defaultProjectId) = dao.observeBuildEvents(projectId)

    fun observeSuggestions(
        projectId: String = defaultProjectId,
        activeStep: String?,
        changedFiles: List<String>,
        lastCommand: String?,
        lastExit: Int?,
    ): Flow<List<BuildSuggestion>> =
        dao.observeBuildEvents(projectId).map { events ->
            val prefs = userPreferences.current()
            buildSuggestionEngine.compute(
                BuildSuggestionsInput(
                    goal = prefs.buildGoal,
                    activeStep = activeStep,
                    changedFiles = changedFiles,
                    lastCommand = lastCommand,
                    lastCommandExitCode = lastExit,
                    recentEvents = events.map { AgentEventHint(it.kind, it.summary) },
                ),
            )
        }

    suspend fun recordBuildEvent(kind: String, summary: String? = null, projectId: String = defaultProjectId) {
        dao.insertBuildEvent(
            BuildEventEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                kind = kind,
                summary = summary,
                createdAt = System.currentTimeMillis(),
            ),
        )
        syncCoordinator.publishStateSnapshot()
    }

    suspend fun sendUserMessage(
        text: String,
        projectId: String = defaultProjectId,
        onAssistantDelta: (String) -> Unit = {},
    ): Result<String> =
        withContext(Dispatchers.IO) {
            ensureDefaultProject()
            val prefs = userPreferences.current()
            val historyList =
                dao.observeMessages(projectId).first().map { ChatMessageDto(it.role, it.content) }

            val userId = UUID.randomUUID().toString()
            dao.insertMessage(
                ChatMessageEntity(userId, projectId, "user", text, System.currentTimeMillis()),
            )

            val skills = dao.allSkills()
            val skillContext =
                skills.joinToString("\n\n") { skill ->
                    "### ${skill.owner}/${skill.repo} @ ${skill.ref}\n${skill.markdown.take(4000)}"
                }

            val result =
                llmClient.chat(
                    baseUrl = prefs.llmBaseUrl,
                    apiKey = prefs.llmApiKey,
                    model = prefs.llmModel,
                    systemPrompt = prefs.mentorSystemPrompt,
                    history = historyList.filter { it.role == "user" || it.role == "assistant" },
                    userMessage = text,
                    skillContext = skillContext,
                )

            result.onSuccess { reply ->
                onAssistantDelta(reply)
                dao.insertMessage(
                    ChatMessageEntity(
                        UUID.randomUUID().toString(),
                        projectId,
                        "assistant",
                        reply,
                        System.currentTimeMillis(),
                    ),
                )
                dao.upsertProject(
                    ProjectEntity(
                        id = projectId,
                        name = "My project",
                        goal = prefs.buildGoal,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                syncCoordinator.publishStateSnapshot()
            }
            result
        }

    suspend fun importSkill(repoUrl: String): Result<SkillEntity> =
        withContext(Dispatchers.IO) {
            skillImporter.importFromRepoUrl(repoUrl).map { imported ->
                val entity =
                    SkillEntity(
                        id = imported.id,
                        owner = imported.owner,
                        repo = imported.repo,
                        ref = imported.ref,
                        title = imported.title,
                        markdown = imported.markdown,
                        addedAt = System.currentTimeMillis(),
                    )
                dao.upsertSkill(entity)
                syncCoordinator.publishStateSnapshot()
                entity
            }
        }

    suspend fun removeSkill(id: String) {
        dao.deleteSkill(id)
        syncCoordinator.publishStateSnapshot()
    }

    suspend fun updateBuildGoal(goal: String) {
        userPreferences.update { it.copy(buildGoal = goal) }
        dao.upsertProject(
            ProjectEntity(
                id = defaultProjectId,
                name = "My project",
                goal = goal,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        syncCoordinator.publishStateSnapshot()
    }

    fun startSyncIfConfigured() {
        syncCoordinator.startFromPrefs()
    }
}
