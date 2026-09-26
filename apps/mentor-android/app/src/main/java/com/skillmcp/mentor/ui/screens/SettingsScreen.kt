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
import com.skillmcp.mentor.ui.theme.ThemeMode

@Composable
fun SettingsScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    val prefs = state.prefs
    val scroll = rememberScrollState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

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

        Text("Build context", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = prefs.buildGoal,
            onValueChange = { vm.updateBuildGoal(it) },
            label = { Text("Current build goal") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.activeStep,
            onValueChange = vm::onActiveStepChange,
            label = { Text("Active step (for suggestion chips)") },
        )
        Button(onClick = vm::recordTestFailed) { Text("Simulate test_failed event") }

        Text("LLM", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = prefs.llmBaseUrl,
            onValueChange = { vm.updatePrefs { p -> p.copy(llmBaseUrl = it) } },
            label = { Text("OpenAI-compatible base URL") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = prefs.llmModel,
            onValueChange = { vm.updatePrefs { p -> p.copy(llmModel = it) } },
            label = { Text("Model") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = prefs.llmApiKey,
            onValueChange = { vm.updatePrefs { p -> p.copy(llmApiKey = it) } },
            label = { Text("API key") },
        )

        Text("Voice", style = MaterialTheme.typography.titleMedium)
        RowSwitch(
            label = "Speak mentor replies",
            checked = prefs.speakResponses,
            onCheckedChange = { vm.updatePrefs { p -> p.copy(speakResponses = it) } },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = prefs.voiceLocaleTag,
            onValueChange = { vm.updatePrefs { p -> p.copy(voiceLocaleTag = it) } },
            label = { Text("Voice locale (e.g. en-US, es-ES, hi-IN)") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = prefs.elevenLabsApiKey,
            onValueChange = { vm.updatePrefs { p -> p.copy(elevenLabsApiKey = it) } },
            label = { Text("ElevenLabs API key (optional cloned voice)") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = prefs.elevenLabsVoiceId,
            onValueChange = { vm.updatePrefs { p -> p.copy(elevenLabsVoiceId = it) } },
            label = { Text("ElevenLabs voice id") },
        )

        Text("Sync & backup", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = prefs.syncWebSocketUrl,
            onValueChange = { vm.updatePrefs { p -> p.copy(syncWebSocketUrl = it) } },
            label = { Text("WebSocket sync relay URL") },
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
