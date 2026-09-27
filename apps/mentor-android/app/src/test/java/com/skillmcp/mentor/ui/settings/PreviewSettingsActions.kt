package com.skillmcp.mentor.ui.settings

import com.skillmcp.mentor.data.MentorPrefs
import com.skillmcp.mentor.llm.ModelPreset

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
