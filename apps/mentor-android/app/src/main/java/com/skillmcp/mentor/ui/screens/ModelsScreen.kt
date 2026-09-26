package com.skillmcp.mentor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.llm.LlmProviderKind
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.components.AppBackground

@Composable
fun ModelsScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("My provider") }
    var baseUrl by remember { mutableStateOf("https://api.openai.com/v1/") }
    var model by remember { mutableStateOf("gpt-4o-mini") }
    var apiKey by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(LlmProviderKind.OPENAI_COMPAT) }

    AppBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Models", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Connect any LLM — cloud APIs or local Ollama. Switch active model anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.llmProfiles) { profile ->
                val selected = state.prefs.activeLlmProfileId == profile.id
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RowWithRadio(
                            selected = selected,
                            onSelect = { vm.selectLlmProfile(profile.id) },
                            title = profile.name,
                            subtitle = "${profile.kind.label} · ${profile.model}",
                        )
                        OutlinedButton(onClick = { vm.testLlmProfile(profile) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Test connection")
                        }
                    }
                }
            }
            item {
                if (!showAdd) {
                    Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Add custom provider")
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("New provider", style = MaterialTheme.typography.titleMedium)
                            LlmProviderKind.entries.forEach { k ->
                                RowWithRadio(
                                    selected = kind == k,
                                    onSelect = { kind = k },
                                    title = k.label,
                                    subtitle = null,
                                )
                            }
                            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(model, { model = it }, label = { Text("Model id") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(apiKey, { apiKey = it }, label = { Text("API key") }, modifier = Modifier.fillMaxWidth())
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

@Composable
private fun RowWithRadio(
    selected: Boolean,
    onSelect: () -> Unit,
    title: String,
    subtitle: String?,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column {
            Text(title, fontWeight = FontWeight.Medium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
