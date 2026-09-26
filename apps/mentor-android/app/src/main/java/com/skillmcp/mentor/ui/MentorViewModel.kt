package com.skillmcp.mentor.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skillmcp.mentor.data.AppContainer
import com.skillmcp.mentor.data.MentorPrefs
import com.skillmcp.mentor.data.db.SavedPromptEntity
import com.skillmcp.mentor.data.db.SkillEntity
import com.skillmcp.mentor.llm.HuggingFaceDefaults
import com.skillmcp.mentor.llm.HuggingFaceHubApi
import com.skillmcp.mentor.llm.HuggingFaceModelSummary
import com.skillmcp.mentor.llm.ModelPreset
import com.skillmcp.mentor.mcp.McpServer
import com.skillmcp.mentor.plugins.BuiltinPlugins
import com.skillmcp.mentor.skills.BundledSkillPack
import com.skillmcp.mentor.skills.BundledSkills
import com.skillmcp.mentor.skills.SkillCatalog
import com.skillmcp.mentor.llm.LlmProfile
import com.skillmcp.mentor.llm.LlmProviderKind
import com.skillmcp.mentor.llm.UsageByDayRow
import com.skillmcp.mentor.llm.UsageByModelRow
import com.skillmcp.mentor.llm.UsageTotals
import com.skillmcp.mentor.mentor.AgentEventHint
import com.skillmcp.mentor.mentor.BuildSuggestion
import com.skillmcp.mentor.mentor.UiConversation
import com.skillmcp.mentor.mentor.UiMessage
import com.skillmcp.mentor.voice.ElevenLabsVoiceClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

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
    val activeStep: String = "",
    val lastCommand: String = "",
    val streamPreview: String = "",
    val savedPrompts: List<SavedPromptEntity> = emptyList(),
    val skillToggles: Map<String, Boolean> = emptyMap(),
    val catalogSkills: List<com.skillmcp.mentor.skills.CatalogSkill> = SkillCatalog.featured,
    val bundledSkillPacks: List<BundledSkillPack> = BundledSkills.packs,
    val builtinPlugins: List<com.skillmcp.mentor.plugins.BuiltinPlugin> = BuiltinPlugins.all,
    val mcpServers: List<McpServer> = emptyList(),
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
    val activeStep = MutableStateFlow("")
    val lastCommand = MutableStateFlow("")
    val usageWindow = MutableStateFlow(UsageWindow.WEEK)
    val streamPreview = MutableStateFlow("")
    val skillToggles = MutableStateFlow<Map<String, Boolean>>(emptyMap())

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
            repository.observeBuildEvents(),
            activeStep,
            lastCommand,
            prefs.prefsFlow.map { it.focusTopic },
        ) { events, step, cmd, goal ->
            container.buildSuggestionEngine.compute(
                com.skillmcp.mentor.mentor.BuildSuggestionsInput(
                    goal = goal,
                    activeStep = step,
                    lastCommand = cmd,
                    recentEvents = events.map { AgentEventHint(it.kind, it.summary) },
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
            combine(
                repository.observeSkills(),
                repository.observeLlmProfiles(),
                repository.observeMcpServers(),
            ) { skills, profiles, mcpServers -> Triple(skills, profiles, mcpServers) },
            combine(activeProfileFlow, usageSlice, prefs.prefsFlow) { activeProfile, usage, mentorPrefs ->
                Triple(activeProfile, usage, mentorPrefs)
            },
        ) { a, b ->
            MetaSlice(
                skills = a.first,
                llmProfiles = a.second,
                mcpServers = a.third,
                activeLlmProfile = b.first,
                usage = b.second,
                prefs = b.third,
            )
        }

    private data class MetaSlice(
        val skills: List<SkillEntity>,
        val llmProfiles: List<LlmProfile>,
        val mcpServers: List<McpServer>,
        val activeLlmProfile: LlmProfile?,
        val usage: UsageSlice,
        val prefs: MentorPrefs,
    )

    private val coreData =
        combine(chatSlice, metaSlice, repository.observeSavedPrompts(), streamPreview, skillToggles) { chat, meta, prompts, preview, toggles ->
            CoreSlice(
                messages = chat.messages,
                conversations = chat.conversations,
                activeConversationId = chat.activeConversationId,
                suggestions = chat.suggestions,
                skills = meta.skills,
                llmProfiles = meta.llmProfiles,
                mcpServers = meta.mcpServers,
                activeLlmProfile = meta.activeLlmProfile,
                usageTotals = meta.usage.totals,
                usageByModel = meta.usage.byModel,
                usageByDay = meta.usage.byDay,
                usageWindow = meta.usage.window,
                prefs = meta.prefs,
                streamPreview = preview,
                savedPrompts = prompts,
                skillToggles = toggles,
            )
        }

    private data class CoreSlice(
        val messages: List<UiMessage>,
        val conversations: List<UiConversation>,
        val activeConversationId: String,
        val suggestions: List<BuildSuggestion>,
        val skills: List<SkillEntity>,
        val llmProfiles: List<LlmProfile>,
        val mcpServers: List<McpServer>,
        val activeLlmProfile: LlmProfile?,
        val usageTotals: UsageTotals,
        val usageByModel: List<UsageByModelRow>,
        val usageByDay: List<UsageByDayRow>,
        val usageWindow: UsageWindow,
        val prefs: MentorPrefs,
        val streamPreview: String,
        val savedPrompts: List<SavedPromptEntity>,
        val skillToggles: Map<String, Boolean>,
    )

    private val interactionState =
        combine(
            combine(draft, isSending, isListening) { d, s, l -> Triple(d, s, l) },
            combine(status, activeStep, lastCommand) { st, step, cmd -> Triple(st, step, cmd) },
        ) { a, b ->
            InteractionSlice(a.first, a.second, a.third, b.first, b.second, b.third)
        }

    private data class InteractionSlice(
        val draft: String,
        val isSending: Boolean,
        val isListening: Boolean,
        val status: String?,
        val activeStep: String,
        val lastCommand: String,
    )

    val uiState: StateFlow<MentorUiState> =
        combine(coreData, interactionState) { core, interaction ->
            MentorUiState(
                messages = core.messages,
                suggestions = core.suggestions,
                skills = core.skills,
                conversations = core.conversations,
                activeConversationId = core.activeConversationId,
                llmProfiles = core.llmProfiles,
                mcpServers = core.mcpServers,
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
                activeStep = interaction.activeStep,
                lastCommand = interaction.lastCommand,
                streamPreview = core.streamPreview,
                savedPrompts = core.savedPrompts,
                skillToggles = core.skillToggles,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MentorUiState())

    init {
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
            container.shareTextHolder.pending.collect { text ->
                if (!text.isNullOrBlank()) {
                    draft.value = text
                    container.shareTextHolder.consume()
                    status.value = "Shared text ready to send"
                }
            }
        }
    }

    fun onDraftChange(value: String) {
        draft.value = value
    }

    fun applySuggestion(prompt: String) {
        draft.value = prompt
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
            streamPreview.value = ""
            val result =
                repository.sendUserMessage(text) { partial -> streamPreview.value = partial }
            streamPreview.value = ""
            isSending.value = false
            if (result.isSuccess) {
                draft.value = ""
                val reply = result.getOrNull() ?: return@launch
                if (prefs.current().speakResponses) speak(reply)
            } else {
                status.value = result.exceptionOrNull()?.message ?: "Send failed"
            }
        }
    }

    fun toggleListen() {
        if (isListening.value) return
        viewModelScope.launch {
            isListening.value = true
            val locale = prefs.current().voiceLocaleTag
            voice.listenOnce(locale).also { isListening.value = false }
                .onSuccess { draft.value = it }
                .onFailure { status.value = it.message }
        }
    }

    fun speak(text: String) {
        viewModelScope.launch {
            val p = prefs.current()
            val eleven =
                if (p.elevenLabsApiKey.isNotBlank() && p.elevenLabsVoiceId.isNotBlank()) {
                    ElevenLabsVoiceClient(p.elevenLabsApiKey, p.elevenLabsVoiceId, appContext.cacheDir)
                } else {
                    null
                }
            voice.speak(text, p.voiceLocaleTag, eleven)
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

    fun saveLlmProfile(profile: LlmProfile) {
        viewModelScope.launch {
            repository.saveLlmProfile(profile)
            status.value = "Saved ${profile.name}"
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

    fun importSkill(url: String) {
        viewModelScope.launch {
            status.value = "Importing skill…"
            repository.importSkill(url).fold(
                onSuccess = { status.value = "Imported ${it.title}" },
                onFailure = { status.value = it.message ?: "Import failed" },
            )
        }
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

    fun runBackupNow() {
        viewModelScope.launch {
            container.backupRepository.uploadIfConfigured().fold(
                onSuccess = { status.value = it },
                onFailure = { status.value = it.message ?: "Backup failed" },
            )
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

    fun installCatalogSkill(url: String) = importSkill(url)

    fun installBundledSkill(pack: BundledSkillPack) {
        viewModelScope.launch {
            val entity = repository.installBundledSkill(pack)
            status.value = "Installed ${entity.title}"
        }
    }

    fun setPluginEnabled(pluginId: String, enabled: Boolean) {
        viewModelScope.launch {
            prefs.update { p ->
                val next =
                    if (enabled) p.enabledPluginIds + pluginId else p.enabledPluginIds - pluginId
                p.copy(enabledPluginIds = next)
            }
        }
    }

    fun addMcpServer(name: String, endpointUrl: String, token: String) {
        viewModelScope.launch {
            repository.addMcpServer(name, endpointUrl, token)
            status.value = "MCP server added"
        }
    }

    fun setMcpServerEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { repository.setMcpServerEnabled(id, enabled) }
    }

    fun deleteMcpServer(id: String) {
        viewModelScope.launch { repository.deleteMcpServer(id) }
    }

    fun testMcpServer(id: String) {
        viewModelScope.launch {
            status.value = "Testing MCP…"
            repository.testMcpServer(id).fold(
                onSuccess = { status.value = it },
                onFailure = { status.value = it.message ?: "MCP test failed" },
            )
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
