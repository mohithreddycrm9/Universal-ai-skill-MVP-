package com.skillmcp.mentor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.llm.LlmProfile
import com.skillmcp.mentor.llm.LlmProviderKind
import com.skillmcp.mentor.llm.connectionLabel
import com.skillmcp.mentor.llm.isConfigured
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.GlassCard
import com.skillmcp.mentor.mentor.ScreenSuggestions
import com.skillmcp.mentor.mentor.SuggestionScreen
import com.skillmcp.mentor.ui.components.ScreenHeader
import com.skillmcp.mentor.ui.components.TabSuggestions
import java.util.UUID

@Composable
fun ModelsScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    var showAddKind by remember { mutableStateOf(false) }

    AppBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ScreenHeader(
                    title = "Models",
                    subtitle = "Choose a provider, sign in with Google, email, phone, or paste an API key.",
                )
            }
            item {
                TabSuggestions(
                    title = "Ask about models",
                    suggestions = ScreenSuggestions.forScreen(SuggestionScreen.MODELS),
                    onSelect = vm::openChatWithSuggestion,
                )
            }
            items(state.llmProfiles, key = { it.id }) { profile ->
                val selected = state.prefs.activeLlmProfileId == profile.id
                ProviderCard(
                    profile = profile,
                    selected = selected,
                    onSelectActive = { vm.selectLlmProfile(profile.id) },
                    onConnect = { vm.openConnectLlm(profile.id) },
                )
            }
            item {
                if (!showAddKind) {
                    OutlinedButton(onClick = { showAddKind = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Add custom OpenAI-compatible endpoint")
                    }
                } else {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Custom endpoint", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            LlmProviderKind.entries.filter { it == LlmProviderKind.OPENAI_COMPAT || it == LlmProviderKind.OLLAMA }
                                .forEach { kind ->
                                    Button(
                                        onClick = {
                                            val id = "custom-${UUID.randomUUID()}"
                                            val draft =
                                                LlmProfile(
                                                    id = id,
                                                    name = if (kind == LlmProviderKind.OLLAMA) "My Ollama" else "Custom API",
                                                    kind = kind,
                                                    baseUrl =
                                                        if (kind == LlmProviderKind.OLLAMA) {
                                                            "http://10.0.2.2:11434/"
                                                        } else {
                                                            "https://api.openai.com/v1/"
                                                        },
                                                    model = if (kind == LlmProviderKind.OLLAMA) "llama3.2" else "gpt-4o-mini",
                                                )
                                            vm.saveLlmProfile(draft)
                                            vm.openConnectLlm(id)
                                            showAddKind = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(kind.label)
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    profile: LlmProfile,
    selected: Boolean,
    onSelectActive: () -> Unit,
    onConnect: () -> Unit,
) {
    val configured = profile.isConfigured()
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowWithRadio(
                selected = selected,
                onSelect = onSelectActive,
                title = profile.name,
                subtitle = "${profile.kind.label} · ${profile.model}",
            )
            AssistChip(
                onClick = onConnect,
                label = { Text(if (configured) "Manage" else "Connect") },
            )
        }
        Text(
            profile.connectionLabel(),
            style = MaterialTheme.typography.bodySmall,
            color =
                if (configured) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            modifier = Modifier.padding(start = 48.dp, bottom = 8.dp),
        )
        OutlinedButton(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
            Text(if (configured) "Update login or API key" else "Connect account")
        }
    }
}

@Composable
private fun RowWithRadio(
    selected: Boolean,
    onSelect: () -> Unit,
    title: String,
    subtitle: String?,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RadioButton(selected = selected, onClick = onSelect)
        Column {
            Text(title, fontWeight = FontWeight.Medium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
