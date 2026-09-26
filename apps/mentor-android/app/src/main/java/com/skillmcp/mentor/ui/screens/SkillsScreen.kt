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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.ui.MentorViewModel

@Composable
fun SkillsScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    var repoUrl by remember { mutableStateOf("https://github.com/anthropics/skills") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("GitHub skills", style = MaterialTheme.typography.titleLarge)
        Text(
            "Import official SKILL.md repos. Content is verified locally and injected into mentor prompts.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = repoUrl,
            onValueChange = { repoUrl = it },
            label = { Text("Repository URL") },
            singleLine = true,
        )
        Button(onClick = { vm.importSkill(repoUrl) }, modifier = Modifier.fillMaxWidth()) {
            Text("Import skill")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.skills) { skill ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(skill.title, style = MaterialTheme.typography.titleMedium)
                        Text("${skill.owner}/${skill.repo} @ ${skill.ref}")
                        TextButton(onClick = { vm.removeSkill(skill.id) }) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }
}
