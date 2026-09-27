package com.skillmcp.mentor.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.skillmcp.mentor.R
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.skillmcp.mentor.security.appLockAuthenticators
import com.skillmcp.mentor.BuildConfig
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.mentor.ScreenSuggestions
import com.skillmcp.mentor.mentor.SuggestionScreen
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.LegalDocumentSheet
import com.skillmcp.mentor.ui.components.TabSuggestions
import com.skillmcp.mentor.ui.components.settings.SettingsProfileHeader
import com.skillmcp.mentor.ui.theme.MentorDimens
import com.skillmcp.mentor.ui.theme.ThemeMode
import com.skillmcp.mentor.ui.theme.userLabel

@Composable
fun SettingsScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    val prefs = state.prefs
    val scroll = rememberScrollState()
    val context = LocalContext.current
    val appLockAuth = remember { appLockAuthenticators(context) }
    var analyticsOptIn by remember { mutableStateOf(vm.analyticsOptIn()) }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) vm.setDailyBriefReminder(true)
        }
    val calendarPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                vm.updatePrefs { p -> p.copy(morningBriefCalendar = true) }
            }
        }
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var confirmErase by remember { mutableStateOf(false) }
    var backupPassphrase by remember { mutableStateOf("") }
    var restorePassphrase by remember { mutableStateOf("") }
    val backupPrompt by vm.backupPassphrasePrompt.collectAsState()
    val restorePrompt by vm.backupRestorePassphrasePrompt.collectAsState()
    if (showPrivacy) {
        LegalDocumentSheet(
            title = "Privacy policy",
            assetPath = "legal/privacy_policy.html",
            onDismiss = { showPrivacy = false },
        )
    }
    if (showTerms) {
        LegalDocumentSheet(
            title = "Terms of use",
            assetPath = "legal/terms_of_service.html",
            onDismiss = { showTerms = false },
        )
    }
    if (showLicenses) {
        LegalDocumentSheet(
            title = "Open source licenses",
            assetPath = "legal/open_source_licenses.html",
            onDismiss = { showLicenses = false },
        )
    }
    if (restorePrompt) {
        AlertDialog(
            onDismissRequest = vm::dismissRestorePassphrase,
            title = { Text("Restore backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Downloads the ciphertext from your backup URL and merges chats, messages, and skills. " +
                            "Use the same passphrase you used when uploading a v2 backup.",
                    )
                    OutlinedTextField(
                        value = restorePassphrase,
                        onValueChange = { restorePassphrase = it },
                        label = { Text("Passphrase (if required)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val phrase = restorePassphrase.toCharArray()
                        restorePassphrase = ""
                        vm.runRestoreNow(if (phrase.isNotEmpty()) phrase else null)
                    },
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissRestorePassphrase) { Text("Cancel") }
            },
        )
    }
    if (backupPrompt) {
        AlertDialog(
            onDismissRequest = vm::dismissBackupPassphrase,
            title = { Text("Backup passphrase") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Optional but recommended: encrypt this backup with a passphrase only you know.")
                    OutlinedTextField(
                        value = backupPassphrase,
                        onValueChange = { backupPassphrase = it },
                        label = { Text("Passphrase") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val phrase = backupPassphrase.toCharArray()
                        backupPassphrase = ""
                        vm.runBackupNow(if (phrase.isNotEmpty()) phrase else null)
                    },
                ) {
                    Text("Upload")
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissBackupPassphrase) { Text("Cancel") }
            },
        )
    }
    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Erase all local data?") },
            text = {
                Text(
                    "Deletes chats, usage history, saved prompts, skills, and all stored API keys on this device. " +
                        "This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmErase = false
                        vm.wipeAllLocalData()
                    },
                ) {
                    Text("Erase")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmErase = false }) { Text("Cancel") }
            },
        )
    }
    val onDailyBriefToggle: (Boolean) -> Unit = { enabled ->
        if (!enabled) {
            vm.setDailyBriefReminder(false)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (granted) {
                vm.setDailyBriefReminder(true)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            vm.setDailyBriefReminder(true)
        }
    }

    AppBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(MentorDimens.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsProfileHeader(
                displayName = prefs.displayName,
                profile = state.activeLlmProfile,
            )
            com.skillmcp.mentor.ui.components.ScreenHeader(
                title = "Settings",
                subtitle = "Personalize your assistant, voice, and sync.",
            )
            OutlinedTextField(
                value = prefs.displayName,
                onValueChange = { name -> vm.updatePrefs { p -> p.copy(displayName = name) } },
                label = { Text("Your name (greeting)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            TabSuggestions(
                title = "Ask about settings",
                suggestions = ScreenSuggestions.forScreen(SuggestionScreen.SETTINGS),
                onSelect = vm::openChatWithSuggestion,
            )

            Text("Privacy & convenience", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            RowSwitch(
                label = "Require biometric unlock",
                checked = prefs.requireBiometricUnlock,
                onCheckedChange = vm::setRequireBiometric,
                enabled = appLockAuth.canPrompt,
            )
            if (!appLockAuth.canPrompt) {
                Text(
                    "Set a screen lock (PIN, pattern, or password) in Android Settings to enable app lock.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RowSwitch(
                label = "Daily morning brief reminder",
                checked = prefs.dailyBriefReminder,
                onCheckedChange = onDailyBriefToggle,
            )
            Text("Morning brief content", style = MaterialTheme.typography.titleSmall)
            RowSwitch(
                label = "Tasks & priorities",
                checked = prefs.morningBriefTasks,
                onCheckedChange = { on -> vm.updatePrefs { p -> p.copy(morningBriefTasks = on) } },
            )
            RowSwitch(
                label = "Calendar (events shared with your AI provider in the brief)",
                checked = prefs.morningBriefCalendar,
                onCheckedChange = { on ->
                    if (!on) {
                        vm.updatePrefs { p -> p.copy(morningBriefCalendar = false) }
                    } else if (
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                            PackageManager.PERMISSION_GRANTED
                    ) {
                        vm.updatePrefs { p -> p.copy(morningBriefCalendar = true) }
                    } else {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    }
                },
            )
            RowSwitch(
                label = "Weather",
                checked = prefs.morningBriefWeather,
                onCheckedChange = { on -> vm.updatePrefs { p -> p.copy(morningBriefWeather = on) } },
            )
            if (prefs.morningBriefWeather) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = prefs.weatherCity,
                    onValueChange = { city -> vm.updatePrefs { p -> p.copy(weatherCity = city) } },
                    label = { Text("Weather city (e.g. Hyderabad)") },
                    singleLine = true,
                )
            }
            RowSwitch(
                label = "News headlines",
                checked = prefs.morningBriefNews,
                onCheckedChange = { on -> vm.updatePrefs { p -> p.copy(morningBriefNews = on) } },
            )
            RowSwitch(
                label = "Help improve Discover (on-device only)",
                checked = analyticsOptIn,
                onCheckedChange = {
                    analyticsOptIn = it
                    vm.setAnalyticsOptIn(it)
                },
            )
            RowSwitch(
                label = "Send anonymous crash logs (opt-in)",
                checked = prefs.crashReportingOptIn,
                onCheckedChange = vm::setCrashReportingOptIn,
            )
            Text(
                "When enabled, uncaught errors are recorded locally to help diagnose crashes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RowSwitch(
                label = "Hands-free voice (listen after each reply)",
                checked = prefs.voiceHandsFree,
                onCheckedChange = { vm.updatePrefs { p -> p.copy(voiceHandsFree = it) } },
            )
            OutlinedButton(onClick = vm::exportChatsMarkdown, modifier = Modifier.fillMaxWidth()) {
                Text("Export chats as Markdown")
            }
            state.lastExportMarkdown?.let { md ->
                SelectionContainer {
                    OutlinedTextField(
                        value = md.take(4000),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Export preview (copy)") },
                        minLines = 4,
                    )
                }
                TextButton(onClick = vm::clearExportMarkdown) { Text("Clear export") }
            }
            Text("Advanced", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            OutlinedButton(onClick = { vm.requestOpenTab("models") }, modifier = Modifier.fillMaxWidth()) {
                Text("Models & API keys")
            }
            OutlinedButton(onClick = { vm.requestOpenTab("skills") }, modifier = Modifier.fillMaxWidth()) {
                Text("Add abilities & tools")
            }
            OutlinedButton(onClick = { vm.requestOpenTab("usage") }, modifier = Modifier.fillMaxWidth()) {
                Text("Activity")
            }
            Text(
                stringResource(R.string.settings_widget_hint_prefix) +
                    stringResource(R.string.widget_title) +
                    ".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { showPrivacy = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Privacy policy (in app)")
            }
            OutlinedButton(
                onClick = {
                    val url = context.getString(com.skillmcp.mentor.R.string.privacy_policy_url)
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Privacy policy (web)")
            }
            OutlinedButton(onClick = { showTerms = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Terms of use")
            }
            OutlinedButton(onClick = { showLicenses = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Open source licenses")
            }
            OutlinedButton(
                onClick = { confirmErase = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Erase all local data")
            }
            Text(
                "API keys and backup tokens live in EncryptedSharedPreferences (Android Keystore). " +
                    "Turn on “Include API keys in encrypted backup” to export them inside the ciphertext.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Text("${stringResource(R.string.app_name)} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            val supportEmail = context.getString(com.skillmcp.mentor.R.string.support_email)
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$supportEmail")
                        },
                    )
                },
            ) {
                Text(
                    context.getString(com.skillmcp.mentor.R.string.support_email_label),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Text("Language", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            listOf("system" to "System", "en" to "English", "hi" to "हिन्दी", "te" to "తెలుగు", "ta" to "தமிழ்").forEach { (tag, label) ->
                val selected = prefs.appLanguageTag == tag
                if (selected) {
                    Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                        Text("✓ $label")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            vm.updatePrefs { p ->
                                val voice =
                                    when (tag) {
                                        "hi" -> "hi-IN"
                                        "te" -> "te-IN"
                                        "ta" -> "ta-IN"
                                        "en" -> "en-US"
                                        else -> p.voiceLocaleTag
                                    }
                                p.copy(appLanguageTag = tag, voiceLocaleTag = voice)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(label)
                    }
                }
            }

            Text("Response style", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            com.skillmcp.mentor.llm.ModelPreset.entries.forEach { preset ->
                val selected = state.prefs.modelPreset == preset
                if (selected) {
                    Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                        Text("✓ ${preset.label} — ${preset.hint}")
                    }
                } else {
                    OutlinedButton(
                        onClick = { vm.setModelPreset(preset) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("${preset.label} — ${preset.hint}")
                    }
                }
            }

            Text("Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = prefs.focusTopic,
                onValueChange = vm::updateFocusTopic,
                label = { Text("Focus topic (optional)") },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = prefs.assistantSystemPrompt,
                onValueChange = { vm.updatePrefs { p -> p.copy(assistantSystemPrompt = it) } },
                label = { Text("System instructions") },
                minLines = 3,
            )
            Text(
                "Manage API keys and models under Settings → Models & API keys.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            RowSwitch(
                label = "Use wallpaper colours (Material You)",
                checked = prefs.useDynamicColor,
                onCheckedChange = { on -> vm.updatePrefs { p -> p.copy(useDynamicColor = on) } },
            )
            ThemeMode.entries.forEach { mode ->
                val selected = prefs.themeMode == mode
                if (selected) {
                    Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                        Text("✓ ${mode.userLabel()}")
                    }
                } else {
                    OutlinedButton(
                        onClick = { vm.updatePrefs { it.copy(themeMode = mode) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(mode.userLabel())
                    }
                }
            }
            Text("Accent hue")
            Slider(
                value = prefs.accentHue,
                onValueChange = { vm.updatePrefs { p -> p.copy(accentHue = it) } },
                valueRange = 0f..360f,
            )
            Text("Font scale")
            Slider(
                value = prefs.fontScale,
                onValueChange = { vm.updatePrefs { p -> p.copy(fontScale = it) } },
                valueRange = 0.85f..1.35f,
            )

            Text("Voice", style = MaterialTheme.typography.titleMedium)
            RowSwitch(
                label = "Speak replies aloud",
                checked = prefs.speakResponses,
                onCheckedChange = { vm.updatePrefs { p -> p.copy(speakResponses = it) } },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = prefs.voiceLocaleTag,
                onValueChange = { vm.updatePrefs { p -> p.copy(voiceLocaleTag = it) } },
                label = { Text("Voice locale (e.g. en-US, es-ES)") },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = prefs.elevenLabsApiKey,
                onValueChange = { vm.updatePrefs { p -> p.copy(elevenLabsApiKey = it) } },
                label = { Text("ElevenLabs API key (optional)") },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = prefs.elevenLabsVoiceId,
                onValueChange = { vm.updatePrefs { p -> p.copy(elevenLabsVoiceId = it) } },
                label = { Text("ElevenLabs voice id") },
            )

            Text("Sync & backup", style = MaterialTheme.typography.titleMedium)
            Text(
                "Optional: point to your own WebSocket sync relay (for example ws://YOUR_IP:8787/sync).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = prefs.syncWebSocketUrl,
                onValueChange = { vm.updatePrefs { p -> p.copy(syncWebSocketUrl = it) } },
                label = { Text("WebSocket sync URL") },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = prefs.backupUploadUrl,
                onValueChange = vm::updateBackupUploadUrl,
                label = { Text("Encrypted backup PUT URL") },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = prefs.backupBearerToken,
                onValueChange = { vm.updatePrefs { p -> p.copy(backupBearerToken = it) } },
                label = { Text("Backup bearer token") },
            )
            RowSwitch(
                label = "Include API keys in encrypted backup",
                checked = prefs.backupIncludeApiKeys,
                onCheckedChange = { on -> vm.updatePrefs { p -> p.copy(backupIncludeApiKeys = on) } },
            )
            Text(
                "Keys are encrypted with your passphrase or device key — never sent in plain text.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = vm::promptBackupPassphrase, modifier = Modifier.fillMaxWidth()) {
                Text("Run backup now")
            }
            OutlinedButton(onClick = vm::promptRestorePassphrase, modifier = Modifier.fillMaxWidth()) {
                Text("Restore from backup URL")
            }
        }
    }
}

@Composable
private fun RowSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
