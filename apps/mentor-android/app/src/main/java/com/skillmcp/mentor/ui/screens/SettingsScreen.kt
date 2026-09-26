package com.skillmcp.mentor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.theme.ThemeMode

@Composable
fun SettingsScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    val prefs = state.prefs
    val scroll = rememberScrollState()

    AppBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            com.skillmcp.mentor.ui.components.ScreenHeader(
                title = "Settings",
                subtitle = "Personalize your assistant, voice, and sync.",
            )

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
                "Manage API keys and models in the Models tab.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            ThemeMode.entries.forEach { mode ->
                Button(
                    onClick = { vm.updatePrefs { it.copy(themeMode = mode) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = prefs.themeMode != mode,
                ) {
                    Text("Theme: ${mode.name}")
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
                "Run relay: cd apps/sync-relay && npm start → ws://YOUR_IP:8787/sync",
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
                onValueChange = { vm.updatePrefs { p -> p.copy(backupUploadUrl = it) } },
                label = { Text("Encrypted backup PUT URL") },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = prefs.backupBearerToken,
                onValueChange = { vm.updatePrefs { p -> p.copy(backupBearerToken = it) } },
                label = { Text("Backup bearer token") },
            )
            Button(onClick = vm::runBackupNow, modifier = Modifier.fillMaxWidth()) {
                Text("Run backup now")
            }
        }
    }
}

@Composable
private fun RowSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
