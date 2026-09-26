package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.llm.HuggingFaceDefaults
import com.skillmcp.mentor.llm.HuggingFaceModelSummary
import com.skillmcp.mentor.llm.LlmProfile
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HuggingFaceModelPicker(
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
