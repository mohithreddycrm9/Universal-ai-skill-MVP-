package com.skillmcp.mentor.ui.settings

import com.skillmcp.mentor.data.MentorPrefs
import com.skillmcp.mentor.llm.ModelPreset

/** Settings screen callbacks; implemented by the ViewModel, no-op in previews ([PreviewSettingsActions]). */
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

object PreviewSettingsActions : SettingsScreenActions {
    override fun analyticsOptIn(): Boolean = false
    override fun setAnalyticsOptIn(enabled: Boolean) = Unit
    override fun setCrashReportingOptIn(enabled: Boolean) = Unit
    override fun setDailyBriefReminder(enabled: Boolean) = Unit
    override fun setRequireBiometric(enabled: Boolean) = Unit
    override fun updatePrefs(transform: (MentorPrefs) -> MentorPrefs) = Unit
    override fun setModelPreset(preset: ModelPreset) = Unit
    override fun updateFocusTopic(topic: String) = Unit
    override fun updateSyncWebSocketUrl(url: String) = Unit
    override fun updateBackupUploadUrl(url: String) = Unit
    override fun openChatWithSuggestion(prompt: String) = Unit
    override fun requestOpenTab(route: String) = Unit
    override fun exportChatsMarkdown() = Unit
    override fun clearExportMarkdown() = Unit
    override fun promptBackupPassphrase() = Unit
    override fun dismissBackupPassphrase() = Unit
    override fun runBackupNow(passphrase: CharArray?) = Unit
    override fun promptRestorePassphrase() = Unit
    override fun dismissRestorePassphrase() = Unit
    override fun runRestoreNow(passphrase: CharArray?) = Unit
    override fun wipeAllLocalData() = Unit
}
