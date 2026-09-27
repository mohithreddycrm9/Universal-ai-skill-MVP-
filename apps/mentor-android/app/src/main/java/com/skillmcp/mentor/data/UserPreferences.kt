package com.skillmcp.mentor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.skillmcp.mentor.llm.ModelPreset
import androidx.datastore.preferences.preferencesDataStore
import com.skillmcp.mentor.ui.theme.ThemeMode
import com.skillmcp.mentor.ui.theme.resolvesDark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mentor_prefs")

data class MentorPrefs(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentHue: Float = 220f,
    val fontScale: Float = 1f,
    val activeLlmProfileId: String = "openai",
    val llmBaseUrl: String = "https://api.openai.com/v1/",
    val llmModel: String = "gpt-4o-mini",
    val llmApiKey: String = "",
    val assistantSystemPrompt: String = DEFAULT_ASSISTANT_PROMPT,
    val voiceLocaleTag: String = Locale.getDefault().toLanguageTag(),
    /** BCP-47 tag or `system` for device default. */
    val appLanguageTag: String = "system",
    val speakResponses: Boolean = true,
    val elevenLabsApiKey: String = "",
    val elevenLabsVoiceId: String = "",
    val syncWebSocketUrl: String = "",
    val syncDeviceId: String = "",
    val backupUploadUrl: String = "",
    val backupBearerToken: String = "",
    val focusTopic: String = "",
    val activeConversationId: String = "default",
    val modelPreset: ModelPreset = ModelPreset.BALANCED,
    val dailyBudgetUsd: Double = 0.0,
    val weeklyBudgetUsd: Double = 0.0,
    val enabledPluginIds: Set<String> = emptySet(),
    val hasSeenWelcome: Boolean = false,
    val requireBiometricUnlock: Boolean = false,
    val dailyBriefReminder: Boolean = false,
    val hasCompletedGuidedSetup: Boolean = false,
    val morningBriefCalendar: Boolean = true,
    val morningBriefWeather: Boolean = true,
    val morningBriefNews: Boolean = false,
    val morningBriefTasks: Boolean = true,
    val voiceHandsFree: Boolean = false,
    val backupIncludeApiKeys: Boolean = false,
    val internalLaunchToken: String = "",
    val useDynamicColor: Boolean = false,
    val displayName: String = "",
) {
    companion object {
        const val DEFAULT_ASSISTANT_PROMPT =
            "You are a helpful, thoughtful personal AI assistant. " +
                "You can discuss any topic: learning, planning, writing, health information (not medical advice), " +
                "travel, productivity, creativity, coding, and daily life. Be clear, accurate, and kind. " +
                "Say when you are uncertain. Refuse harmful or illegal requests."
    }

    /** @deprecated use assistantSystemPrompt */
    val mentorSystemPrompt: String get() = assistantSystemPrompt

    /** @deprecated use focusTopic */
    val buildGoal: String get() = focusTopic
}

// Composable-only API — use theme from MainActivity with system dark flag instead.
fun MentorPrefs.resolvedDarkTheme(systemDark: Boolean): Boolean = themeMode.resolvesDark(systemDark)

class UserPreferences(
    private val context: Context,
    private val secureStore: LlmSecureStore,
) {
    val prefsFlow: Flow<MentorPrefs> =
        context.dataStore.data.map { prefs ->
            MentorPrefs(
                themeMode = ThemeMode.entries.find { it.name == prefs[KEY_THEME] } ?: ThemeMode.SYSTEM,
                accentHue = prefs[KEY_ACCENT] ?: 220f,
                fontScale = prefs[KEY_FONT_SCALE] ?: 1f,
                llmBaseUrl = prefs[KEY_LLM_BASE] ?: "https://api.openai.com/v1/",
                llmModel = prefs[KEY_LLM_MODEL] ?: "gpt-4o-mini",
                llmApiKey = "",
                activeLlmProfileId = prefs[KEY_ACTIVE_LLM] ?: "openai",
                assistantSystemPrompt =
                    prefs[KEY_ASSISTANT_PROMPT]
                        ?: prefs[KEY_MENTOR_PROMPT]
                        ?: MentorPrefs.DEFAULT_ASSISTANT_PROMPT,
                voiceLocaleTag = prefs[KEY_VOICE_LOCALE] ?: Locale.getDefault().toLanguageTag(),
                appLanguageTag = prefs[KEY_APP_LANGUAGE] ?: "system",
                speakResponses = prefs[KEY_SPEAK] ?: true,
                elevenLabsApiKey = secureStore.getAppSecret(LlmSecureStore.SECRET_ELEVEN_LABS),
                elevenLabsVoiceId = prefs[KEY_ELEVEN_VOICE] ?: "",
                syncWebSocketUrl = prefs[KEY_SYNC_URL] ?: "",
                syncDeviceId = prefs[KEY_DEVICE_ID] ?: "",
                backupUploadUrl = prefs[KEY_BACKUP_URL] ?: "",
                backupBearerToken = secureStore.getAppSecret(LlmSecureStore.SECRET_BACKUP_TOKEN),
                focusTopic = prefs[KEY_FOCUS_TOPIC] ?: prefs[KEY_BUILD_GOAL] ?: "",
                activeConversationId = prefs[KEY_CONVERSATION] ?: "default",
                modelPreset = ModelPreset.entries.find { it.name == prefs[KEY_MODEL_PRESET] } ?: ModelPreset.BALANCED,
                dailyBudgetUsd = prefs[KEY_DAILY_BUDGET]?.toDoubleOrNull() ?: 0.0,
                weeklyBudgetUsd = prefs[KEY_WEEKLY_BUDGET]?.toDoubleOrNull() ?: 0.0,
                enabledPluginIds = prefs[KEY_ENABLED_PLUGINS] ?: emptySet(),
                hasSeenWelcome = prefs[KEY_SEEN_WELCOME] ?: false,
                requireBiometricUnlock = prefs[KEY_BIOMETRIC] ?: false,
                dailyBriefReminder = prefs[KEY_DAILY_BRIEF] ?: false,
                hasCompletedGuidedSetup = prefs[KEY_GUIDED_SETUP] ?: false,
                morningBriefCalendar = prefs[KEY_BRIEF_CAL] ?: true,
                morningBriefWeather = prefs[KEY_BRIEF_WEATHER] ?: true,
                morningBriefNews = prefs[KEY_BRIEF_NEWS] ?: false,
                morningBriefTasks = prefs[KEY_BRIEF_TASKS] ?: true,
                voiceHandsFree = prefs[KEY_VOICE_HANDS_FREE] ?: false,
                backupIncludeApiKeys = prefs[KEY_BACKUP_KEYS] ?: false,
                internalLaunchToken = prefs[KEY_INTERNAL_TOKEN] ?: "",
                useDynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: false,
                displayName = prefs[KEY_DISPLAY_NAME] ?: "",
            )
        }

    fun current(): MentorPrefs = runBlocking { prefsFlow.first() }

    suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun update(transform: (MentorPrefs) -> MentorPrefs) {
        val next = transform(current())
        secureStore.setAppSecret(LlmSecureStore.SECRET_ELEVEN_LABS, next.elevenLabsApiKey)
        secureStore.setAppSecret(LlmSecureStore.SECRET_BACKUP_TOKEN, next.backupBearerToken)
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = next.themeMode.name
            prefs[KEY_ACCENT] = next.accentHue
            prefs[KEY_FONT_SCALE] = next.fontScale
            prefs[KEY_LLM_BASE] = next.llmBaseUrl
            prefs[KEY_LLM_MODEL] = next.llmModel
            prefs.remove(KEY_LLM_KEY)
            prefs[KEY_ACTIVE_LLM] = next.activeLlmProfileId
            prefs[KEY_ASSISTANT_PROMPT] = next.assistantSystemPrompt
            prefs[KEY_VOICE_LOCALE] = next.voiceLocaleTag
            prefs[KEY_APP_LANGUAGE] = next.appLanguageTag
            prefs[KEY_SPEAK] = next.speakResponses
            prefs.remove(KEY_ELEVEN_KEY)
            prefs[KEY_ELEVEN_VOICE] = next.elevenLabsVoiceId
            prefs[KEY_SYNC_URL] = next.syncWebSocketUrl
            prefs[KEY_DEVICE_ID] = next.syncDeviceId
            prefs[KEY_BACKUP_URL] = next.backupUploadUrl
            prefs.remove(KEY_BACKUP_TOKEN)
            prefs[KEY_FOCUS_TOPIC] = next.focusTopic
            prefs[KEY_BUILD_GOAL] = next.focusTopic
            prefs[KEY_CONVERSATION] = next.activeConversationId
            prefs[KEY_MODEL_PRESET] = next.modelPreset.name
            prefs[KEY_DAILY_BUDGET] = next.dailyBudgetUsd.toString()
            prefs[KEY_WEEKLY_BUDGET] = next.weeklyBudgetUsd.toString()
            prefs[KEY_ENABLED_PLUGINS] = next.enabledPluginIds
            prefs[KEY_SEEN_WELCOME] = next.hasSeenWelcome
            prefs[KEY_BIOMETRIC] = next.requireBiometricUnlock
            prefs[KEY_DAILY_BRIEF] = next.dailyBriefReminder
            prefs[KEY_GUIDED_SETUP] = next.hasCompletedGuidedSetup
            prefs[KEY_BRIEF_CAL] = next.morningBriefCalendar
            prefs[KEY_BRIEF_WEATHER] = next.morningBriefWeather
            prefs[KEY_BRIEF_NEWS] = next.morningBriefNews
            prefs[KEY_BRIEF_TASKS] = next.morningBriefTasks
            prefs[KEY_VOICE_HANDS_FREE] = next.voiceHandsFree
            prefs[KEY_BACKUP_KEYS] = next.backupIncludeApiKeys
            prefs[KEY_INTERNAL_TOKEN] = next.internalLaunchToken
            prefs[KEY_DYNAMIC_COLOR] = next.useDynamicColor
            prefs[KEY_DISPLAY_NAME] = next.displayName
        }
    }

    suspend fun ensureSecretsMigratedFromDataStore() {
        val snapshot = context.dataStore.data.first()
            val legacyEleven = snapshot[KEY_ELEVEN_KEY] ?: ""
            val legacyBackup = snapshot[KEY_BACKUP_TOKEN] ?: ""
            if (legacyEleven.isNotBlank() && secureStore.getAppSecret(LlmSecureStore.SECRET_ELEVEN_LABS).isBlank()) {
                secureStore.setAppSecret(LlmSecureStore.SECRET_ELEVEN_LABS, legacyEleven)
            }
            if (legacyBackup.isNotBlank() && secureStore.getAppSecret(LlmSecureStore.SECRET_BACKUP_TOKEN).isBlank()) {
                secureStore.setAppSecret(LlmSecureStore.SECRET_BACKUP_TOKEN, legacyBackup)
            }
        if (legacyEleven.isNotBlank() || legacyBackup.isNotBlank()) {
            context.dataStore.edit { prefs ->
                prefs.remove(KEY_ELEVEN_KEY)
                prefs.remove(KEY_BACKUP_TOKEN)
                prefs.remove(KEY_LLM_KEY)
            }
        }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_ACCENT = floatPreferencesKey("accent_hue")
        val KEY_FONT_SCALE = floatPreferencesKey("font_scale")
        val KEY_LLM_BASE = stringPreferencesKey("llm_base")
        val KEY_LLM_MODEL = stringPreferencesKey("llm_model")
        val KEY_LLM_KEY = stringPreferencesKey("llm_key")
        val KEY_ACTIVE_LLM = stringPreferencesKey("active_llm_profile")
        val KEY_ASSISTANT_PROMPT = stringPreferencesKey("assistant_prompt")
        val KEY_MENTOR_PROMPT = stringPreferencesKey("mentor_prompt")
        val KEY_FOCUS_TOPIC = stringPreferencesKey("focus_topic")
        val KEY_VOICE_LOCALE = stringPreferencesKey("voice_locale")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_SPEAK = booleanPreferencesKey("speak")
        val KEY_ELEVEN_KEY = stringPreferencesKey("eleven_key")
        val KEY_ELEVEN_VOICE = stringPreferencesKey("eleven_voice")
        val KEY_SYNC_URL = stringPreferencesKey("sync_url")
        val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        val KEY_BACKUP_URL = stringPreferencesKey("backup_url")
        val KEY_BACKUP_TOKEN = stringPreferencesKey("backup_token")
        val KEY_BUILD_GOAL = stringPreferencesKey("build_goal")
        val KEY_CONVERSATION = stringPreferencesKey("active_conversation")
        val KEY_MODEL_PRESET = stringPreferencesKey("model_preset")
        val KEY_DAILY_BUDGET = stringPreferencesKey("daily_budget_usd")
        val KEY_WEEKLY_BUDGET = stringPreferencesKey("weekly_budget_usd")
        val KEY_ENABLED_PLUGINS = stringSetPreferencesKey("enabled_plugins")
        val KEY_SEEN_WELCOME = booleanPreferencesKey("seen_welcome")
        val KEY_BIOMETRIC = booleanPreferencesKey("require_biometric")
        val KEY_DAILY_BRIEF = booleanPreferencesKey("daily_brief_reminder")
        val KEY_GUIDED_SETUP = booleanPreferencesKey("guided_setup_done")
        val KEY_BRIEF_CAL = booleanPreferencesKey("brief_calendar")
        val KEY_BRIEF_WEATHER = booleanPreferencesKey("brief_weather")
        val KEY_BRIEF_NEWS = booleanPreferencesKey("brief_news")
        val KEY_BRIEF_TASKS = booleanPreferencesKey("brief_tasks")
        val KEY_VOICE_HANDS_FREE = booleanPreferencesKey("voice_hands_free")
        val KEY_BACKUP_KEYS = booleanPreferencesKey("backup_include_api_keys")
        val KEY_INTERNAL_TOKEN = stringPreferencesKey("internal_launch_token")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
    }
}
