package com.skillmcp.mentor.mentor

import com.skillmcp.mentor.data.UserPreferences
import com.skillmcp.mentor.data.db.BuildEventEntity
import com.skillmcp.mentor.data.db.ChatMessageEntity
import com.skillmcp.mentor.data.db.MentorDao
import com.skillmcp.mentor.data.db.ProjectEntity
import com.skillmcp.mentor.data.db.SkillEntity
import com.skillmcp.mentor.llm.LlmChatResult
import com.skillmcp.mentor.llm.LlmProfile
import com.skillmcp.mentor.llm.LlmProfileRepository
import com.skillmcp.mentor.llm.LlmStreaming
import com.skillmcp.mentor.llm.MultiLlmClient
import com.skillmcp.mentor.skills.GitHubSkillImporter
import com.skillmcp.mentor.sync.SyncCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

data class UiMessage(
    val id: String,
    val role: String,
    val content: String,
)

data class UiConversation(
    val id: String,
    val name: String,
    val updatedAt: Long,
)

class MentorRepository(
    private val dao: MentorDao,
    private val multiLlmClient: MultiLlmClient,
    private val llmStreaming: LlmStreaming,
    private val llmProfileRepository: LlmProfileRepository,
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
        if (dao.allProjects().none { it.id == defaultProjectId }) {
            dao.upsertProject(
                ProjectEntity(
                    id = defaultProjectId,
                    name = "General",
                    goal = "",
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun observeActiveConversationId(): Flow<String> =
        userPreferences.prefsFlow.map { it.activeConversationId.ifBlank { defaultProjectId } }

    fun observeConversations(): Flow<List<UiConversation>> =
        dao.observeProjects().map { list ->
            list.map { UiConversation(it.id, it.name, it.updatedAt) }
        }

    fun observeMessages(): Flow<List<UiMessage>> =
        observeActiveConversationId().flatMapLatest { id ->
            dao.observeMessages(id).map { list -> list.map { UiMessage(it.id, it.role, it.content) } }
        }

    fun observeSkills() = dao.observeSkills()

    fun observeLlmProfiles() = llmProfileRepository.observeProfiles()

    fun observeUsageTotals(sinceMs: Long) = llmProfileRepository.observeUsageTotals(sinceMs)

    fun observeUsageByModel(sinceMs: Long) = llmProfileRepository.observeUsageByModel(sinceMs)

    fun observeUsageByDay(sinceMs: Long) = llmProfileRepository.observeUsageByDay(sinceMs)

    fun observeBuildEvents(): Flow<List<BuildEventEntity>> =
        observeActiveConversationId().flatMapLatest { dao.observeBuildEvents(it) }

    suspend fun selectConversation(id: String) {
        userPreferences.update { it.copy(activeConversationId = id) }
    }

    suspend fun createConversation(name: String): String {
        val id = UUID.randomUUID().toString()
        dao.upsertProject(
            ProjectEntity(
                id = id,
                name = name.ifBlank { "New chat" },
                goal = "",
                updatedAt = System.currentTimeMillis(),
            ),
        )
        userPreferences.update { it.copy(activeConversationId = id) }
        syncCoordinator.publishStateSnapshot()
        return id
    }

    suspend fun deleteConversation(id: String) {
        if (id == defaultProjectId) return
        dao.deleteProject(id)
        val active = userPreferences.current().activeConversationId
        if (active == id) {
            userPreferences.update { it.copy(activeConversationId = defaultProjectId) }
        }
        syncCoordinator.publishStateSnapshot()
    }

    suspend fun recordBuildEvent(kind: String, summary: String? = null) {
        val projectId = userPreferences.current().activeConversationId.ifBlank { defaultProjectId }
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
        onStreamUpdate: (String) -> Unit = {},
    ): Result<String> =
        withContext(Dispatchers.IO) {
            bootstrap()
            val prefs = userPreferences.current()
            val projectId = prefs.activeConversationId.ifBlank { defaultProjectId }
            val profile = llmProfileRepository.activeProfile()
            val historyList =
                dao.observeMessages(projectId).first().map { ChatMessageDto(it.role, it.content) }

            dao.insertMessage(
                ChatMessageEntity(
                    UUID.randomUUID().toString(),
                    projectId,
                    "user",
                    text,
                    System.currentTimeMillis(),
                ),
            )

            val skills = dao.allSkills()
            val skillContext =
                skills.joinToString("\n\n") { skill ->
                    "### ${skill.owner}/${skill.repo} @ ${skill.ref}\n${skill.markdown.take(4000)}"
                }

            val history = historyList.filter { it.role == "user" || it.role == "assistant" }
            val result =
                llmStreaming.streamChat(
                    profile = profile,
                    systemPrompt = prefs.assistantSystemPrompt,
                    history = history,
                    userMessage = text,
                    extraContext = skillContext,
                    onChunk = onStreamUpdate,
                )

            result.onSuccess { chat ->
                dao.insertMessage(
                    ChatMessageEntity(
                        UUID.randomUUID().toString(),
                        projectId,
                        "assistant",
                        chat.content,
                        System.currentTimeMillis(),
                    ),
                )
                onSuccessChat(projectId, profile, chat, prefs)
            }
            result.onFailure { err -> onFailureChat(profile, err) }
            result.map { it.content }
        }

    private suspend fun onSuccessChat(
        projectId: String,
        profile: LlmProfile,
        chat: LlmChatResult,
        prefs: com.skillmcp.mentor.data.MentorPrefs,
    ) {
        llmProfileRepository.recordUsage(profile, chat, success = true)
        val project = dao.allProjects().find { it.id == projectId }
        dao.upsertProject(
            ProjectEntity(
                id = projectId,
                name = project?.name ?: "Chat",
                goal = prefs.focusTopic,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        syncCoordinator.publishStateSnapshot()
    }

    private suspend fun onFailureChat(profile: LlmProfile, err: Throwable) {
        llmProfileRepository.recordUsage(
            profile,
            LlmChatResult("", null, profile.model, 0),
            success = false,
            errorMessage = err.message,
        )
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
        syncCoordinator.publishStateSnapshot()
    }

    suspend fun setActiveLlmProfile(id: String) = llmProfileRepository.setActiveProfile(id)

    suspend fun saveLlmProfile(profile: LlmProfile) = llmProfileRepository.upsertProfile(profile)

    suspend fun deleteLlmProfile(id: String) = llmProfileRepository.deleteProfile(id)

    suspend fun testLlmProfile(profile: LlmProfile) = llmProfileRepository.testProfile(profile)

    suspend fun activeLlmProfile() = llmProfileRepository.activeProfile()

    fun startSyncIfConfigured() {
        syncCoordinator.startFromPrefs()
    }
}
