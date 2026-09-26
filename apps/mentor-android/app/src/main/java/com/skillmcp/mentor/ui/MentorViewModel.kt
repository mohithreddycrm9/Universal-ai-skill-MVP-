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
import com.skillmcp.mentor.policy.SpendCheck
import com.skillmcp.mentor.skills.CatalogSkill
import com.skillmcp.mentor.skills.SkillCatalog
import com.skillmcp.mentor.llm.GoogleLlmSignIn
import com.skillmcp.mentor.llm.LlmProfile
import com.skillmcp.mentor.llm.LlmProviderKind
import com.skillmcp.mentor.llm.isConfigured
import com.skillmcp.mentor.llm.UsageByDayRow
import com.skillmcp.mentor.llm.UsageByModelRow
import com.skillmcp.mentor.llm.UsageTotals
import com.skillmcp.mentor.mentor.AgentEventHint
import com.skillmcp.mentor.mentor.BuildSuggestion
import com.skillmcp.mentor.mentor.UiConversation
import com.skillmcp.mentor.mentor.UiMessage
import com.skillmcp.mentor.util.UserFacingError
import com.skillmcp.mentor.util.UserFacingErrors
import com.skillmcp.mentor.voice.ElevenLabsVoiceClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val spendGuard: SpendCheck = SpendCheck(allowed = true),
    val pendingShare: SharePayload? = null,
    val pendingSkillInstall: SkillInstallRequest? = null,
    val lastExportMarkdown: String? = null,
)

class MentorViewModel(
    private val container: AppContainer,
    private val appContext: Context,
) : ViewModel() {
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

    private val spendGuardFlow = repository.observeSpendGuard()
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
            spendGuardFlow,
            rankedUseCasesFlow,
        ) { q, spend, ranked ->
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
                spendGuard = spend,
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
        val spendGuard: SpendCheck,
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
            combine(draft, isSending, isListening) { d, s, l -> Triple(d, s, l) },
            combine(status, statusDetails, activeStep, lastCommand) { st, details, step, cmd ->
                Quad(st, details, step, cmd)
            },
        ) { a, b ->
            InteractionSlice(a.first, a.second, a.third, b.first, b.second, b.third, b.fourth)
        }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private data class InteractionSlice(
        val draft: String,
        val isSending: Boolean,
        val isListening: Boolean,
        val status: String?,
        val statusDetails: String?,
        val activeStep: String,
        val lastCommand: String,
    )

    val uiState: StateFlow<MentorUiState> =
        combine(
            combine(coreData, interactionState) { core, interaction -> core to interaction },
            combine(pendingShareConsent, pendingSkillInstall, lastExportMarkdown) { share, skillInstall, exportMd ->
                Triple(share, skillInstall, exportMd)
            },
        ) { a, b ->
            val core = a.first
            val interaction = a.second
            val share = b.first
            val skillInstall = b.second
            val exportMd = b.third
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
                streamPreview = core.streamPreview,
                savedPrompts = core.savedPrompts,
                skillToggles = core.skillToggles,
                catalogSkills = core.catalogSkills,
                rankedUseCases = core.rankedUseCases,
                spendGuard = core.spendGuard,
                pendingShare = share,
                pendingSkillInstall = skillInstall,
                lastExportMarkdown = exportMd,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MentorUiState())

    private val guidedSetupVisibleInner = MutableStateFlow(true)
    val guidedSetupVisible: StateFlow<Boolean> = guidedSetupVisibleInner

    init {
        refreshRankedUseCases()
        viewModelScope.launch {
            repository.bootstrap()
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
                if (payload != null && payload.text.isNotBlank()) {
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
                }
            }
        }
        viewModelScope.launch {
            if (prefs.current().dailyBriefReminder) {
                DailyBriefWorker.schedule(appContext)
            }
        }
    }

    fun onDraftChange(value: String) {
        draft.value = value
    }

    fun applySuggestion(prompt: String) {
        draft.value = prompt
    }

    fun openChatWithSuggestion(prompt: String) {
        draft.value = prompt
        status.value = null
        openChatRequestsInner.tryEmit(Unit)
    }

    fun markWelcomeSeen() {
        viewModelScope.launch { prefs.update { it.copy(hasSeenWelcome = true) } }
    }

    fun completeGuidedSetup() {
        guidedSetupVisibleInner.value = false
        viewModelScope.launch {
            prefs.update { it.copy(hasCompletedGuidedSetup = true, hasSeenWelcome = true) }
        }
    }

    fun acceptSharedContent() {
        val payload = pendingShareConsent.value ?: return
        draft.value = payload.text
        pendingShareConsent.value = null
        status.value = null
        openChatRequestsInner.tryEmit(Unit)
    }

    fun declineSharedContent() {
        pendingShareConsent.value = null
    }

    val spendRaisePreview = MutableStateFlow<Pair<Double, Double>?>(null)

    fun previewRaiseSpendLimits() {
        viewModelScope.launch {
            val p = prefs.current()
            val daily = if (p.dailyBudgetUsd > 0) p.dailyBudgetUsd * 1.25 else 5.0
            val weekly = if (p.weeklyBudgetUsd > 0) p.weeklyBudgetUsd * 1.25 else 25.0
            spendRaisePreview.value = daily to weekly
        }
    }

    fun confirmRaiseSpendLimits() {
        val preview = spendRaisePreview.value ?: return
        spendRaisePreview.value = null
        viewModelScope.launch {
            prefs.update { p -> p.copy(dailyBudgetUsd = preview.first, weeklyBudgetUsd = preview.second) }
            status.value =
                "Daily cap set to $${"%.2f".format(preview.first)} · weekly $${"%.2f".format(preview.second)} (estimates)"
        }
    }

    fun dismissSpendRaisePreview() {
        spendRaisePreview.value = null
    }

    fun dismissSpendBlockMessage() {
        status.value = null
    }

    fun exportChatsMarkdown() {
        viewModelScope.launch {
            val md = container.chatExporter.exportAllMarkdown()
            lastExportMarkdown.value = md
            status.value = "Chat export ready — copy from Settings"
        }
    }

    fun clearExportMarkdown() {
        lastExportMarkdown.value = null
    }

    fun setAnalyticsOptIn(enabled: Boolean) {
        container.usageAnalytics.setOptIn(enabled)
    }

    fun analyticsOptIn(): Boolean = container.usageAnalytics.isOptInCached()

    suspend fun loadAnalyticsOptIn(): Boolean = container.usageAnalytics.isOptIn()

    fun showWidgetHint() {
        markWelcomeSeen()
        status.value = "Add widget: long-press home screen → Widgets → Universal AI"
    }

    fun setRequireBiometric(enabled: Boolean) {
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

    fun setDailyBriefReminder(enabled: Boolean) {
        viewModelScope.launch {
            prefs.update { it.copy(dailyBriefReminder = enabled) }
            if (enabled) {
                DailyBriefWorker.schedule(appContext)
            } else {
                DailyBriefWorker.cancel(appContext)
            }
        }
    }

    fun startPopularUseCase(useCase: PopularUseCase) {
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

    fun sendMessage() {
        val text = draft.value.trim()
        if (text.isEmpty() || isSending.value) return
        viewModelScope.launch {
            isSending.value = true
            status.value = null
            statusDetails.value = null
            streamPreview.value = ""
            val result =
                repository.sendUserMessage(text) { partial -> streamPreview.value = partial }
            streamPreview.value = ""
            isSending.value = false
            if (result.isSuccess) {
                draft.value = ""
                val reply = result.getOrNull() ?: return@launch
                val p = prefs.current()
                if (p.speakResponses) {
                    speakAndThen(reply) {
                        if (p.voiceHandsFree) startHandsFreeTurn(autoSend = true)
                    }
                } else if (p.voiceHandsFree) {
                    startHandsFreeTurn(autoSend = true)
                }
            } else {
                val parsed =
                    result.exceptionOrNull()?.let(UserFacingErrors::parse)
                        ?: UserFacingError("Send failed", null)
                status.value = parsed.summary
                statusDetails.value = parsed.details
            }
        }
    }

    fun toggleListen() {
        startHandsFreeTurn(autoSend = false)
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

    fun newConversation() {
        viewModelScope.launch {
            repository.createConversation("Chat ${System.currentTimeMillis() % 1000}")
        }
    }

    fun selectConversation(id: String) {
        viewModelScope.launch { repository.selectConversation(id) }
    }

    fun deleteConversation(id: String) {
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

    fun requestOpenTab(route: String) {
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

    fun updatePrefs(transform: (MentorPrefs) -> MentorPrefs) {
        viewModelScope.launch {
            prefs.update(transform)
            repository.startSyncIfConfigured()
        }
    }

    fun updateBackupUploadUrl(url: String) {
        viewModelScope.launch {
            prefs.update { it.copy(backupUploadUrl = url) }
            BackupScheduler.syncSchedule(appContext, url.trim())
        }
    }

    fun runBackupNow() {
        viewModelScope.launch {
            container.backupRepository.uploadIfConfigured().fold(
                onSuccess = { status.value = it },
                onFailure = { status.value = it.message ?: "Backup failed" },
            )
        }
    }

    fun wipeAllLocalData() {
        viewModelScope.launch {
            container.localDataWiper.wipeAllUserContent()
            repository.bootstrap()
            status.value = "All on-device data erased"
        }
    }

    fun updateFocusTopic(topic: String) {
        viewModelScope.launch { repository.updateFocusTopic(topic) }
    }

    fun setModelPreset(preset: ModelPreset) {
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

    fun renameConversation(id: String, name: String) {
        viewModelScope.launch { repository.renameConversation(id, name) }
    }

    fun pinConversation(id: String, pinned: Boolean) {
        viewModelScope.launch { repository.setConversationPinned(id, pinned) }
    }

    fun savePrompt(title: String, body: String) {
        viewModelScope.launch { repository.savePrompt(title, body) }
    }

    fun deletePrompt(id: String) {
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
