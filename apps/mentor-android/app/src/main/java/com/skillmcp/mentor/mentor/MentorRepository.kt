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
import com.skillmcp.mentor.policy.SpendCheck
import com.skillmcp.mentor.policy.SpendGuard
import com.skillmcp.mentor.policy.SpendLimitException
import com.skillmcp.mentor.policy.SpendPolicy
import com.skillmcp.mentor.llm.TokenCostEstimator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit
import com.skillmcp.mentor.extensions.ExtensionOrchestrator
import com.skillmcp.mentor.skills.BundledSkillInstaller
import com.skillmcp.mentor.skills.BundledSkillPack
import com.skillmcp.mentor.skills.GitHubSkillImporter
import com.skillmcp.mentor.data.db.ConversationSkillEntity
import com.skillmcp.mentor.data.db.SavedPromptEntity
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

data class SendMessageResult(
    val content: String,
    val assistantMessageId: String,
    val costUsd: Double,
)

data class UiConversation(
    val id: String,
    val name: String,
    val updatedAt: Long,
    val pinned: Boolean = false,
    val folderTag: String = "",
)

class MentorRepository(
    private val dao: MentorDao,
    private val multiLlmClient: MultiLlmClient,
    private val llmStreaming: LlmStreaming,
    private val llmProfileRepository: LlmProfileRepository,
    private val userPreferences: UserPreferences,
    private val skillImporter: GitHubSkillImporter,
    private val bundledSkillInstaller: BundledSkillInstaller,
    private val extensionOrchestrator: ExtensionOrchestrator,
    private val syncCoordinator: SyncCoordinator,
) {
    val defaultProjectId = "default"

    suspend fun bootstrap() {
        userPreferences.ensureSecretsMigratedFromDataStore()
        llmProfileRepository.ensureDefaults()
        ensureDefaultProject()
        seedDefaultPrompts()
    }

    private suspend fun seedDefaultPrompts() {
        val existing = dao.observeSavedPrompts().first()
        val knownTitles = existing.map { it.title }.toSet()
        val defaults =
            listOf(
                "Explain this like I'm new to the topic, with a simple example." to "Explain simply",
                "Create a step-by-step plan I can follow today." to "Action plan",
                "Review my message for clarity, tone, and grammar. Suggest improvements." to "Writing coach",
                "What are the top 3 things I should focus on this week?" to "Weekly focus",
                "Help me prepare talking points for a meeting tomorrow." to "Meeting prep",
                "Turn my rough notes into a clear outline with headings." to "Outline notes",
                "Suggest 5 interview questions for a role I describe." to "Interview prep",
                "Give me a gentle habit I can start today and track for 7 days." to "Small habit",
                "Help me plan my week with priorities, time blocks, and one stretch goal." to "Plan my week",
                "Brainstorm 10 creative ideas for a goal I describe." to "Brainstorm",
                "Compare pros and cons of two options I give you." to "Compare options",
                "Summarize the key points from our conversation in 5 bullets." to "Summarize chat",
                "Draft a professional email. Ask me for recipient, tone, and key points." to "Draft email",
                "Build a 7-day study plan for a subject I name." to "Study plan",
                "Plan meals for the week and give me a grouped grocery list." to "Meal plan",
                "Help me compare products before I buy. Ask category and budget." to "Shop compare",
                "Run a morning brief: my top 3 tasks for today." to "Morning brief",
                "I'll paste a draft—rewrite it in casual and professional versions." to "Polish message",
            )
        defaults.forEach { (body, title) ->
            if (title in knownTitles) return@forEach
            dao.upsertSavedPrompt(
                SavedPromptEntity(UUID.randomUUID().toString(), title, body, System.currentTimeMillis()),
            )
        }
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
            list
                .sortedWith(compareByDescending<ProjectEntity> { it.pinned }.thenByDescending { it.updatedAt })
                .map { UiConversation(it.id, it.name, it.updatedAt, it.pinned, it.folderTag) }
        }

    fun observeSavedPrompts() = dao.observeSavedPrompts()

    fun observeMessages(): Flow<List<UiMessage>> =
        observeActiveConversationId().flatMapLatest { id ->
            dao.observeMessages(id).map { list -> list.map { UiMessage(it.id, it.role, it.content) } }
        }

    fun observeSkills() = dao.observeSkills()

    fun observeLlmProfiles() = llmProfileRepository.observeProfiles()

    fun observeUsageTotals(sinceMs: Long) = llmProfileRepository.observeUsageTotals(sinceMs)

    fun observeUsageByModel(sinceMs: Long) = llmProfileRepository.observeUsageByModel(sinceMs)

    fun observeUsageByDay(sinceMs: Long) = llmProfileRepository.observeUsageByDay(sinceMs)

    fun observeSpendGuard(): Flow<SpendCheck> =
        combine(
            userPreferences.prefsFlow,
            dao.observeUsageTotals(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)),
        ) { prefs, _ -> prefs }
            .flatMapLatest { prefs ->
                flow {
                    emit(
                        SpendGuard.check(
                            dao,
                            prefs.dailyBudgetUsd,
                            prefs.weeklyBudgetUsd,
                        ),
                    )
                }
            }

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

    suspend fun renameConversation(id: String, name: String) {
        val project = dao.allProjects().find { it.id == id } ?: return
        dao.upsertProject(project.copy(name = name.trim().ifBlank { project.name }, updatedAt = System.currentTimeMillis()))
        syncCoordinator.publishStateSnapshot()
    }

    suspend fun setConversationPinned(id: String, pinned: Boolean) {
        val project = dao.allProjects().find { it.id == id } ?: return
        dao.upsertProject(project.copy(pinned = pinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun setConversationFolderTag(id: String, tag: String) {
        val project = dao.allProjects().find { it.id == id } ?: return
        dao.upsertProject(
            project.copy(folderTag = tag.trim(), updatedAt = System.currentTimeMillis()),
        )
        syncCoordinator.publishStateSnapshot()
    }

    suspend fun searchConversations(query: String): List<UiConversation> {
        if (query.isBlank()) {
            return dao.allProjects().map { UiConversation(it.id, it.name, it.updatedAt, it.pinned, it.folderTag) }
        }
        return dao.searchProjects(query.trim()).map {
            UiConversation(it.id, it.name, it.updatedAt, it.pinned, it.folderTag)
        }
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
        vision: com.skillmcp.mentor.llm.ChatVisionAttachment? = null,
        pdfExtract: String? = null,
        onStreamUpdate: (String) -> Unit = {},
        isCancelled: () -> Boolean = { false },
    ): Result<SendMessageResult> =
        withContext(Dispatchers.IO) {
            bootstrap()
            val prefs = userPreferences.current()
            val spend =
                SpendGuard.check(dao, prefs.dailyBudgetUsd, prefs.weeklyBudgetUsd)
            if (!spend.allowed) {
                return@withContext Result.failure(SpendLimitException(spend))
            }
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

            val skillContext = buildSkillContext(projectId)
            val extraContext =
                extensionOrchestrator.augmentExtraContext(
                    userMessage = text,
                    skillContext = skillContext,
                    enabledPluginIds = prefs.enabledPluginIds,
                )

            val userPayload =
                buildString {
                    append(text)
                    if (!pdfExtract.isNullOrBlank()) {
                        append("\n\n--- PDF text ---\n")
                        append(pdfExtract.trim())
                    }
                }
            val history = historyList.filter { it.role == "user" || it.role == "assistant" }
            val result =
                llmStreaming.streamChat(
                    profile = profile,
                    systemPrompt = prefs.assistantSystemPrompt,
                    history = history,
                    userMessage = userPayload,
                    extraContext = extraContext,
                    vision = vision,
                    onChunk = onStreamUpdate,
                    temperature = prefs.modelPreset.temperature,
                    isCancelled = isCancelled,
                )

            if (result.isFailure) {
                onFailureChat(projectId, profile, result.exceptionOrNull() ?: Exception("Send failed"))
                return@withContext Result.failure(result.exceptionOrNull()!!)
            }
            val chat = result.getOrThrow()
            val assistantId = UUID.randomUUID().toString()
            dao.insertMessage(
                ChatMessageEntity(
                    assistantId,
                    projectId,
                    "assistant",
                    chat.content,
                    System.currentTimeMillis(),
                ),
            )
            onSuccessChat(projectId, profile, chat, prefs)
            val cost =
                chat.usage?.let { u ->
                    val input = u.promptTokens * profile.inputCostPer1M / 1_000_000.0
                    val output = u.completionTokens * profile.outputCostPer1M / 1_000_000.0
                    input + output
                } ?: TokenCostEstimator.estimateReplyCost(profile, chat.content)
            Result.success(SendMessageResult(chat.content, assistantId, cost))
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
                pinned = project?.pinned ?: false,
                folderTag = project?.folderTag ?: "",
            ),
        )
        syncCoordinator.publishStateSnapshot()
    }

    private suspend fun onFailureChat(projectId: String, profile: LlmProfile, err: Throwable) {
        val userMessage = com.skillmcp.mentor.util.UserFacingErrors.message(err)
        val stored = com.skillmcp.mentor.util.UserFacingErrors.redactForStorage(err)
        dao.insertMessage(
            ChatMessageEntity(
                UUID.randomUUID().toString(),
                projectId,
                "assistant",
                "⚠️ $userMessage",
                System.currentTimeMillis(),
            ),
        )
        llmProfileRepository.recordUsage(
            profile,
            LlmChatResult("", null, profile.model, 0),
            success = false,
            errorMessage = stored,
        )
        syncCoordinator.publishStateSnapshot()
    }

    suspend fun installBundledSkill(pack: BundledSkillPack): SkillEntity {
        val entity = bundledSkillInstaller.toEntity(pack)
        dao.upsertSkill(entity)
        val convo = userPreferences.current().activeConversationId.ifBlank { defaultProjectId }
        dao.upsertConversationSkill(ConversationSkillEntity(convo, entity.id, enabled = true))
        syncCoordinator.publishStateSnapshot()
        return entity
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
                val convo = userPreferences.current().activeConversationId.ifBlank { defaultProjectId }
                dao.upsertConversationSkill(ConversationSkillEntity(convo, entity.id, enabled = true))
                syncCoordinator.publishStateSnapshot()
                entity
            }
        }

    suspend fun setSkillEnabledForConversation(conversationId: String, skillId: String, enabled: Boolean) {
        dao.upsertConversationSkill(ConversationSkillEntity(conversationId, skillId, enabled))
    }

    suspend fun isSkillEnabled(conversationId: String, skillId: String): Boolean {
        val row = dao.conversationSkills(conversationId).find { it.skillId == skillId }
        return row?.enabled ?: true
    }

    private suspend fun buildSkillContext(conversationId: String): String {
        val overrides = dao.conversationSkills(conversationId).associate { it.skillId to it.enabled }
        return dao.allSkills()
            .filter { overrides[it.id] != false }
            .joinToString("\n\n") { skill ->
                "### ${skill.title}\n${skill.markdown.take(4000)}"
            }
    }

    suspend fun savePrompt(title: String, body: String) {
        dao.upsertSavedPrompt(
            SavedPromptEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim().ifBlank { "Prompt" },
                body = body.trim(),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deletePrompt(id: String) {
        dao.deleteSavedPrompt(id)
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

    suspend fun disconnectLlmProfile(id: String) = llmProfileRepository.disconnectProfile(id)

    suspend fun deleteLlmProfile(id: String) = llmProfileRepository.deleteProfile(id)

    suspend fun testLlmProfile(profile: LlmProfile) = llmProfileRepository.testProfile(profile)

    suspend fun activeLlmProfile() = llmProfileRepository.activeProfile()

    fun startSyncIfConfigured() {
        syncCoordinator.startFromPrefs()
    }
}
