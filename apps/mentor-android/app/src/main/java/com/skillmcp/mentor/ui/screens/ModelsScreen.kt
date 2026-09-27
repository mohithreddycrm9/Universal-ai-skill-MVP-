package com.skillmcp.mentor.ui.screens

import com.skillmcp.mentor.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import com.skillmcp.mentor.llm.connectionLabel
import com.skillmcp.mentor.llm.isConfigured
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.GlassCard
import com.skillmcp.mentor.mentor.ScreenSuggestions
import com.skillmcp.mentor.mentor.SuggestionScreen
import com.skillmcp.mentor.ui.components.SecondaryScreenTopBar
import com.skillmcp.mentor.ui.components.TabSuggestions
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(vm: MentorViewModel, onBack: (() -> Unit)? = null) {
    val state by vm.uiState.collectAsState()
    var showAddKind by remember { mutableStateOf(false) }

    AppBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                if (onBack != null) {
                    SecondaryScreenTopBar(
                        title = stringResource(R.string.models_title),
                        subtitle = stringResource(R.string.models_subtitle),
                        onBack = onBack,
                    )
                }
            },
        ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (onBack == null) {
                item {
                    com.skillmcp.mentor.ui.components.ScreenHeader(
                        title = stringResource(R.string.models_title),
                        subtitle = stringResource(R.string.models_subtitle),
                    )
                }
            }
            item {
                TabSuggestions(
                    title = stringResource(R.string.models_ask),
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
                        Text(stringResource(R.string.models_add_openai_compatible))
                    }
                } else {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.models_custom_endpoint), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
}

@Composable
private fun ProviderCard(
    profile: LlmProfile,
    selected: Boolean,
    onSelectActive: () -> Unit,
    onConnect: () -> Unit,
) {
    val configured = profile.isConfigured()
    val statusColor =
        if (configured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.Canvas(Modifier.size(10.dp)) {
                drawCircle(color = statusColor)
            }
            RowWithRadio(
                modifier = Modifier.weight(1f),
                selected = selected,
                onSelect = onSelectActive,
                title = profile.name,
                subtitle = "${profile.kind.label} · ${profile.model}",
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
        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
            Text(if (configured) "Manage connection" else "Connect")
        }
    }
}

@Composable
private fun RowWithRadio(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onSelect: () -> Unit,
    title: String,
    subtitle: String?,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RadioButton(selected = selected, onClick = onSelect)
        Column {
            Text(title, fontWeight = FontWeight.Medium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
