package com.skillmcp.mentor.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.llm.defaultLlmProfiles
import com.skillmcp.mentor.mentor.UiMessage
import com.skillmcp.mentor.ui.MentorUiState
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.chat.ChatMessageContent
import com.skillmcp.mentor.ui.components.chat.PremiumComposerBar
import com.skillmcp.mentor.ui.components.ScreenHeader
import com.skillmcp.mentor.ui.components.TabSuggestions
import com.skillmcp.mentor.ui.screens.DiscoverScreenContent
import com.skillmcp.mentor.ui.theme.CodeMentorTheme

object MainScreenPreviewSamples {
    fun chatState(): MentorUiState =
        MentorUiState(
            messages =
                listOf(
                    UiMessage("1", "user", "Help me plan meals for the week."),
                    UiMessage(
                        "2",
                        "assistant",
                        "Here is a simple plan with vegetarian options and a grocery list.",
                    ),
                ),
            activeLlmProfile = defaultLlmProfiles().first(),
            draft = "Add lunch ideas",
        )

    fun discoverState(): MentorUiState =
        MentorUiState(
            activeLlmProfile = defaultLlmProfiles().first(),
            usageTotals =
                com.skillmcp.mentor.llm.UsageTotals(
                    requestCount = 12,
                    promptTokens = 4000,
                    completionTokens = 900,
                    estimatedUsd = 0.42,
                ),
        )

    fun settingsState(): MentorUiState =
        MentorUiState(
            prefs =
                com.skillmcp.mentor.data.MentorPrefs(
                    requireBiometricUnlock = false,
                    dailyBriefReminder = true,
                    morningBriefCalendar = true,
                ),
        )
}

@Composable
fun ChatScreenPreview(darkTheme: Boolean) {
    CodeMentorTheme(darkTheme = darkTheme) {
        ChatScreenPreviewContent(state = MainScreenPreviewSamples.chatState())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatScreenPreviewContent(state: MentorUiState) {
    val profile = state.activeLlmProfile
    AppBackground {
        Column(Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("Universal AI", fontWeight = FontWeight.Bold)
                        profile?.let {
                            Text(
                                it.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Menu, contentDescription = null)
                    }
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    ChatMessageContent(
                        content = msg.content,
                        isUser = msg.role == "user",
                        isStreaming = false,
                        modelLabel = if (msg.role != "user") "OpenAI" else null,
                        estimatedCostUsd = null,
                    )
                }
            }
            PremiumComposerBar(
                draft = state.draft,
                onDraftChange = {},
                onSend = {},
                onMic = {},
                onAttach = null,
                isListening = false,
                isSending = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun DiscoverScreenPreview(darkTheme: Boolean) {
    CodeMentorTheme(darkTheme = darkTheme) {
        DiscoverScreenContent(
            state = MainScreenPreviewSamples.discoverState(),
            onConnect = {},
            onPopularUseCase = {},
            onOpenUsage = {},
            onOpenModels = {},
        )
    }
}

@Composable
fun SettingsScreenPreview(darkTheme: Boolean) {
    CodeMentorTheme(darkTheme = darkTheme) {
        val prefs = MainScreenPreviewSamples.settingsState().prefs
        AppBackground {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ScreenHeader(
                    title = "Settings",
                    subtitle = "Personalize your assistant, voice, and sync.",
                )
                TabSuggestions(
                    title = "Ask about settings",
                    suggestions = emptyList(),
                    onSelect = {},
                )
                Text("Privacy & convenience", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                PreviewRowSwitch(label = "Require biometric unlock", checked = prefs.requireBiometricUnlock)
                PreviewRowSwitch(label = "Daily morning brief reminder", checked = prefs.dailyBriefReminder)
                PreviewRowSwitch(label = "Calendar (events shared with AI provider)", checked = prefs.morningBriefCalendar)
            }
        }
    }
}

@Composable
private fun PreviewRowSwitch(label: String, checked: Boolean) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        Switch(checked = checked, onCheckedChange = {})
    }
}
