package com.skillmcp.mentor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.llm.HuggingFaceDefaults
import com.skillmcp.mentor.llm.HuggingFaceModelSummary
import com.skillmcp.mentor.llm.LlmProfile
import com.skillmcp.mentor.llm.LlmProviderKind
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.GlassCard
import com.skillmcp.mentor.ui.components.ScreenHeader
import kotlinx.coroutines.delay

@Composable
fun ModelsScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("My provider") }
    var baseUrl by remember { mutableStateOf("https://api.openai.com/v1/") }
    var model by remember { mutableStateOf("gpt-4o-mini") }
    var apiKey by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(LlmProviderKind.OPENAI_COMPAT) }

    LaunchedEffect(kind) {
        if (kind == LlmProviderKind.HUGGING_FACE) {
            baseUrl = HuggingFaceDefaults.ROUTER_BASE_URL
            model = HuggingFaceDefaults.featuredChatModels.first().first
            name = "Hugging Face model"
        }
    }

    AppBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ScreenHeader(
                    title = "Models",
                    subtitle = "Connect cloud APIs, Hugging Face Hub models, or a local runtime.",
                )
            }
            items(state.llmProfiles, key = { it.id }) { profile ->
                val selected = state.prefs.activeLlmProfileId == profile.id
                GlassCard {
                    RowWithRadio(
                        selected = selected,
                        onSelect = { vm.selectLlmProfile(profile.id) },
                        title = profile.name,
                        subtitle = "${profile.kind.label} · ${profile.model}",
                    )
                    if (profile.kind == LlmProviderKind.HUGGING_FACE) {
                        HuggingFaceModelPicker(
                            profile = profile,
                            onSave = vm::updateLlmProfile,
                            onSearch = vm::searchHuggingFaceModels,
                        )
                    }
                    OutlinedButton(onClick = { vm.testLlmProfile(profile) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Test connection")
                    }
                }
            }
            item {
                if (!showAdd) {
                    Button(
                        onClick = { showAdd = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text("Add provider")
                    }
                } else {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("New provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            LlmProviderKind.entries.forEach { k ->
                                RowWithRadio(selected = kind == k, onSelect = { kind = k }, title = k.label, subtitle = null)
                            }
                            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                            if (kind != LlmProviderKind.HUGGING_FACE) {
                                OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                            } else {
                                Text(
                                    "Runs via Hugging Face Inference (router). Add a token with Inference Providers permission.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedTextField(
                                model,
                                { model = it },
                                label = { Text("Model id") },
                                placeholder = { Text("org/model-name:fastest") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            )
                            OutlinedTextField(
                                apiKey,
                                { apiKey = it },
                                label = { Text(if (kind == LlmProviderKind.HUGGING_FACE) "Hugging Face token (hf_…)" else "API key") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            )
                            if (kind == LlmProviderKind.HUGGING_FACE) {
                                HuggingFaceModelPicker(
                                    profile =
                                        LlmProfile(
                                            id = "draft",
                                            name = name,
                                            kind = LlmProviderKind.HUGGING_FACE,
                                            baseUrl = baseUrl,
                                            model = model,
                                            apiKey = apiKey,
                                        ),
                                    onSave = { updated ->
                                        model = updated.model
                                        apiKey = updated.apiKey
                                    },
                                    onSearch = vm::searchHuggingFaceModels,
                                    pickOnly = true,
                                )
                            }
                            Button(
                                onClick = {
                                    vm.addCustomLlmProfile(name, kind, baseUrl, model, apiKey)
                                    showAdd = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Save & activate")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HuggingFaceModelPicker(
    profile: LlmProfile,
    onSave: (LlmProfile) -> Unit,
    onSearch: (String, String, (List<HuggingFaceModelSummary>) -> Unit) -> Unit,
    pickOnly: Boolean = false,
) {
    var token by remember(profile.id) { mutableStateOf(profile.apiKey) }
    var modelId by remember(profile.id, profile.model) { mutableStateOf(profile.model) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<HuggingFaceModelSummary>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery, token) {
        if (searchQuery.trim().length < 2) {
            searchResults = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(400)
        onSearch(searchQuery, token) { rows ->
            searchResults = rows
            searching = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("Hugging Face Hub", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (!pickOnly) {
            OutlinedTextField(
                value = token,
                onValueChange = {
                    token = it
                    onSave(profile.copy(apiKey = it, model = modelId))
                },
                label = { Text("Access token") },
                placeholder = { Text("hf_…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        OutlinedTextField(
            value = modelId,
            onValueChange = {
                modelId = it
                if (!pickOnly) onSave(profile.copy(apiKey = token, model = it))
            },
            label = { Text("Model id") },
            placeholder = { Text("meta-llama/Meta-Llama-3-8B-Instruct:fastest") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Text("Popular", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HuggingFaceDefaults.featuredChatModels.forEach { (id, label) ->
                FilterChip(
                    selected = modelId == id,
                    onClick = {
                        modelId = id
                        if (!pickOnly) onSave(profile.copy(apiKey = token, model = id))
                    },
                    label = { Text(label) },
                )
            }
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Hub models") },
            placeholder = { Text("llama, qwen, mistral…") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (searching) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        }
        searchResults.take(12).forEach { row ->
            OutlinedButton(
                onClick = {
                    val picked = "${row.id}:fastest"
                    modelId = picked
                    if (!pickOnly) onSave(profile.copy(apiKey = token, model = picked))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text(row.id, fontWeight = FontWeight.Medium)
                    Text(
                        listOfNotNull(row.pipelineTag, "${row.downloads} downloads").joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (!pickOnly) {
            Button(
                onClick = { onSave(profile.copy(apiKey = token, model = modelId)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save model & token")
            }
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
