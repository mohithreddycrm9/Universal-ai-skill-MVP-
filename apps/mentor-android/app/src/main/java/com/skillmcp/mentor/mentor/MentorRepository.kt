package com.skillmcp.mentor.mentor

import com.skillmcp.mentor.data.UserPreferences
import com.skillmcp.mentor.data.db.BuildEventEntity
import com.skillmcp.mentor.data.db.ChatMessageEntity
import com.skillmcp.mentor.data.db.MentorDao
import com.skillmcp.mentor.data.db.ProjectEntity
import com.skillmcp.mentor.data.db.ReplyVersionEntity
import com.skillmcp.mentor.data.db.SkillEntity
import com.skillmcp.mentor.llm.LlmChatResult
import com.skillmcp.mentor.llm.LlmProfile
import com.skillmcp.mentor.llm.LlmProfileRepository
import com.skillmcp.mentor.llm.LlmStreaming
import com.skillmcp.mentor.llm.MultiLlmClient
import com.skillmcp.mentor.policy.AllowanceCheck
import com.skillmcp.mentor.policy.MessageAllowanceGuard
import com.skillmcp.mentor.policy.SpendLimitException
import com.skillmcp.mentor.policy.MessageLimitMigrator
import com.skillmcp.mentor.policy.SpendPolicy
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    /** Number of regenerated versions of an assistant reply (1 = never regenerated). */
    val versionCount: Int = 1,
    /** 0-based index of the version currently shown. */
    val versionIndex: Int = 0,
)

data class SendMessageResult(
    val content: String,
    val assistantMessageId: String,
)

data class UiConversation(
    val id: String,
    val name: String,
    val updatedAt: Long,
    val pinned: Boolean = false,
    val folderTag: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Volatile private var legacyPromptsChecked = false

    suspend fun bootstrap(): String? {
        userPreferences.warmCache()
        userPreferences.ensureSecretsMigratedFromDataStore()
        val migrated = MessageLimitMigrator.applyIfNeeded(userPreferences.get())
        var notice: String? = null
        if (migrated != null) {
            userPreferences.update { migrated }
            if (migrated.dailyMessageLimit > 0 || migrated.weeklyMessageLimit > 0) {
                notice =
                    "Your limits are now daily message caps " +
                        "(${migrated.dailyMessageLimit}/day, ${migrated.weeklyMessageLimit}/week). " +
                        "Change them in Activity."
            }
        }
        llmProfileRepository.ensureDefaults()
        ensureDefaultProject()
        removeLegacySeededPrompts()
        return notice
    }

    /**
     * Older builds pre-filled the saved-prompt library with 18 template prompts the user never wrote.
     * New installs start empty; this removes those untouched copies (exact title + body match) once.
     */
    private suspend fun removeLegacySeededPrompts() {
        if (legacyPromptsChecked) return
        legacyPromptsChecked = true
        dao.observeSavedPrompts().first()
            .filter { LegacySeededPrompts.isUntouchedSeed(it.title, it.body) }
            .forEach { dao.deleteSavedPrompt(it.id) }
    }

    suspend fun ensureDefaultProject() {
        if (dao.allProjects().none { it.id == defaultProjectId }) {
            dao.upsertProject(
                ProjectEntity(
                    id = defaultProjectId,
                    name = "New chat",
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
            combine(dao.observeMessages(id), dao.observeReplyVersions(id)) { list, versions ->
                val byMessage = versions.groupBy({ it.messageId }, { it.content })
                list.map { entity ->
                    val v = byMessage[entity.id].orEmpty()
                    UiMessage(
                        id = entity.id,
                        role = entity.role,
                        content = entity.content,
                        versionCount = v.size.coerceAtLeast(1),
                        versionIndex = ChatBranching.visibleVersionIndex(v, entity.content),
                    )
                }
            }
        }

    private suspend fun uiMessagesFor(projectId: String): List<UiMessage> =
        dao.messagesFor(projectId).map { UiMessage(it.id, it.role, it.content) }

    /**
     * Edit & resend: removes [messageId] (a user message in the active chat) and everything after it.
     * The caller then sends the edited text as a normal new turn. Returns false if nothing was removed.
     */
    suspend fun dropFromMessage(messageId: String): Boolean =
        withContext(Dispatchers.IO) {
            val projectId = userPreferences.current().activeConversationId.ifBlank { defaultProjectId }
            val ids = ChatBranching.idsToDropForEdit(uiMessagesFor(projectId), messageId)
            if (ids.isEmpty()) return@withContext false
            dao.deleteReplyVersionsFor(ids)
            dao.deleteMessages(ids)
            syncCoordinator.publishStateSnapshot()
            true
        }

    /**
     * Regenerates the latest reply in place (same message id). Every version is kept in reply_versions
     * so the user can switch between them; on failure the current reply is left untouched.
     */
    suspend fun regenerateReply(
        assistantMessageId: String,
        onStreamUpdate: (String) -> Unit = {},
        isCancelled: () -> Boolean = { false },
    ): Result<String> =
        withContext(Dispatchers.IO) {
            val prefs = userPreferences.current()
            val allowance = MessageAllowanceGuard.check(dao, prefs.dailyMessageLimit, prefs.weeklyMessageLimit)
            if (!allowance.allowed) return@withContext Result.failure(SpendLimitException(allowance))
            val projectId = prefs.activeConversationId.ifBlank { defaultProjectId }
            val context =
                ChatBranching.regenerateContext(uiMessagesFor(projectId), assistantMessageId)
                    ?: return@withContext Result.failure(IllegalStateException("Only the latest reply can be regenerated"))
            val profile = llmProfileRepository.activeProfile()
            val result =
                streamReply(
                    prefs = prefs,
                    profile = profile,
                    projectId = projectId,
                    history = context.history.map { ChatMessageDto(it.role, it.content) },
                    userText = context.userMessage.content,
                    userPayload = context.userMessage.content,
                    vision = null,
                    onStreamUpdate = onStreamUpdate,
                    isCancelled = isCancelled,
                )
            val chat = result.getOrElse { return@withContext Result.failure(it) }
            val existing = dao.replyVersions(assistantMessageId)
            val all = ChatBranching.versionsAfterRegenerate(existing.map { it.content }, context.reply.content, chat.content)
            val now = System.currentTimeMillis()
            dao.deleteReplyVersionsFor(listOf(assistantMessageId))
            dao.insertReplyVersions(
                all.mapIndexed { i, content ->
                    ReplyVersionEntity(UUID.randomUUID().toString(), assistantMessageId, projectId, content, i, now + i)
                },
            )
            dao.getMessage(assistantMessageId)?.let { dao.updateMessage(it.copy(content = chat.content)) }
            onSuccessChat(projectId, profile, chat, prefs)
            Result.success(chat.content)
        }

    /** Shows another saved version of a regenerated reply. */
    suspend fun selectReplyVersion(messageId: String, index: Int) =
        withContext(Dispatchers.IO) {
            val version = dao.replyVersions(messageId).getOrNull(index) ?: return@withContext
            dao.getMessage(messageId)?.let { dao.updateMessage(it.copy(content = version.content)) }
        }

    /** Drawer search: messages whose text matches, with a short snippet around the match. */
    suspend fun searchMessageHits(query: String, limit: Int = 20): List<MessageSearchHit> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.length < 2) return@withContext emptyList()
            val names = dao.allProjects().associate { it.id to it.name }
            dao.searchMessages(q, limit).mapNotNull { m ->
                val name = names[m.projectId] ?: return@mapNotNull null
                MessageSearchHit(m.projectId, name, m.id, SearchSnippet.around(m.content, q))
            }
        }

    fun observeSkills() = dao.observeSkills()

    fun observeLlmProfiles() = llmProfileRepository.observeProfiles()

    fun observeUsageTotals(sinceMs: Long) = llmProfileRepository.observeUsageTotals(sinceMs)

    fun observeUsageByModel(sinceMs: Long) = llmProfileRepository.observeUsageByModel(sinceMs)

    fun observeUsageByDay(sinceMs: Long) = llmProfileRepository.observeUsageByDay(sinceMs)

    fun observeMessageAllowance(): Flow<AllowanceCheck> =
        userPreferences.prefsFlow.flatMapLatest { prefs ->
            flow {
                emit(
                    MessageAllowanceGuard.check(
                        dao,
                        prefs.dailyMessageLimit,
                        prefs.weeklyMessageLimit,
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
                name = name.ifBlank { "New chat" }, // replaced by ChatTitle after the first message
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
        dao.deleteReplyVersionsForProject(id)
        dao.deleteMessagesForProject(id)
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
        userMessageId: String = UUID.randomUUID().toString(),
        assistantMessageId: String = UUID.randomUUID().toString(),
        onUserMessageSaved: () -> Unit = {},
    ): Result<SendMessageResult> =
        withContext(Dispatchers.IO) {
            bootstrap()
            val prefs = userPreferences.current()
            val allowance =
                MessageAllowanceGuard.check(
                    dao,
                    prefs.dailyMessageLimit,
                    prefs.weeklyMessageLimit,
                )
            if (!allowance.allowed) {
                return@withContext Result.failure(SpendLimitException(allowance))
            }
            val projectId = prefs.activeConversationId.ifBlank { defaultProjectId }
            val profile = llmProfileRepository.activeProfile()
            val historyList =
                dao.observeMessages(projectId).first().map { ChatMessageDto(it.role, it.content) }

            dao.insertMessage(
                ChatMessageEntity(
                    userMessageId,
                    projectId,
                    "user",
                    text,
                    System.currentTimeMillis(),
                ),
            )
            onUserMessageSaved()
            autoTitleIfNeeded(projectId, text, isFirstUserMessage = historyList.none { it.role == "user" })

            val userPayload =
                buildString {
                    append(text)
                    if (!pdfExtract.isNullOrBlank()) {
                        append("\n\n--- PDF text ---\n")
                        append(pdfExtract.trim())
                    }
                }
            val result =
                streamReply(
                    prefs = prefs,
                    profile = profile,
                    projectId = projectId,
                    history = historyList,
                    userText = text,
                    userPayload = userPayload,
                    vision = vision,
                    onStreamUpdate = onStreamUpdate,
                    isCancelled = isCancelled,
                )

            if (result.isFailure) {
                onFailureChat(
                    projectId,
                    profile,
                    result.exceptionOrNull() ?: Exception("Send failed"),
                    assistantMessageId,
                )
                return@withContext Result.failure(result.exceptionOrNull()!!)
            }
            val chat = result.getOrThrow()
            val assistantId = assistantMessageId
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
            Result.success(SendMessageResult(chat.content, assistantId))
        }

    private suspend fun streamReply(
        prefs: com.skillmcp.mentor.data.MentorPrefs,
        profile: LlmProfile,
        projectId: String,
        history: List<ChatMessageDto>,
        userText: String,
        userPayload: String,
        vision: com.skillmcp.mentor.llm.ChatVisionAttachment?,
        onStreamUpdate: (String) -> Unit,
        isCancelled: () -> Boolean,
    ): Result<LlmChatResult> {
        val skillContext = buildSkillContext(projectId)
        val extraContext =
            extensionOrchestrator.augmentExtraContext(
                userMessage = userText,
                skillContext = skillContext,
                enabledPluginIds = prefs.enabledPluginIds,
            )
        return llmStreaming.streamChat(
            profile = profile,
            systemPrompt = PersonalizationPrompt.compose(prefs),
            history = history.filter { it.role == "user" || it.role == "assistant" },
            userMessage = userPayload,
            extraContext = extraContext,
            vision = vision,
            onChunk = onStreamUpdate,
            temperature = prefs.modelPreset.temperature,
            isCancelled = isCancelled,
        )
    }

    /** New chats are titled from their first message instead of keeping "New chat". */
    private suspend fun autoTitleIfNeeded(projectId: String, firstText: String, isFirstUserMessage: Boolean) {
        if (!isFirstUserMessage) return
        val project = dao.allProjects().find { it.id == projectId } ?: return
        if (!ChatTitle.isPlaceholder(project.name)) return
        val title = ChatTitle.fromFirstMessage(firstText) ?: return
        dao.upsertProject(project.copy(name = title))
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

    private suspend fun onFailureChat(
        projectId: String,
        profile: LlmProfile,
        err: Throwable,
        messageId: String = UUID.randomUUID().toString(),
    ) {
        val userMessage = com.skillmcp.mentor.util.UserFacingErrors.message(err)
        val stored = com.skillmcp.mentor.util.UserFacingErrors.redactForStorage(err)
        dao.insertMessage(
            ChatMessageEntity(
                messageId,
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
