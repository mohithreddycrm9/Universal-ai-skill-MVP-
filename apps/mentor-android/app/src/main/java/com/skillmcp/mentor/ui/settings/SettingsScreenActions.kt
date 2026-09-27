package com.skillmcp.mentor.ui.settings

import com.skillmcp.mentor.data.MentorPrefs
import com.skillmcp.mentor.llm.ModelPreset

/** Settings screen callbacks; implemented by the ViewModel, no-op in snapshot tests (PreviewSettingsActions, src/test). */
interface SettingsScreenActions {
    fun analyticsOptIn(): Boolean
    fun setAnalyticsOptIn(enabled: Boolean)
    fun setCrashReportingOptIn(enabled: Boolean)
    fun setDailyBriefReminder(enabled: Boolean)
    fun setRequireBiometric(enabled: Boolean)
    fun updatePrefs(transform: (MentorPrefs) -> MentorPrefs)
    fun setModelPreset(preset: ModelPreset)
    fun updateFocusTopic(topic: String)
    fun updateSyncWebSocketUrl(url: String)
    fun updateBackupUploadUrl(url: String)
    fun openChatWithSuggestion(prompt: String)
    fun requestOpenTab(route: String)
    fun exportChatsMarkdown()
    fun clearExportMarkdown()
    fun promptBackupPassphrase()
    fun dismissBackupPassphrase()
    fun runBackupNow(passphrase: CharArray? = null)
    fun promptRestorePassphrase()
    fun dismissRestorePassphrase()
    fun runRestoreNow(passphrase: CharArray? = null)
    fun wipeAllLocalData()
}
