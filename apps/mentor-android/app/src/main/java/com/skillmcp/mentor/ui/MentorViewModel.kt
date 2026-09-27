package com.skillmcp.mentor.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skillmcp.mentor.data.AppContainer
import com.skillmcp.mentor.data.LaunchAction
import com.skillmcp.mentor.backup.BackupScheduler
import com.skillmcp.mentor.notify.DailyBriefWorker
import com.skillmcp.mentor.data.MentorPrefs
import com.skillmcp.mentor.data.db.SavedPromptEntity
import com.skillmcp.mentor.data.db.SkillEntity
import com.skillmcp.mentor.llm.HuggingFaceDefaults
import com.skillmcp.mentor.llm.HuggingFaceHubApi
import com.skillmcp.mentor.llm.HuggingFaceModelSummary
import com.skillmcp.mentor.llm.ModelPreset
import com.skillmcp.mentor.plugins.BuiltinPlugins
import com.skillmcp.mentor.skills.BundledSkillPack
import com.skillmcp.mentor.skills.BundledSkills
import com.skillmcp.mentor.mentor.PopularUseCase
import com.skillmcp.mentor.mentor.UseCaseCatalog
import com.skillmcp.mentor.analytics.UsageAnalytics
import com.skillmcp.mentor.data.SharePayload
import com.skillmcp.mentor.policy.AllowanceCheck
import com.skillmcp.mentor.skills.CatalogSkill
import com.skillmcp.mentor.skills.SkillCatalog
import com.skillmcp.mentor.llm.GoogleLlmSignIn
import com.skillmcp.mentor.llm.LlmProfile
import com.skillmcp.mentor.llm.LlmProviderKind
import com.skillmcp.mentor.llm.ConversationContext
import com.skillmcp.mentor.llm.isConfigured
import com.skillmcp.mentor.llm.UsageByDayRow
import com.skillmcp.mentor.llm.UsageByModelRow
import com.skillmcp.mentor.llm.UsageTotals
import com.skillmcp.mentor.mentor.AgentEventHint
import com.skillmcp.mentor.mentor.BuildSuggestion
import com.skillmcp.mentor.mentor.UiConversation
import com.skillmcp.mentor.mentor.UiMessage
import com.skillmcp.mentor.util.UrlSecurityPolicy
import com.skillmcp.mentor.util.UserFacingError
import com.skillmcp.mentor.util.UserFacingErrors
import com.skillmcp.mentor.llm.ChatVisionAttachment
import com.skillmcp.mentor.llm.VisionCapabilities
import com.skillmcp.mentor.util.ImageAttachmentProcessor
import com.skillmcp.mentor.util.PdfTextExtractor
import com.skillmcp.mentor.voice.ElevenLabsVoiceClient
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.TimeUnit

data class SkillInstallRequest(
    val title: String,
    val sourceUrl: String,
    val trustTier: String,
    val needsNetwork: Boolean,
)

enum class UsageWindow(val days: Int, val label: String) {
    TODAY(1, "Today"),
    WEEK(7, "7 days"),
    MONTH(30, "30 days"),
}

data class MentorUiState(
    val messages: List<UiMessage> = emptyList(),
    val suggestions: List<BuildSuggestion> = emptyList(),
    val skills: List<SkillEntity> = emptyList(),
    val conversations: List<UiConversation> = emptyList(),
    val activeConversationId: String = "default",
    val llmProfiles: List<LlmProfile> = emptyList(),
    val activeLlmProfile: LlmProfile? = null,
    val usageTotals: UsageTotals = UsageTotals(0, 0, 0, 0.0),
    val usageByModel: List<UsageByModelRow> = emptyList(),
    val usageByDay: List<UsageByDayRow> = emptyList(),
    val usageWindow: UsageWindow = UsageWindow.WEEK,
    val prefs: MentorPrefs = MentorPrefs(),
    val draft: String = "",
    val isSending: Boolean = false,
    val isListening: Boolean = false,
    val status: String? = null,
    val statusDetails: String? = null,
    val activeStep: String = "",
    val lastCommand: String = "",
    val streamPreview: String = "",
    val savedPrompts: List<SavedPromptEntity> = emptyList(),
    val skillToggles: Map<String, Boolean> = emptyMap(),
    val catalogSkills: List<CatalogSkill> = emptyList(),
    val bundledSkillPacks: List<BundledSkillPack> = BundledSkills.packs,
    val builtinPlugins: List<com.skillmcp.mentor.plugins.BuiltinPlugin> = BuiltinPlugins.all,
    val popularUseCases: List<PopularUseCase> = UseCaseCatalog.featured,
    val rankedUseCases: List<PopularUseCase> = UseCaseCatalog.featured,
    val messageAllowance: AllowanceCheck = AllowanceCheck(allowed = true),
    val pendingShare: SharePayload? = null,
    val pendingSkillInstall: SkillInstallRequest? = null,
    val lastExportMarkdown: String? = null,
    /** Shown immediately after send (at the bottom) until the persisted row with the same id arrives. */
    val pendingUserMessage: String? = null,
    /** Stable key of the optimistic user bubble; equals the persisted message id. */
    val pendingUserMessageId: String? = null,
    /** Stable key shared by Thinking…, the streaming reply and the persisted assistant message. */
    val replyKey: String? = null,
    /** True while the thinking/streaming row should be rendered (reply not yet persisted). */
    val showReplySlot: Boolean = false,
    /** User message being edited in the composer (edit & resend). */
    val editingMessageId: String? = null,
    /** Assistant message being regenerated in place (streams into the same row). */
    val regeneratingMessageId: String? = null,
    /** Message currently read aloud. */
    val speakingMessageId: String? = null,
    /** Drawer search: messages whose text matched, one per chat. */
    val messageSearchHits: List<com.skillmcp.mentor.mentor.MessageSearchHit> = emptyList(),
) {
    /** The model this chat uses: its remembered model if still saved, else the app-wide active model. */
    val chatLlmProfile: LlmProfile?
        get() =
            conversations.find { it.id == activeConversationId }?.llmProfileId
                ?.takeIf { it.isNotBlank() }
                ?.let { id -> llmProfiles.find { it.id == id } }
                ?: activeLlmProfile
}

/** Chat state that isn't persisted: edit/regenerate/read-aloud targets and message search hits. */
data class ChatExtras(
    val editingMessageId: String? = null,
    val regeneratingMessageId: String? = null,
    val speakingMessageId: String? = null,
    val messageSearchHits: List<com.skillmcp.mentor.mentor.MessageSearchHit> = emptyList(),
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class MentorViewModel(
    private val container: AppContainer,
    private val appContext: Context,
) : ViewModel(),
    com.skillmcp.mentor.ui.chat.ChatScreenActions,
    com.skillmcp.mentor.ui.settings.SettingsScreenActions {
    private val repository = container.mentorRepository
    private val prefs = container.userPreferences
    private val voice = container.voiceMentor

    val draft = MutableStateFlow("")
    val isSending = MutableStateFlow(false)
    val isListening = MutableStateFlow(false)
    val status = MutableStateFlow<String?>(null)
    val statusDetails = MutableStateFlow<String?>(null)
    val activeStep = MutableStateFlow("")
    val lastCommand = MutableStateFlow("")
    val usageWindow = MutableStateFlow(UsageWindow.WEEK)
    val streamPreview = MutableStateFlow("")
    val skillToggles = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val pendingShareConsent = MutableStateFlow<SharePayload?>(null)
    val pendingSkillInstall = MutableStateFlow<SkillInstallRequest?>(null)
    val lastExportMarkdown = MutableStateFlow<String?>(null)
    val pendingVision = MutableStateFlow<ChatVisionAttachment?>(null)
    val pendingImagePreviewUri = MutableStateFlow<Uri?>(null)
    val pendingPdfExtract = MutableStateFlow<String?>(null)
    val backupPassphrasePrompt = MutableStateFlow(false)
    val backupRestorePassphrasePrompt = MutableStateFlow(false)
    val drawerSearchResults = MutableStateFlow<List<UiConversation>?>(null)
    private val pendingUserMessage = MutableStateFlow<com.skillmcp.mentor.ui.chat.PendingSend?>(null)
    private val chatExtras = MutableStateFlow(ChatExtras())
    /** Auto read-aloud only follows a spoken question (like voice mode in other assistants). */
    @Volatile private var lastInputWasVoice = false

    private val openChatRequestsInner = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openChatRequests: SharedFlow<Unit> = openChatRequestsInner.asSharedFlow()

    private val openTabInner = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openTabRequests: SharedFlow<String> = openTabInner.asSharedFlow()

    private val connectLlmProfileIdInner = MutableStateFlow<String?>(null)
    val connectLlmProfileId: StateFlow<String?> = connectLlmProfileIdInner

    private val sinceMs =
        usageWindow.map { window ->
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(window.days.toLong())
        }

    private val usageTotalsFlow =
        sinceMs.flatMapLatest { repository.observeUsageTotals(it) }

    private val usageByModelFlow =
        sinceMs.flatMapLatest { repository.observeUsageByModel(it) }

    private val usageByDayFlow =
        sinceMs.flatMapLatest { repository.observeUsageByDay(it) }

    private val activeProfileFlow =
        combine(
            repository.observeLlmProfiles(),
            prefs.prefsFlow.map { it.activeLlmProfileId },
        ) { profiles, id ->
            profiles.find { it.id == id } ?: profiles.firstOrNull()
        }

    private val suggestionsFlow =
        combine(
            combine(
                repository.observeBuildEvents(),
                repository.observeMessages(),
                repository.observeSkills(),
            ) { events, messages, skills ->
                Triple(events, messages.size, skills.isNotEmpty())
            },
            combine(activeStep, lastCommand, prefs.prefsFlow) { step, cmd, mentorPrefs ->
                Triple(step, cmd, mentorPrefs)
            },
        ) { a, b ->
            val (events, messageCount, hasSkills) = a
            val (step, cmd, mentorPrefs) = b
            container.buildSuggestionEngine.compute(
                com.skillmcp.mentor.mentor.BuildSuggestionsInput(
                    goal = mentorPrefs.focusTopic,
                    activeStep = step,
                    lastCommand = cmd,
                    recentEvents = events.map { AgentEventHint(it.kind, it.summary) },
                    messageCount = messageCount,
                    hasInstalledSkills = hasSkills,
                    enabledPluginIds = mentorPrefs.enabledPluginIds,
                    limit = 16,
                ),
            )
        }

    private val chatSlice =
        combine(
            repository.observeMessages(),
            repository.observeConversations(),
            repository.observeActiveConversationId(),
            suggestionsFlow,
        ) { messages, conversations, activeId, suggestions ->
            ChatSlice(messages, conversations, activeId, suggestions)
        }

    private data class ChatSlice(
        val messages: List<UiMessage>,
        val conversations: List<UiConversation>,
        val activeConversationId: String,
        val suggestions: List<BuildSuggestion>,
    )

    private val usageSlice =
        combine(usageTotalsFlow, usageByModelFlow, usageByDayFlow, usageWindow) { totals, byModel, byDay, window ->
            UsageSlice(totals, byModel, byDay, window)
        }

    private data class UsageSlice(
        val totals: UsageTotals,
        val byModel: List<UsageByModelRow>,
        val byDay: List<UsageByDayRow>,
        val window: UsageWindow,
    )

    private val metaSlice =
        combine(
            repository.observeSkills(),
            repository.observeLlmProfiles(),
            activeProfileFlow,
            usageSlice,
            prefs.prefsFlow,
        ) { skills, profiles, activeProfile, usage, mentorPrefs ->
            MetaSlice(skills, profiles, activeProfile, usage, mentorPrefs)
        }

    private data class MetaSlice(
        val skills: List<SkillEntity>,
        val llmProfiles: List<LlmProfile>,
        val activeLlmProfile: LlmProfile?,
        val usage: UsageSlice,
        val prefs: MentorPrefs,
    )

    private val allowanceFlow = repository.observeMessageAllowance()
    private val streamCancelled = java.util.concurrent.atomic.AtomicBoolean(false)
    private val rankedUseCasesFlow = MutableStateFlow(UseCaseCatalog.featured)

    private val coreData =
        combine(
            combine(
                chatSlice,
                metaSlice,
                repository.observeSavedPrompts(),
                streamPreview,
                skillToggles,
            ) { chat, meta, prompts, preview, toggles ->
                Quintuple(chat, meta, prompts, preview, toggles)
            },
            allowanceFlow,
            rankedUseCasesFlow,
        ) { q, allowance, ranked ->
            val chat = q.first
            val meta = q.second
            CoreSlice(
                messages = chat.messages,
                conversations = chat.conversations,
                activeConversationId = chat.activeConversationId,
                suggestions = chat.suggestions,
                skills = meta.skills,
                llmProfiles = meta.llmProfiles,
                activeLlmProfile = meta.activeLlmProfile,
                usageTotals = meta.usage.totals,
                usageByModel = meta.usage.byModel,
                usageByDay = meta.usage.byDay,
                usageWindow = meta.usage.window,
                prefs = meta.prefs,
                streamPreview = q.fourth,
                savedPrompts = q.third,
                skillToggles = q.fifth,
                messageAllowance = allowance,
                catalogSkills = SkillCatalog.featuredForUi(meta.skills),
                rankedUseCases = ranked,
            )
        }

    private data class Quintuple<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E,
    )

    private data class CoreSlice(
        val messages: List<UiMessage>,
        val conversations: List<UiConversation>,
        val activeConversationId: String,
        val suggestions: List<BuildSuggestion>,
        val skills: List<SkillEntity>,
        val llmProfiles: List<LlmProfile>,
        val activeLlmProfile: LlmProfile?,
        val usageTotals: UsageTotals,
        val usageByModel: List<UsageByModelRow>,
        val usageByDay: List<UsageByDayRow>,
        val usageWindow: UsageWindow,
        val prefs: MentorPrefs,
        val streamPreview: String,
        val savedPrompts: List<SavedPromptEntity>,
        val skillToggles: Map<String, Boolean>,
        val messageAllowance: AllowanceCheck,
        val catalogSkills: List<CatalogSkill>,
        val rankedUseCases: List<PopularUseCase>,
    )

    private fun refreshRankedUseCases() {
        viewModelScope.launch {
            val tops = container.usageAnalytics.topIds(UsageAnalytics.EVENT_USE_CASE)
            rankedUseCasesFlow.value =
                if (tops.isEmpty()) {
                    UseCaseCatalog.featured
                } else {
                    val order = tops.mapIndexed { index, pair -> pair.first to index }.toMap()
                    UseCaseCatalog.featured.sortedBy { order[it.id] ?: Int.MAX_VALUE }
                }
        }
    }

    private val interactionState =
        combine(
            combine(draft, isSending, isListening, pendingUserMessage) { d, s, l, pending ->
                Quad(d, s, l, pending)
            },
            combine(status, statusDetails, activeStep, lastCommand) { st, details, step, cmd ->
                Quad(st, details, step, cmd)
            },
        ) { a, b ->
            InteractionSlice(a.first, a.second, a.third, a.fourth, b.first, b.second, b.third, b.fourth)
        }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private data class InteractionSlice(
        val draft: String,
        val isSending: Boolean,
        val isListening: Boolean,
        val pendingUserMessage: com.skillmcp.mentor.ui.chat.PendingSend?,
        val status: String?,
        val statusDetails: String?,
        val activeStep: String,
        val lastCommand: String,
    )

    val uiState: StateFlow<MentorUiState> =
        combine(
            combine(coreData, interactionState) { core, interaction -> core to interaction },
            combine(pendingShareConsent, pendingSkillInstall, lastExportMarkdown, chatExtras) { share, skillInstall, exportMd, extras ->
                Quad(share, skillInstall, exportMd, extras)
            },
        ) { a, b ->
            val core = a.first
            val interaction = a.second
            val share = b.first
            val skillInstall = b.second
            val exportMd = b.third
            val extras = b.fourth
            val persistedIds = core.messages.mapTo(HashSet()) { it.id }
            MentorUiState(
                messages = core.messages,
                suggestions = core.suggestions,
                skills = core.skills,
                conversations = core.conversations,
                activeConversationId = core.activeConversationId,
                llmProfiles = core.llmProfiles,
                activeLlmProfile = core.activeLlmProfile,
                usageTotals = core.usageTotals,
                usageByModel = core.usageByModel,
                usageByDay = core.usageByDay,
                usageWindow = core.usageWindow,
                prefs = core.prefs,
                draft = interaction.draft,
                isSending = interaction.isSending,
                isListening = interaction.isListening,
                status = interaction.status,
                statusDetails = interaction.statusDetails,
                activeStep = interaction.activeStep,
                lastCommand = interaction.lastCommand,
                pendingUserMessage =
                    com.skillmcp.mentor.ui.chat.PendingSendLogic.visiblePendingText(
                        interaction.pendingUserMessage,
                        persistedIds,
                    ),
                pendingUserMessageId = interaction.pendingUserMessage?.userMessageId,
                replyKey = interaction.pendingUserMessage?.replyId,
                showReplySlot =
                    com.skillmcp.mentor.ui.chat.PendingSendLogic.showReplySlot(
                        interaction.isSending,
                        interaction.pendingUserMessage,
                        persistedIds,
                    ),
                streamPreview = core.streamPreview,
                savedPrompts = core.savedPrompts,
                skillToggles = core.skillToggles,
                catalogSkills = core.catalogSkills,
                rankedUseCases = core.rankedUseCases,
                messageAllowance = core.messageAllowance,
                pendingShare = share,
                pendingSkillInstall = skillInstall,
                lastExportMarkdown = exportMd,
                editingMessageId = extras.editingMessageId,
                regeneratingMessageId = extras.regeneratingMessageId,
                speakingMessageId = extras.speakingMessageId,
                messageSearchHits = extras.messageSearchHits,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MentorUiState())

    private val guidedSetupVisibleInner = MutableStateFlow(true)
    val guidedSetupVisible: StateFlow<Boolean> = guidedSetupVisibleInner

    private val chatUiStore by lazy { com.skillmcp.mentor.data.ChatUiStateStore(appContext) }
    private var draftOwnerId: String? = null

    /** Saved scroll position for [conversationId], or null to follow the latest message. */
    override fun chatScrollFor(conversationId: String): com.skillmcp.mentor.data.ChatScrollPosition? =
        if (conversationId.isBlank()) null else chatUiStore.scroll(conversationId)

    override fun saveChatScroll(conversationId: String, position: com.skillmcp.mentor.data.ChatScrollPosition) {
        chatUiStore.setScroll(conversationId, position)
    }

    init {
        refreshRankedUseCases()
        // Per-chat draft: save the outgoing chat's draft, load the incoming one, persist edits (debounced).
        viewModelScope.launch {
            repository.observeActiveConversationId()
                .distinctUntilChanged()
                .collectLatest { id ->
                    val previous = draftOwnerId
                    if (previous != null && previous != id) {
                        chatUiStore.setDraft(previous, draft.value)
                        draft.value = chatUiStore.draft(id)
                    } else if (previous == null && draft.value.isBlank()) {
                        draft.value = chatUiStore.draft(id)
                    }
                    draftOwnerId = id
                    draft.drop(1).debounce(250).collect { text -> chatUiStore.setDraft(id, text) }
                }
        }
        viewModelScope.launch {
            val migrationNotice = repository.bootstrap()
            if (!migrationNotice.isNullOrBlank()) {
                status.value = migrationNotice
            }
            repository.startSyncIfConfigured()
        }
        viewModelScope.launch {
            combine(repository.observeActiveConversationId(), repository.observeSkills()) { convoId, skills ->
                convoId to skills
            }.collect { (convoId, skills) ->
                skillToggles.value =
                    skills.associate { skill ->
                        skill.id to repository.isSkillEnabled(convoId, skill.id)
                    }
            }
        }
        viewModelScope.launch {
            container.shareTextHolder.pending.collect { payload ->
                if (payload != null && (payload.text.isNotBlank() || payload.imageUri != null)) {
                    container.shareTextHolder.consume()
                    if (payload.sendsToAiProvider) {
                        pendingShareConsent.value = payload
                    } else {
                        draft.value = payload.text
                        openChatRequestsInner.tryEmit(Unit)
                    }
                }
            }
        }
        viewModelScope.launch {
            container.launchIntentHolder.actions.collect { action ->
                when (action) {
                    is LaunchAction.UseCase -> {
                        val useCase = UseCaseCatalog.featured.find { it.id == action.id }
                        if (useCase != null) {
                            startPopularUseCase(useCase)
                        } else {
                            openChatWithSuggestion("Help me with workflow: ${action.id}")
                        }
                    }
                    is LaunchAction.OpenTab -> openTabInner.tryEmit(action.route)
                    is LaunchAction.Draft -> openChatWithSuggestion(action.text)
                    LaunchAction.VoiceChat -> {
                        openChatRequestsInner.tryEmit(Unit)
                        startHandsFreeTurn(autoSend = false)
                    }
                }
            }
        }
        viewModelScope.launch {
            if (prefs.current().dailyBriefReminder) {
                DailyBriefWorker.schedule(appContext)
            }
        }
    }

    override fun onDraftChange(value: String) {
        lastInputWasVoice = false
        draft.value = value
    }

    override fun applySuggestion(prompt: String) {
        draft.value = prompt
    }

    override fun openChatWithSuggestion(prompt: String) {
        draft.value = prompt
        status.value = null
        openChatRequestsInner.tryEmit(Unit)
    }

    fun markWelcomeSeen() {
        viewModelScope.launch { prefs.update { it.copy(hasSeenWelcome = true) } }
    }

    /** Ends onboarding; a non-blank name (optional last page) becomes the profile name. */
    fun finishWelcome(name: String) {
        val trimmed = name.trim().take(80)
        viewModelScope.launch {
            prefs.update { it.copy(hasSeenWelcome = true, displayName = trimmed.ifEmpty { it.displayName }) }
        }
    }

    fun completeGuidedSetup() {
        guidedSetupVisibleInner.value = false
        viewModelScope.launch {
            prefs.update { it.copy(hasCompletedGuidedSetup = true, hasSeenWelcome = true) }
        }
    }

    override fun acceptSharedContent() {
        val payload = pendingShareConsent.value ?: return
        draft.value = payload.text
        pendingShareConsent.value = null
        // A shared image becomes the attachment of the next message (same path as the + button).
        payload.imageUri?.let { attachFromUri(Uri.parse(it), payload.imageMimeType ?: "image/*") }
        status.value = null
        openChatRequestsInner.tryEmit(Unit)
    }

    override fun declineSharedContent() {
        pendingShareConsent.value = null
    }

    override fun dismissSpendBlockMessage() {
        viewModelScope.launch {
            val until = com.skillmcp.mentor.policy.SpendPolicy.nextRollingDayDismissMs()
            prefs.update { it.copy(spendDailyBlockDismissedUntilMs = until) }
            status.value =
                "Limit reminder hidden for 24 hours (rolling window, same as the daily message limit)."
        }
    }

    override fun cancelSend() {
        streamCancelled.set(true)
        isSending.value = false
        streamPreview.value = ""
        chatExtras.value = chatExtras.value.copy(regeneratingMessageId = null)
        status.value = null
    }

    override fun startEdit(messageId: String) {
        if (isSending.value) return
        val message = uiState.value.messages.find { it.id == messageId && it.role == "user" } ?: return
        chatExtras.value = chatExtras.value.copy(editingMessageId = messageId)
        draft.value = message.content
        lastInputWasVoice = false
    }

    override fun cancelEdit() {
        if (chatExtras.value.editingMessageId == null) return
        chatExtras.value = chatExtras.value.copy(editingMessageId = null)
        draft.value = ""
    }

    override fun regenerate(messageId: String) {
        if (isSending.value) return
        stopReadAloud()
        chatExtras.value = chatExtras.value.copy(regeneratingMessageId = messageId, editingMessageId = null)
        isSending.value = true
        viewModelScope.launch {
            streamCancelled.set(false)
            status.value = null
            statusDetails.value = null
            streamPreview.value = ""
            val result =
                repository.regenerateReply(
                    assistantMessageId = messageId,
                    onStreamUpdate = { partial -> streamPreview.value = partial },
                    isCancelled = { streamCancelled.get() },
                )
            if (result.isSuccess) {
                // Keep streaming text on screen until the updated row arrives, so the swap is invisible.
                val text = result.getOrNull().orEmpty()
                withTimeoutOrNull(1_500) { uiState.first { s -> s.messages.any { it.id == messageId && it.content == text } } }
            } else if (!streamCancelled.get()) {
                val parsed = result.exceptionOrNull()?.let(UserFacingErrors::parse) ?: UserFacingError("Regenerate failed", null)
                status.value = parsed.summary
                statusDetails.value = parsed.details
            }
            isSending.value = false
            streamPreview.value = ""
            chatExtras.value = chatExtras.value.copy(regeneratingMessageId = null)
        }
    }

    override fun selectReplyVersion(messageId: String, index: Int) {
        viewModelScope.launch { repository.selectReplyVersion(messageId, index) }
    }

    override fun readAloud(messageId: String, text: String) {
        voice.stopSpeaking()
        chatExtras.value = chatExtras.value.copy(speakingMessageId = messageId)
        viewModelScope.launch {
            val p = prefs.current()
            val eleven =
                if (p.elevenLabsApiKey.isNotBlank() && p.elevenLabsVoiceId.isNotBlank()) {
                    ElevenLabsVoiceClient(p.elevenLabsApiKey, p.elevenLabsVoiceId, appContext.cacheDir)
                } else {
                    null
                }
            voice.speak(com.skillmcp.mentor.mentor.SpeechText.fromMarkdown(text), p.voiceLocaleTag, eleven)
            if (chatExtras.value.speakingMessageId == messageId) {
                chatExtras.value = chatExtras.value.copy(speakingMessageId = null)
            }
        }
    }

    override fun stopReadAloud() {
        if (chatExtras.value.speakingMessageId == null) return
        voice.stopSpeaking()
        chatExtras.value = chatExtras.value.copy(speakingMessageId = null)
    }

    override fun sendFollowUp(prompt: String) {
        if (isSending.value) return
        draft.value = prompt
        lastInputWasVoice = false
        sendMessage()
    }

    override fun exportChatsMarkdown() {
        viewModelScope.launch {
            val md = container.chatExporter.exportAllMarkdown()
            lastExportMarkdown.value = md
            status.value = "Chat export ready — copy from Settings"
        }
    }

    override fun clearExportMarkdown() {
        lastExportMarkdown.value = null
    }

    override fun setAnalyticsOptIn(enabled: Boolean) {
        container.usageAnalytics.setOptIn(enabled)
    }

    override fun analyticsOptIn(): Boolean = container.usageAnalytics.isOptInCached()

    suspend fun loadAnalyticsOptIn(): Boolean = container.usageAnalytics.isOptIn()

    fun showWidgetHint() {
        markWelcomeSeen()
        status.value =
            appContext.getString(com.skillmcp.mentor.R.string.widget_picker_hint_prefix) +
                appContext.getString(com.skillmcp.mentor.R.string.widget_title)
    }

    override fun setRequireBiometric(enabled: Boolean) {
        viewModelScope.launch { prefs.update { it.copy(requireBiometricUnlock = enabled) } }
    }

    fun onAppLockUnavailable() {
        viewModelScope.launch {
            prefs.update { it.copy(requireBiometricUnlock = false) }
            status.value =
                "App lock was turned off because this device has no screen lock. " +
                    "Set a PIN in Android Settings to use lock again."
        }
    }

    fun hideGuidedSetupForConnect() {
        guidedSetupVisibleInner.value = false
    }

    fun showGuidedSetupIfNeeded() {
        viewModelScope.launch {
            if (!prefs.current().hasCompletedGuidedSetup) {
                guidedSetupVisibleInner.value = true
            }
        }
    }

    fun clearStatusDetails() {
        statusDetails.value = null
    }

    override fun setDailyBriefReminder(enabled: Boolean) {
        viewModelScope.launch {
            prefs.update { it.copy(dailyBriefReminder = enabled) }
            if (enabled) {
                DailyBriefWorker.schedule(appContext)
            } else {
                DailyBriefWorker.cancel(appContext)
            }
        }
    }

    override fun startPopularUseCase(useCase: PopularUseCase) {
        container.usageAnalytics.record(UsageAnalytics.EVENT_USE_CASE, useCase.id)
        viewModelScope.launch {
            useCase.bundledSkillAsset?.let { asset ->
                val pack = BundledSkills.packs.find { it.assetPath == asset }
                if (pack != null) {
                    runCatching { repository.installBundledSkill(pack) }
                }
            }
            openChatWithSuggestion(useCase.prompt)
        }
    }

    fun setUsageWindow(window: UsageWindow) {
        usageWindow.value = window
    }

    override fun attachFromUri(uri: Uri, mimeType: String?) {
        viewModelScope.launch {
            when {
                mimeType?.startsWith("image/") == true -> {
                    val profile = uiState.value.chatLlmProfile
                    if (profile == null || !VisionCapabilities.supportsVision(profile)) {
                        status.value =
                            appContext.getString(
                                com.skillmcp.mentor.R.string.attach_image_needs_vision,
                                profile?.let { ConversationContext.modelLabel(it) } ?: "",
                            )
                        return@launch
                    }
                    val vision = ImageAttachmentProcessor.fromUri(appContext, uri)
                    if (vision == null) {
                        status.value = appContext.getString(com.skillmcp.mentor.R.string.attach_image_unreadable)
                        return@launch
                    }
                    pendingVision.value = vision
                    pendingImagePreviewUri.value = uri
                    pendingPdfExtract.value = null
                    status.value = null
                }
                mimeType == "application/pdf" -> {
                    pendingVision.value = null
                    pendingImagePreviewUri.value = null
                    pendingPdfExtract.value = PdfTextExtractor.extractText(appContext, uri)
                    status.value = null
                }
                else -> {
                    pendingVision.value = null
                    pendingPdfExtract.value = null
                    status.value = "Unsupported attachment type."
                }
            }
        }
    }

    override fun clearAttachment() {
        pendingVision.value = null
        pendingImagePreviewUri.value = null
        pendingPdfExtract.value = null
    }

    override fun searchChats(query: String) {
        viewModelScope.launch {
            drawerSearchResults.value =
                if (query.isBlank()) null else repository.searchConversations(query)
            val hits =
                if (query.isBlank()) emptyList() else com.skillmcp.mentor.mentor.SearchSnippet.onePerConversation(repository.searchMessageHits(query))
            chatExtras.value = chatExtras.value.copy(messageSearchHits = hits)
        }
    }

    override fun setConversationTag(id: String, tag: String) {
        viewModelScope.launch { repository.setConversationFolderTag(id, tag) }
    }

    override fun shareChatMarkdown(conversationId: String) {
        viewModelScope.launch { shareText(container.chatExporter.exportConversationMarkdown(conversationId), conversationId) }
    }

    /** Mid-chat model switch: same conversation, full history goes to the new model on the next turn. */
    override fun switchChatModel(profileId: String) {
        val profile = uiState.value.llmProfiles.find { it.id == profileId } ?: return
        if (!profile.isConfigured()) {
            openConnectLlm(profileId)
            return
        }
        viewModelScope.launch { repository.switchChatModel(profileId) }
    }

    override fun connectModel(profileId: String) = openConnectLlm(profileId)

    override fun shareChatText(conversationId: String) {
        viewModelScope.launch { shareText(container.chatExporter.exportConversationText(conversationId), conversationId) }
    }

    private fun shareText(body: String, conversationId: String) {
        val title = uiState.value.conversations.find { it.id == conversationId }?.name
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                if (!title.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, body)
            }
        appContext.startActivity(
            Intent.createChooser(intent, appContext.getString(com.skillmcp.mentor.R.string.chat_share_chat))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override fun shareChatPdf(conversationId: String) {
        viewModelScope.launch {
            val file = container.chatPdfExporter.exportConversationPdf(conversationId)
            val uri =
                FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    file,
                )
            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            appContext.startActivity(Intent.createChooser(intent, "Share PDF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun sendMessage() {
        val text = draft.value.trim()
        if (text.isEmpty() || isSending.value || chatExtras.value.regeneratingMessageId != null) return
        val editId = chatExtras.value.editingMessageId
        val fromVoice = lastInputWasVoice
        lastInputWasVoice = false
        stopReadAloud()
        val pending =
            com.skillmcp.mentor.ui.chat.PendingSend(
                userMessageId = java.util.UUID.randomUUID().toString(),
                replyId = java.util.UUID.randomUUID().toString(),
                text = text,
            )
        draft.value = ""
        pendingUserMessage.value = pending
        isSending.value = true
        chatExtras.value = chatExtras.value.copy(editingMessageId = null)
        viewModelScope.launch {
            // Edit & resend: the edited message and everything after it are replaced by this new turn.
            if (editId != null) repository.dropFromMessage(editId)
            streamCancelled.set(false)
            status.value = null
            statusDetails.value = null
            streamPreview.value = ""
            val vision = pendingVision.value
            val pdf = pendingPdfExtract.value
            val previewUri = pendingImagePreviewUri.value
            val result =
                repository.sendUserMessage(
                    text = text,
                    vision = vision,
                    pdfExtract = pdf,
                    onStreamUpdate = { partial -> streamPreview.value = partial },
                    isCancelled = { streamCancelled.get() },
                    userMessageId = pending.userMessageId,
                    assistantMessageId = pending.replyId,
                    onUserMessageSaved = {
                        pendingUserMessage.value = pendingUserMessage.value?.copy(saved = true)
                    },
                )
            // Keep the reply row until the persisted row (same key) is in the list, so the swap is invisible.
            // A blocked send never writes rows, so only wait when the user message was persisted.
            if (pendingUserMessage.value?.saved == true) {
                withTimeoutOrNull(1_500) {
                    uiState.first { s -> s.messages.any { it.id == pending.replyId } }
                }
            }
            isSending.value = false
            streamPreview.value = ""
            pendingUserMessage.value = null
            if (result.isSuccess) {
                pendingVision.value = null
                pendingPdfExtract.value = null
                pendingImagePreviewUri.value = null
                val send = result.getOrNull() ?: return@launch
                if (send.imageDropped) {
                    status.value = appContext.getString(com.skillmcp.mentor.R.string.model_image_dropped, send.modelLabel)
                }
                val reply = send.content
                val p = prefs.current()
                if (p.speakResponses && (fromVoice || p.voiceHandsFree)) {
                    speakAndThen(reply) {
                        if (p.voiceHandsFree) startHandsFreeTurn(autoSend = true)
                    }
                } else if (p.voiceHandsFree) {
                    startHandsFreeTurn(autoSend = true)
                }
            } else {
                pendingVision.value = vision
                pendingPdfExtract.value = pdf
                pendingImagePreviewUri.value = previewUri
                draft.value = com.skillmcp.mentor.ui.chat.PendingSendLogic.draftAfterFailure(draft.value, text)
                val parsed =
                    result.exceptionOrNull()?.let(UserFacingErrors::parse)
                        ?: UserFacingError("Send failed", null)
                status.value = parsed.summary
                statusDetails.value = parsed.details
            }
        }
    }

    override fun toggleListen() {
        startHandsFreeTurn(autoSend = false)
    }

    override fun cancelListening() {
        voice.cancelListening()
        isListening.value = false
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun startHandsFreeTurn(autoSend: Boolean) {
        if (isListening.value || isSending.value) return
        if (!hasMicPermission()) {
            status.value = "Microphone permission is required for voice input."
            return
        }
        viewModelScope.launch {
            isListening.value = true
            val locale = prefs.current().voiceLocaleTag
            voice.listenOnce(locale).also { isListening.value = false }
                .onSuccess { heard ->
                    draft.value = heard
                    lastInputWasVoice = heard.isNotBlank()
                    if (autoSend && heard.isNotBlank()) sendMessage()
                }
                .onFailure { err -> status.value = err.message ?: "Voice input failed" }
        }
    }

    fun speak(text: String) {
        speakAndThen(text, onDone = {})
    }

    private fun speakAndThen(text: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val p = prefs.current()
            val eleven =
                if (p.elevenLabsApiKey.isNotBlank() && p.elevenLabsVoiceId.isNotBlank()) {
                    ElevenLabsVoiceClient(p.elevenLabsApiKey, p.elevenLabsVoiceId, appContext.cacheDir)
                } else {
                    null
                }
            voice.speak(text, p.voiceLocaleTag, eleven)
            onDone()
        }
    }

    override fun newConversation() {
        viewModelScope.launch {
            repository.createConversation("New chat")
        }
    }

    override fun selectConversation(id: String) {
        viewModelScope.launch { repository.selectConversation(id) }
    }

    override fun deleteConversation(id: String) {
        chatUiStore.clear(id)
        viewModelScope.launch { repository.deleteConversation(id) }
    }

    fun selectLlmProfile(id: String) {
        viewModelScope.launch { repository.setActiveLlmProfile(id) }
    }

    fun openConnectLlm(profileId: String) {
        connectLlmProfileIdInner.value = profileId
    }

    fun dismissConnectLlm() {
        connectLlmProfileIdInner.value = null
        showGuidedSetupIfNeeded()
    }

    override fun requestOpenTab(route: String) {
        openTabInner.tryEmit(route)
    }

    fun saveLlmProfile(profile: LlmProfile) {
        viewModelScope.launch {
            repository.saveLlmProfile(profile)
            status.value = "Saved ${profile.name}"
        }
    }

    fun saveLlmConnection(profile: LlmProfile, activate: Boolean) {
        viewModelScope.launch {
            UrlSecurityPolicy.validateLlmBaseUrl(profile.kind, profile.baseUrl)?.let {
                status.value = it
                return@launch
            }
            repository.saveLlmProfile(profile)
            if (activate) repository.setActiveLlmProfile(profile.id)
            status.value =
                if (profile.isConfigured()) {
                    "Connected to ${profile.name}"
                } else {
                    "Saved ${profile.name} — add credentials to chat"
                }
        }
    }

    fun disconnectLlmProfile(id: String) {
        viewModelScope.launch {
            repository.disconnectLlmProfile(id)
            status.value = "Disconnected — add a new API key when ready"
        }
    }

    fun signInWithGoogle(webClientId: String, onEmail: (String) -> Unit) {
        viewModelScope.launch {
            GoogleLlmSignIn.signIn(appContext, webClientId)
                .onSuccess { email ->
                    onEmail(email)
                    status.value = "Signed in as $email"
                }
                .onFailure { status.value = it.message ?: "Google sign-in failed" }
        }
    }

    fun addCustomLlmProfile(name: String, kind: LlmProviderKind, baseUrl: String, model: String, apiKey: String) {
        val id = "custom-${UUID.randomUUID()}"
        val resolvedBase =
            when {
                kind == LlmProviderKind.HUGGING_FACE && baseUrl.isBlank() -> HuggingFaceDefaults.ROUTER_BASE_URL
                else -> baseUrl
            }
        UrlSecurityPolicy.validateLlmBaseUrl(kind, resolvedBase)?.let {
            status.value = it
            return
        }
        val resolvedModel =
            when {
                kind == LlmProviderKind.HUGGING_FACE && model.isBlank() ->
                    HuggingFaceDefaults.featuredChatModels.first().first
                else -> model
            }
        saveLlmProfile(
            LlmProfile(
                id = id,
                name = name,
                kind = kind,
                baseUrl = resolvedBase,
                model = resolvedModel,
                apiKey = apiKey,
            ),
        )
        viewModelScope.launch { repository.setActiveLlmProfile(id) }
    }

    fun updateLlmProfile(profile: LlmProfile) {
        saveLlmProfile(profile)
    }

    fun searchHuggingFaceModels(
        query: String,
        accessToken: String,
        onResult: (List<HuggingFaceModelSummary>) -> Unit,
    ) {
        viewModelScope.launch {
            val result = HuggingFaceHubApi().searchModels(query, accessToken)
            onResult(result.getOrElse { emptyList() })
            result.exceptionOrNull()?.let { status.value = it.message }
        }
    }

    fun testLlmProfile(profile: LlmProfile) {
        viewModelScope.launch {
            status.value = "Testing ${profile.name}…"
            val result = repository.testLlmProfile(profile)
            status.value = result.fold(onSuccess = { "Connected: $it" }, onFailure = { it.message ?: "Failed" })
        }
    }

    fun deleteLlmProfile(id: String) {
        viewModelScope.launch { repository.deleteLlmProfile(id) }
    }

    fun requestInstallFromUrl(
        url: String,
        title: String,
        trustTier: String = "Custom",
        needsNetwork: Boolean = true,
    ) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        pendingSkillInstall.value =
            SkillInstallRequest(
                title = title,
                sourceUrl = trimmed,
                trustTier = trustTier,
                needsNetwork = needsNetwork,
            )
    }

    fun confirmSkillInstall() {
        val req = pendingSkillInstall.value ?: return
        pendingSkillInstall.value = null
        viewModelScope.launch {
            status.value = "Importing skill…"
            repository.importSkill(req.sourceUrl).fold(
                onSuccess = {
                    container.usageAnalytics.record(UsageAnalytics.EVENT_SKILL, it.id)
                    status.value = "Imported ${it.title}"
                },
                onFailure = { status.value = it.message ?: "Import failed" },
            )
        }
    }

    fun dismissSkillInstall() {
        pendingSkillInstall.value = null
    }

    fun importSkill(url: String) {
        requestInstallFromUrl(url, title = "Custom skill", trustTier = "Unverified", needsNetwork = true)
    }

    fun removeSkill(id: String) {
        viewModelScope.launch { repository.removeSkill(id) }
    }

    override fun updatePrefs(transform: (MentorPrefs) -> MentorPrefs) {
        viewModelScope.launch {
            prefs.update(transform)
            repository.startSyncIfConfigured()
        }
    }

    override fun updateBackupUploadUrl(url: String) {
        viewModelScope.launch {
            UrlSecurityPolicy.httpsRequiredError(url, "Backup URL")?.let {
                status.value = it
                return@launch
            }
            prefs.update { it.copy(backupUploadUrl = url) }
            BackupScheduler.syncSchedule(appContext, url.trim())
        }
    }

    override fun updateSyncWebSocketUrl(url: String) {
        viewModelScope.launch {
            UrlSecurityPolicy.httpsRequiredError(url, "Sync WebSocket URL")?.let {
                status.value = it
                return@launch
            }
            updatePrefs { it.copy(syncWebSocketUrl = url) }
        }
    }

    override fun promptBackupPassphrase() {
        backupPassphrasePrompt.value = true
    }

    override fun dismissBackupPassphrase() {
        backupPassphrasePrompt.value = false
    }

    override fun runBackupNow(passphrase: CharArray?) {
        viewModelScope.launch {
            backupPassphrasePrompt.value = false
            container.backupRepository.uploadIfConfigured(passphrase).fold(
                onSuccess = { status.value = it },
                onFailure = { status.value = it.message ?: "Backup failed" },
            )
        }
    }

    override fun promptRestorePassphrase() {
        backupRestorePassphrasePrompt.value = true
    }

    override fun dismissRestorePassphrase() {
        backupRestorePassphrasePrompt.value = false
    }

    override fun runRestoreNow(passphrase: CharArray?) {
        viewModelScope.launch {
            backupRestorePassphrasePrompt.value = false
            container.backupRepository.restoreFromConfiguredUrl(passphrase).fold(
                onSuccess = {
                    status.value = it
                    repository.bootstrap()
                },
                onFailure = { status.value = it.message ?: "Restore failed" },
            )
        }
    }

    override fun setCrashReportingOptIn(enabled: Boolean) {
        viewModelScope.launch {
            prefs.update { it.copy(crashReportingOptIn = enabled) }
        }
    }

    override fun wipeAllLocalData() {
        viewModelScope.launch {
            container.localDataWiper.wipeAllUserContent()
            repository.bootstrap()
            status.value = "All on-device data erased"
        }
    }

    override fun updateFocusTopic(topic: String) {
        viewModelScope.launch { repository.updateFocusTopic(topic) }
    }

    override fun setModelPreset(preset: ModelPreset) {
        viewModelScope.launch { prefs.update { it.copy(modelPreset = preset) } }
    }

    fun setSkillEnabled(skillId: String, enabled: Boolean) {
        viewModelScope.launch {
            val convo = prefs.current().activeConversationId.ifBlank { "default" }
            repository.setSkillEnabledForConversation(convo, skillId, enabled)
            skillToggles.value = skillToggles.value + (skillId to enabled)
        }
    }

    fun installCatalogSkill(entry: CatalogSkill) {
        requestInstallFromUrl(
            url = entry.sourceUrl,
            title = entry.title,
            trustTier = entry.trustTier,
            needsNetwork = entry.needsNetwork,
        )
    }

    fun installBundledSkill(pack: BundledSkillPack) {
        viewModelScope.launch {
            val entity = repository.installBundledSkill(pack)
            container.usageAnalytics.record(UsageAnalytics.EVENT_SKILL, entity.id)
            status.value = "Installed ${entity.title}"
        }
    }

    fun setPluginEnabled(pluginId: String, enabled: Boolean) {
        if (BuiltinPlugins.isAlwaysEnabled(pluginId)) return
        viewModelScope.launch {
            prefs.update { p ->
                val next =
                    if (enabled) p.enabledPluginIds + pluginId else p.enabledPluginIds - pluginId
                p.copy(enabledPluginIds = next)
            }
        }
    }

    override fun renameConversation(id: String, name: String) {
        viewModelScope.launch { repository.renameConversation(id, name) }
    }

    override fun pinConversation(id: String, pinned: Boolean) {
        viewModelScope.launch { repository.setConversationPinned(id, pinned) }
    }

    override fun savePrompt(title: String, body: String) {
        viewModelScope.launch { repository.savePrompt(title, body) }
    }

    override fun deletePrompt(id: String) {
        viewModelScope.launch { repository.deletePrompt(id) }
    }

    class Factory(
        private val container: AppContainer,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MentorViewModel(container, appContext) as T
    }
}
