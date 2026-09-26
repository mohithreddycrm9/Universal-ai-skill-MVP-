package com.skillmcp.mentor.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skillmcp.mentor.data.AppContainer
import com.skillmcp.mentor.data.MentorPrefs
import com.skillmcp.mentor.data.db.SkillEntity
import com.skillmcp.mentor.mentor.AgentEventHint
import com.skillmcp.mentor.mentor.BuildSuggestion
import com.skillmcp.mentor.mentor.UiMessage
import com.skillmcp.mentor.voice.ElevenLabsVoiceClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MentorUiState(
    val messages: List<UiMessage> = emptyList(),
    val suggestions: List<BuildSuggestion> = emptyList(),
    val skills: List<SkillEntity> = emptyList(),
    val prefs: MentorPrefs = MentorPrefs(),
    val draft: String = "",
    val isSending: Boolean = false,
    val isListening: Boolean = false,
    val status: String? = null,
    val activeStep: String = "",
    val lastCommand: String = "",
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

    private val suggestionsFlow =
        combine(
            repository.observeBuildEvents(),
            activeStep,
            lastCommand,
            prefs.prefsFlow.map { it.buildGoal },
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

    private val coreData =
        combine(
            repository.observeMessages(),
            suggestionsFlow,
            repository.observeSkills(),
            prefs.prefsFlow,
        ) { messages, suggestions, skills, mentorPrefs ->
            CoreSlice(messages, suggestions, skills, mentorPrefs)
        }

    private data class CoreSlice(
        val messages: List<UiMessage>,
        val suggestions: List<BuildSuggestion>,
        val skills: List<SkillEntity>,
        val prefs: MentorPrefs,
    )

    private val interactionState =
        combine(
            combine(draft, isSending, isListening) { d, sending, listening -> Triple(d, sending, listening) },
            combine(status, activeStep, lastCommand) { st, step, cmd -> Triple(st, step, cmd) },
        ) { first, second ->
            InteractionSlice(
                draft = first.first,
                isSending = first.second,
                isListening = first.third,
                status = second.first,
                activeStep = second.second,
                lastCommand = second.third,
            )
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
                prefs = core.prefs,
                draft = interaction.draft,
                isSending = interaction.isSending,
                isListening = interaction.isListening,
                status = interaction.status,
                activeStep = interaction.activeStep,
                lastCommand = interaction.lastCommand,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MentorUiState())

    init {
        viewModelScope.launch {
            repository.ensureDefaultProject()
            repository.startSyncIfConfigured()
        }
    }

    fun onDraftChange(value: String) {
        draft.value = value
    }

    fun onActiveStepChange(value: String) {
        activeStep.value = value
    }

    fun onLastCommandChange(value: String) {
        lastCommand.value = value
    }

    fun applySuggestion(prompt: String) {
        draft.value = prompt
    }

    fun recordTestFailed() {
        viewModelScope.launch {
            repository.recordBuildEvent("test_failed", "Unit tests failed on device")
            status.value = "Logged test_failed — new chips generated"
        }
    }

    fun sendMessage() {
        val text = draft.value.trim()
        if (text.isEmpty() || isSending.value) return
        viewModelScope.launch {
            isSending.value = true
            status.value = null
            val result = repository.sendUserMessage(text)
            isSending.value = false
            if (result.isSuccess) {
                draft.value = ""
                val reply = result.getOrNull() ?: return@launch
                if (prefs.current().speakResponses) {
                    speak(reply)
                }
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
            val heard = voice.listenOnce(locale)
            isListening.value = false
            heard.onSuccess { draft.value = it }
            heard.onFailure { status.value = it.message }
        }
    }

    fun speak(text: String) {
        viewModelScope.launch {
            val p = prefs.current()
            val eleven =
                if (p.elevenLabsApiKey.isNotBlank() && p.elevenLabsVoiceId.isNotBlank()) {
                    ElevenLabsVoiceClient(
                        apiKey = p.elevenLabsApiKey,
                        voiceId = p.elevenLabsVoiceId,
                        cacheDir = appContext.cacheDir,
                    )
                } else {
                    null
                }
            voice.speak(text, p.voiceLocaleTag, eleven)
        }
    }

    fun importSkill(url: String) {
        viewModelScope.launch {
            status.value = "Importing skill…"
            val result = repository.importSkill(url)
            status.value =
                result.fold(
                    onSuccess = { "Imported ${it.title}" },
                    onFailure = { it.message ?: "Import failed" },
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
            val result = container.backupRepository.uploadIfConfigured()
            status.value = result.getOrElse { it.message ?: "Backup failed" }
        }
    }

    fun updateBuildGoal(goal: String) {
        viewModelScope.launch { repository.updateBuildGoal(goal) }
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
