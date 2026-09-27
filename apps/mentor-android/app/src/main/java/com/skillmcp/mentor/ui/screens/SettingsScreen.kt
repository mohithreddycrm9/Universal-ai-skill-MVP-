package com.skillmcp.mentor.ui.screens

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.skillmcp.mentor.ui.components.settings.SettingsBlock
import com.skillmcp.mentor.ui.components.settings.SettingsDivider
import com.skillmcp.mentor.ui.components.settings.SettingsGroup
import com.skillmcp.mentor.ui.components.settings.SettingsNavRow
import com.skillmcp.mentor.ui.components.settings.SettingsSwitchRow
import com.skillmcp.mentor.ui.theme.BrandColors
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

@Composable
fun SettingsScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    val backupPrompt by vm.backupPassphrasePrompt.collectAsState()
    val restorePrompt by vm.backupRestorePassphrasePrompt.collectAsState()
    SettingsScreenContent(state = state, backupPrompt = backupPrompt, restorePrompt = restorePrompt, vm = vm)
}

/** Stateless settings UI; snapshot tests pass a no-op PreviewSettingsActions (src/test). */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    state: com.skillmcp.mentor.ui.MentorUiState,
    backupPrompt: Boolean,
    restorePrompt: Boolean,
    vm: com.skillmcp.mentor.ui.settings.SettingsScreenActions,
) {
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
    if (showPrivacy) {
        LegalDocumentSheet(
            title = stringResource(R.string.settings_privacy_policy),
            assetPath = "legal/privacy_policy.html",
            onDismiss = { showPrivacy = false },
        )
    }
    if (showTerms) {
        LegalDocumentSheet(
            title = stringResource(R.string.settings_terms),
            assetPath = "legal/terms_of_service.html",
            onDismiss = { showTerms = false },
        )
    }
    if (showLicenses) {
        LegalDocumentSheet(
            title = stringResource(R.string.settings_licenses),
            assetPath = "legal/open_source_licenses.html",
            onDismiss = { showLicenses = false },
        )
    }
    if (restorePrompt) {
        AlertDialog(
            onDismissRequest = vm::dismissRestorePassphrase,
            title = { Text(stringResource(R.string.settings_restore_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_restore_body))
                    OutlinedTextField(
                        value = restorePassphrase,
                        onValueChange = { restorePassphrase = it },
                        label = { Text(stringResource(R.string.settings_passphrase_optional)) },
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
                    Text(stringResource(R.string.action_restore))
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissRestorePassphrase) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
    if (backupPrompt) {
        AlertDialog(
            onDismissRequest = vm::dismissBackupPassphrase,
            title = { Text(stringResource(R.string.settings_backup_passphrase_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_backup_passphrase_body))
                    OutlinedTextField(
                        value = backupPassphrase,
                        onValueChange = { backupPassphrase = it },
                        label = { Text(stringResource(R.string.settings_passphrase)) },
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
                    Text(stringResource(R.string.action_upload))
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissBackupPassphrase) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text(stringResource(R.string.settings_erase_title)) },
            text = {
                Text(stringResource(R.string.settings_erase_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmErase = false
                        vm.wipeAllLocalData()
                    },
                ) {
                    Text(stringResource(R.string.action_erase))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmErase = false }) { Text(stringResource(R.string.action_cancel)) }
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

    var showBackup by remember { mutableStateOf(false) }
    var showVoiceAdvanced by remember { mutableStateOf(false) }
    AppBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = MentorDimens.ScreenHorizontal, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp).semantics { heading() },
            )
            SettingsProfileHeader(
                displayName = prefs.displayName,
                profile = state.activeLlmProfile,
                onClick = { vm.requestOpenTab("personalization") },
            )

            SettingsGroup(title = stringResource(R.string.settings_group_assistant)) {
                SettingsNavRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.personalization_title),
                    subtitle = stringResource(R.string.settings_personalization_sub),
                    onClick = { vm.requestOpenTab("personalization") },
                    tint = BrandColors.Coral,
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Rounded.Hub,
                    title = stringResource(R.string.settings_models_keys),
                    subtitle = state.activeLlmProfile?.let { "${it.name} · ${it.model}" }
                        ?: stringResource(R.string.settings_models_sub),
                    onClick = { vm.requestOpenTab("models") },
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Rounded.Extension,
                    title = stringResource(R.string.settings_add_abilities),
                    subtitle = stringResource(R.string.settings_abilities_sub),
                    onClick = { vm.requestOpenTab("skills") },
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Rounded.Insights,
                    title = stringResource(R.string.nav_activity),
                    subtitle = stringResource(R.string.settings_activity_sub),
                    onClick = { vm.requestOpenTab("usage") },
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_appearance)) {
                SettingsBlock {
                    Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.labelLarge)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = prefs.themeMode == mode,
                                onClick = { vm.updatePrefs { it.copy(themeMode = mode) } },
                                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                                label = { Text(mode.localizedLabel(), maxLines = 1) },
                            )
                        }
                    }
                }
                SettingsSwitchRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.settings_dynamic_color),
                    subtitle = stringResource(R.string.settings_dynamic_color_sub),
                    checked = prefs.useDynamicColor,
                    onCheckedChange = { on -> vm.updatePrefs { p -> p.copy(useDynamicColor = on) } },
                )
                SettingsDivider()
                SettingsBlock {
                    Text(stringResource(R.string.settings_font_scale), style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = prefs.fontScale,
                        onValueChange = { vm.updatePrefs { p -> p.copy(fontScale = it) } },
                        valueRange = 0.85f..1.35f,
                    )
                    if (!prefs.useDynamicColor) {
                        Text(stringResource(R.string.settings_accent_hue), style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = prefs.accentHue,
                            onValueChange = { vm.updatePrefs { p -> p.copy(accentHue = it) } },
                            valueRange = 0f..360f,
                        )
                    }
                }
                SettingsDivider()
                SettingsBlock {
                    Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "system" to stringResource(R.string.settings_language_system),
                            "en" to "English",
                            "hi" to "हिन्दी",
                            "te" to "తెలుగు",
                            "ta" to "தமிழ்",
                        ).forEach { (tag, label) ->
                            FilterChip(
                                selected = prefs.appLanguageTag == tag,
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
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            SettingsGroup(title = stringResource(R.string.settings_voice)) {
                SettingsSwitchRow(
                    icon = Icons.Rounded.RecordVoiceOver,
                    title = stringResource(R.string.settings_speak_replies),
                    subtitle = stringResource(R.string.settings_speak_replies_sub),
                    checked = prefs.speakResponses,
                    onCheckedChange = { vm.updatePrefs { p -> p.copy(speakResponses = it) } },
                )
                SettingsDivider()
                SettingsSwitchRow(
                    icon = Icons.Rounded.Mic,
                    title = stringResource(R.string.settings_hands_free),
                    subtitle = stringResource(R.string.settings_hands_free_sub),
                    checked = prefs.voiceHandsFree,
                    onCheckedChange = { vm.updatePrefs { p -> p.copy(voiceHandsFree = it) } },
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.settings_voice_advanced),
                    subtitle = stringResource(R.string.settings_voice_advanced_sub),
                    onClick = { showVoiceAdvanced = !showVoiceAdvanced },
                    showChevron = false,
                )
                AnimatedVisibility(showVoiceAdvanced) {
                    SettingsBlock {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = prefs.voiceLocaleTag,
                            onValueChange = { vm.updatePrefs { p -> p.copy(voiceLocaleTag = it) } },
                            label = { Text(stringResource(R.string.settings_voice_locale)) },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = prefs.elevenLabsApiKey,
                            onValueChange = { vm.updatePrefs { p -> p.copy(elevenLabsApiKey = it) } },
                            label = { Text(stringResource(R.string.settings_eleven_key)) },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = prefs.elevenLabsVoiceId,
                            onValueChange = { vm.updatePrefs { p -> p.copy(elevenLabsVoiceId = it) } },
                            label = { Text(stringResource(R.string.settings_eleven_voice)) },
                            singleLine = true,
                        )
                    }
                }
            }

            SettingsGroup(title = stringResource(R.string.settings_morning_brief)) {
                SettingsSwitchRow(
                    icon = Icons.Rounded.WbSunny,
                    title = stringResource(R.string.settings_brief_reminder),
                    subtitle = stringResource(R.string.settings_brief_reminder_sub),
                    checked = prefs.dailyBriefReminder,
                    onCheckedChange = onDailyBriefToggle,
                )
                SettingsDivider()
                SettingsSwitchRow(
                    icon = null,
                    title = stringResource(R.string.settings_brief_tasks),
                    checked = prefs.morningBriefTasks,
                    onCheckedChange = { on -> vm.updatePrefs { p -> p.copy(morningBriefTasks = on) } },
                )
                SettingsSwitchRow(
                    icon = null,
                    title = stringResource(R.string.settings_brief_calendar),
                    subtitle = stringResource(R.string.settings_brief_calendar_sub),
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
                SettingsSwitchRow(
                    icon = null,
                    title = stringResource(R.string.settings_brief_weather),
                    checked = prefs.morningBriefWeather,
                    onCheckedChange = { on -> vm.updatePrefs { p -> p.copy(morningBriefWeather = on) } },
                )
                if (prefs.morningBriefWeather) {
                    SettingsBlock {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = prefs.weatherCity,
                            onValueChange = { city -> vm.updatePrefs { p -> p.copy(weatherCity = city) } },
                            label = { Text(stringResource(R.string.settings_brief_city)) },
                            singleLine = true,
                        )
                    }
                }
                SettingsSwitchRow(
                    icon = null,
                    title = stringResource(R.string.settings_brief_news),
                    checked = prefs.morningBriefNews,
                    onCheckedChange = { on -> vm.updatePrefs { p -> p.copy(morningBriefNews = on) } },
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_group_privacy)) {
                SettingsSwitchRow(
                    icon = Icons.Rounded.Fingerprint,
                    title = stringResource(R.string.settings_app_lock),
                    subtitle = if (appLockAuth.canPrompt) null else stringResource(R.string.settings_app_lock_unavailable),
                    checked = prefs.requireBiometricUnlock,
                    onCheckedChange = vm::setRequireBiometric,
                    enabled = appLockAuth.canPrompt,
                )
                SettingsDivider()
                SettingsSwitchRow(
                    icon = Icons.Rounded.QueryStats,
                    title = stringResource(R.string.settings_analytics),
                    subtitle = stringResource(R.string.settings_analytics_sub),
                    checked = analyticsOptIn,
                    onCheckedChange = {
                        analyticsOptIn = it
                        vm.setAnalyticsOptIn(it)
                    },
                )
                SettingsDivider()
                SettingsSwitchRow(
                    icon = Icons.Rounded.BugReport,
                    title = stringResource(R.string.settings_crash),
                    subtitle = stringResource(R.string.settings_crash_sub),
                    checked = prefs.crashReportingOptIn,
                    onCheckedChange = vm::setCrashReportingOptIn,
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_group_data)) {
                SettingsNavRow(
                    icon = Icons.Rounded.Description,
                    title = stringResource(R.string.settings_export_markdown),
                    subtitle = stringResource(R.string.settings_export_sub),
                    onClick = vm::exportChatsMarkdown,
                    showChevron = false,
                )
                state.lastExportMarkdown?.let { md ->
                    SettingsBlock {
                        SelectionContainer {
                            OutlinedTextField(
                                value = md.take(4000),
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.settings_export_preview)) },
                                minLines = 4,
                            )
                        }
                        TextButton(onClick = vm::clearExportMarkdown) { Text(stringResource(R.string.settings_clear_export)) }
                    }
                }
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Rounded.CloudSync,
                    title = stringResource(R.string.settings_sync_backup),
                    subtitle = stringResource(R.string.settings_backup_sub),
                    onClick = { showBackup = !showBackup },
                    showChevron = false,
                )
                AnimatedVisibility(showBackup) {
                    SettingsBlock {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = prefs.syncWebSocketUrl,
                            onValueChange = vm::updateSyncWebSocketUrl,
                            label = { Text(stringResource(R.string.settings_sync_url)) },
                            supportingText = { Text(stringResource(R.string.settings_sync_url_hint)) },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = prefs.backupUploadUrl,
                            onValueChange = vm::updateBackupUploadUrl,
                            label = { Text(stringResource(R.string.settings_backup_url)) },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = prefs.backupBearerToken,
                            onValueChange = { vm.updatePrefs { p -> p.copy(backupBearerToken = it) } },
                            label = { Text(stringResource(R.string.settings_backup_token)) },
                            singleLine = true,
                        )
                        SettingsSwitchRow(
                            icon = null,
                            title = stringResource(R.string.settings_include_keys),
                            subtitle = stringResource(R.string.settings_include_keys_sub),
                            checked = prefs.backupIncludeApiKeys,
                            onCheckedChange = { on -> vm.updatePrefs { p -> p.copy(backupIncludeApiKeys = on) } },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = vm::promptBackupPassphrase) { Text(stringResource(R.string.settings_run_backup)) }
                            OutlinedButton(onClick = vm::promptRestorePassphrase) { Text(stringResource(R.string.action_restore)) }
                        }
                    }
                }
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Rounded.DeleteForever,
                    title = stringResource(R.string.settings_erase_all),
                    subtitle = stringResource(R.string.settings_erase_sub),
                    onClick = { confirmErase = true },
                    tint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error,
                    showChevron = false,
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_about)) {
                SettingsNavRow(
                    icon = Icons.Rounded.Shield,
                    title = stringResource(R.string.settings_privacy_policy),
                    onClick = { showPrivacy = true },
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Rounded.Public,
                    title = stringResource(R.string.settings_privacy_web),
                    onClick = {
                        val url = context.getString(com.skillmcp.mentor.R.string.privacy_policy_url)
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Rounded.Gavel,
                    title = stringResource(R.string.settings_terms),
                    onClick = { showTerms = true },
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Rounded.Code,
                    title = stringResource(R.string.settings_licenses),
                    onClick = { showLicenses = true },
                )
                SettingsDivider()
                val supportEmail = context.getString(com.skillmcp.mentor.R.string.support_email)
                SettingsNavRow(
                    icon = Icons.Rounded.Email,
                    title = stringResource(R.string.settings_contact_support),
                    subtitle = supportEmail,
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:$supportEmail") },
                        )
                    },
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.app_name),
                    subtitle = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    onClick = {},
                    showChevron = false,
                )
            }
            Text(
                stringResource(R.string.settings_keys_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            TabSuggestions(
                title = stringResource(R.string.settings_ask),
                suggestions = ScreenSuggestions.forScreen(SuggestionScreen.SETTINGS),
                onSelect = vm::openChatWithSuggestion,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeMode.localizedLabel(): String =
    when (this) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }
