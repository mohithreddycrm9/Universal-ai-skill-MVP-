package com.skillmcp.mentor.mentor

import com.skillmcp.mentor.data.UserPreferences
import com.skillmcp.mentor.data.db.BuildEventEntity
import com.skillmcp.mentor.data.db.ChatMessageEntity
import com.skillmcp.mentor.data.db.MentorDao
import com.skillmcp.mentor.data.db.ProjectEntity
import com.skillmcp.mentor.data.db.SkillEntity
import com.skillmcp.mentor.llm.LlmProfileRepository
import com.skillmcp.mentor.llm.MultiLlmClient
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
    private val multiLlmClient: MultiLlmClient,
    private val llmProfileRepository: LlmProfileRepository,
    private val buildSuggestionEngine: BuildSuggestionEngine,
    private val userPreferences: UserPreferences,
    private val skillImporter: GitHubSkillImporter,
    private val syncCoordinator: SyncCoordinator,
) {
    val defaultProjectId = "default"

    suspend fun bootstrap() {
        llmProfileRepository.ensureDefaults()
        ensureDefaultProject()
    }

    suspend fun ensureDefaultProject() {
        val existing = dao.allProjects().any { it.id == defaultProjectId }
        if (!existing) {
            dao.upsertProject(
                ProjectEntity(
                    id = defaultProjectId,
                    name = "Conversations",
                    goal = userPreferences.current().focusTopic,
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

    fun observeLlmProfiles() = llmProfileRepository.observeProfiles()

    fun observeUsageTotals(sinceMs: Long) = llmProfileRepository.observeUsageTotals(sinceMs)

    fun observeUsageByModel(sinceMs: Long) = llmProfileRepository.observeUsageByModel(sinceMs)

    fun observeUsageByDay(sinceMs: Long) = llmProfileRepository.observeUsageByDay(sinceMs)

    fun observeBuildEvents(projectId: String = defaultProjectId) = dao.observeBuildEvents(projectId)

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
            bootstrap()
            val prefs = userPreferences.current()
            val profile = llmProfileRepository.activeProfile()
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
                multiLlmClient.chat(
                    profile = profile,
                    systemPrompt = prefs.assistantSystemPrompt,
                    history = historyList.filter { it.role == "user" || it.role == "assistant" },
                    userMessage = text,
                    extraContext = skillContext,
                )

            result.onSuccess { chat ->
                onAssistantDelta(chat.content)
                llmProfileRepository.recordUsage(profile, chat, success = true)
                dao.insertMessage(
                    ChatMessageEntity(
                        UUID.randomUUID().toString(),
                        projectId,
                        "assistant",
                        chat.content,
                        System.currentTimeMillis(),
                    ),
                )
                dao.upsertProject(
                    ProjectEntity(
                        id = projectId,
                        name = "Conversations",
                        goal = prefs.focusTopic,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                syncCoordinator.publishStateSnapshot()
            }
            result.onFailure { err ->
                llmProfileRepository.recordUsage(
                    profile,
                    com.skillmcp.mentor.llm.LlmChatResult("", null, profile.model, 0),
                    success = false,
                    errorMessage = err.message,
                )
            }
            result.map { it.content }
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

    suspend fun updateFocusTopic(topic: String) {
        userPreferences.update { it.copy(focusTopic = topic) }
        dao.upsertProject(
            ProjectEntity(
                id = defaultProjectId,
                name = "Conversations",
                goal = topic,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        syncCoordinator.publishStateSnapshot()
    }

    suspend fun setActiveLlmProfile(id: String) = llmProfileRepository.setActiveProfile(id)

    suspend fun saveLlmProfile(profile: com.skillmcp.mentor.llm.LlmProfile) =
        llmProfileRepository.upsertProfile(profile)

    suspend fun deleteLlmProfile(id: String) = llmProfileRepository.deleteProfile(id)

    fun startSyncIfConfigured() {
        syncCoordinator.startFromPrefs()
    }
}
